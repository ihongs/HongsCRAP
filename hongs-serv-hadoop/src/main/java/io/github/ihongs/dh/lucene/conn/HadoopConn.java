package io.github.ihongs.dh.lucene.conn;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.util.daemon.Chore;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.AlreadyClosedException;

/**
 * 分布式存储连接
 * 存在读慢的缺陷
 * @author Hongs
 */
@Core.Singleton
public class HadoopConn implements Conn {

    @Core.Soliloquy
    public static class Getter implements ConnGetter {

        @Override
        public Conn get (String dbpath, String dbname) {
            return  Core.getInstance().got(Conn.class.getName() +":"+ dbname, () -> new CourseConn(
                    Core.getInterior().got(Conn.class.getName() +"|"+ dbname, () -> new HadoopConn(
                        dbpath, dbname
                    ))
                ));
        }

    }

    public HadoopConn(String dbpath, String dbname) {
        this(dbpath, dbname, CoreConfig.getInstance());
    }

    public HadoopConn(String dbpath, String dbname, Properties cc) {
        String uri = cc.getProperty("core.hadoop.hdfs.uri", "hdfs://localhost:9000");
        String dir = cc.getProperty("core.hadoop.data.dir", "/hongs/dir");
        if (!dbpath.startsWith("/")) {
             dbpath = dir+"/"+dbpath;
        }

        this.dbname = dbname;
        this.dbpath = dbpath;

        this.ramBuf = Double .parseDouble(cc.getProperty("core.lucene.ram.buf.size", "16"  ));
        this.maxBuf = Integer.parseInt   (cc.getProperty("core.lucene.max.buf.docs", "-1"  ));
        this.maxCnt = Integer.parseInt   (cc.getProperty("core.lucene.flush.limit" , "1000"));
        int  maxSec = Integer.parseInt   (cc.getProperty("core.lucene.flush.timed" , "600" )); // 定时存盘

        // 初始化 HDFS
        try {
            Configuration conf = new Configuration();
            conf.set("dfs.client.use.datanode.hostname", "true");
            if (!uri.isEmpty()) {
                this.hdfs = FileSystem.get(new java.net.URI(uri), conf);
            } else {
                this.hdfs = FileSystem.get(conf);
            }
            CoreLogger.trace("HadoopConn initialized: uri={}, fs={}, dbpath={}",
                uri, hdfs.getClass().getSimpleName(), dbpath);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize HDFS for " + dbname, e);
        }

        final HadoopConn that = this;
        this.flushr = new Runnable() {
            private volatile boolean running; // 不必精确, 内部有锁
            @Override
            public void run() {
                if (!running) {
                    try {
                        running = true ;
                        that . flush ();
                    } finally {
                        running = false;
                    }
                }
            }
        };
        this.flushs = Chore.getInstance().ran(this.flushr, maxSec , maxSec);
    }

    private final ReentrantReadWriteLock WL = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock RL = new ReentrantReadWriteLock();
    private final ScheduledFuture flushs;
    private final Runnable flushr;
    private final String   dbpath;
    private final String   dbname;
    private final FileSystem hdfs;
    private  HdfsDirectory dbdir  = null;
    private  IndexWriter   writer = null;
    private  IndexReader   reader = null;
    private  IndexSearcher finder = null;
    private volatile boolean vary = true; // 变更标识
    private volatile int    count = 0;    // 冲刷计数
    private final    int   maxCnt ;       // 冲刷限定
    private final    int   maxBuf ;       // 缓存数量
    private final   double ramBuf ;       // 缓存容量(MB)

    @Override
    public String getDbName() {
        return dbname;
    }

    @Override
    public String getDbPath() {
        return dbpath;
    }

    @Override
    public IndexWriter getWriter() throws IOException {
        WL.readLock().lock();
        try {
            if (writer != null && writer.isOpen()) {
                return writer;
            }
        } finally {
            WL.readLock().unlock();
        }

        WL.writeLock().lock();
        try {
            if (writer != null && writer.isOpen()) {
                return writer;
            }

            IndexWriterConfig iwc = new IndexWriterConfig();
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            iwc.setMaxBufferedDocs(maxBuf);
            iwc.setRAMBufferSizeMB(ramBuf);
            iwc.setCommitOnClose  ( true );

            boolean   dix = hdfs.exists(new Path(dbpath));
            if (dbdir == null) {
                dbdir = new HdfsDirectory(hdfs, new Path(dbpath));
            }

            writer = new IndexWriter(dbdir, iwc);

            if (! dix) writer.commit(); // 初始化目录, 规避首次读报错

            CoreLogger.trace("Start the hadoop writer for {}", dbname);

            return writer;
        } finally {
            WL.writeLock().unlock();
        }
    }

    @Override
    public IndexReader getReader() throws IOException {
        RL.readLock().lock();
        try {
            if (!vary && 0 < reader.getRefCount()) {
                return reader;
            }
        } finally {
            RL.readLock().unlock();
        }

        RL.writeLock().lock();
        try {
            if (!vary && 0 < reader.getRefCount()) {
                return reader;
            }

            long  t0 = System.currentTimeMillis();
            getWriter();
            long  t1 = System.currentTimeMillis();
            IndexReader readar = reader;
            reader = DirectoryReader.open(writer);
            long  t2 = System.currentTimeMillis();
            finder = new  IndexSearcher  (reader);

            CoreLogger.trace("open(writer)={}ms, open(reader)={}ms, db={}", (t1 - t0), (t2 - t1), dbname);

            // 释放旧的连接
            if (null != readar) try {
                    readar.decRef();
                if (readar.getRefCount() <= 0) {
                    readar.close ();
                    CoreLogger.trace("Close the hadoop reader for {}", dbname);
                }
            } catch (AlreadyClosedException e) {
                // Pass
            }

            CoreLogger.trace("Start the hadoop reader for {}", dbname);

            vary = false ;
            return reader;
        } finally {
            RL.writeLock().unlock();
        }
    }

    @Override
    public IndexSearcher getFinder() throws IOException {
        getReader();

        return finder;
    }

    @Override
    public void write(Map<String, Document> docs) throws IOException {
        if (docs == null || docs.isEmpty()) {
            return;
        }

        RL.writeLock().lock();
        try {
            IndexWriter  iw = getWriter  ();

            for(Map.Entry<String, Document> et : docs.entrySet()) {
                String   id = et.getKey  ();
                Document dc = et.getValue();
                if (dc != null) {
                    iw.updateDocument (new Term("@"+Cnst.ID_KEY, id), dc);
                } else {
                    iw.deleteDocuments(new Term("@"+Cnst.ID_KEY, id)    );
                }

                count += 1;
            }

            vary = true;

            // 超量冲刷, 后台执行
            if (count >= maxCnt) {
                count  = 0;
                Chore.getInstance()
                     .exe( flushr );
            }
        } finally {
            RL.writeLock().unlock();
        }
    }

    public void flush() {
        try {
        try {
            if (writer == null
            || !writer.isOpen()) {
                return;
            }

            writer.commit();

            CoreLogger.trace("Flush the hadoop recs to HDFS for {}", dbname);
        } catch (IOException x) {
            CoreLogger.error(x);
        }
        } catch ( Throwable x ) {
            x.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
        try {
            flushs.cancel(false);

            if (writer != null
            &&  writer.isOpen()) {
                writer.commit();
                writer.close ();
                writer  = null ;
            }

            if (reader != null ) {
                reader.close ();
                reader  = null ;
                finder  = null ;
            }

            if (dbdir  != null) {
                dbdir .close ();
                dbdir   = null ;
            }

            CoreLogger.trace("Close the hadoop conn for {}", dbname);
        } catch (IOException x) {
            CoreLogger.error(x);
        }
        } catch ( Throwable x ) {
            x.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return "Hadoop conn " + dbname;
    }

}
