package io.github.ihongs.dh.lucene.conn;

import io.github.ihongs.CoreLogger;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;

/**
 * 基于 HDFS 的 Lucene Directory 实现
 * 将索引文件存储在 HDFS 上, 支持分布式读取
 */
public class HdfsDirectory extends Directory {

    private final FileSystem fs;
    private final Path dirPath;
    private volatile boolean isOpen = true;

    public HdfsDirectory(FileSystem fs, Path dirPath) throws IOException {
        this.fs = fs;
        this.dirPath = dirPath;
        if (!fs.exists(dirPath)) {
            if (!fs.mkdirs(dirPath)) {
                throw new IOException("Failed to create directory: " + dirPath);
            }
            CoreLogger.trace("Created directory: {}", dirPath);
        }
    }

    @Override
    public String[] listAll() throws IOException {
        ensureOpen();
        FileStatus[] stats = fs.listStatus(dirPath);
        String[] names = new String[stats.length];
        for (int i = 0; i < stats.length; i++) {
            names[i] = stats[i].getPath().getName();
        }
        return names;
    }

    @Override
    public void deleteFile(String name) throws IOException {
        ensureOpen();
        Path p = new Path(dirPath, name);
        if (fs.exists(p)) {
            fs.delete(p, false);
        }
    }

    @Override
    public long fileLength(String name) throws IOException {
        ensureOpen();
        return fs.getFileStatus(new Path(dirPath, name)).getLen();
    }

    @Override
    public IndexOutput createOutput(String name, IOContext context) throws IOException {
        ensureOpen();
        Path p = new Path(dirPath, name);
        OutputStream out = fs.create(p, true);
        return new HdfsIndexOutput(name, out);
    }

    @Override
    public IndexOutput createTempOutput(String prefix, String suffix, IOContext context) throws IOException {
        ensureOpen();
        String name = prefix + "_" + java.util.UUID.randomUUID().toString() + suffix;
        Path p = new Path(dirPath, name);
        OutputStream out = fs.create(p, true);
        return new HdfsIndexOutput(name, out);
    }

    @Override
    public IndexInput openInput(String name, IOContext context) throws IOException {
        ensureOpen();
        Path p = new Path(dirPath, name);
        return new HdfsIndexInput(name, fs.open(p), fs.getFileStatus(p).getLen());
    }

    @Override
    public void sync(Collection<String> names) throws IOException {
        ensureOpen();
        // HDFS 本身保证数据一致性, 无需额外 sync
    }

    @Override
    public void syncMetaData() throws IOException {
        ensureOpen();
        // HDFS 本身保证数据一致性, 无需额外 sync
    }

    @Override
    public void rename(String source, String dest) throws IOException {
        ensureOpen();
        Path src = new Path(dirPath, source);
        Path dst = new Path(dirPath, dest);
        fs.rename(src, dst);
    }

    @Override
    public void close() throws IOException {
        if (isOpen) {
            isOpen = false;
        }
    }

    @Override
    public java.util.Set<String> getPendingDeletions() throws IOException {
        return java.util.Collections.emptySet();
    }

    protected void ensureOpen() throws AlreadyClosedException {
        if (!isOpen) {
            throw new AlreadyClosedException("HdfsDirectory is closed: " + dirPath);
        }
    }

    @Override
    public Lock obtainLock(String name) throws IOException {
        // 暂时禁用 HDFS 锁，仅用于测试性能
        return new Lock() {
            @Override
            public void ensureValid() throws AlreadyClosedException {
            }
            @Override
            public void close() {
            }
        };
    }

    public FileSystem getFileSystem() {
        return fs;
    }

    public Path getDirPath() {
        return dirPath;
    }

    /**
     * HDFS 上的 IndexOutput 实现
     */
    private static class HdfsIndexOutput extends IndexOutput {

        private final String name;
        private final OutputStream out;
        private long pos = 0;
        private final java.util.zip.CRC32 crc = new java.util.zip.CRC32();

        HdfsIndexOutput(String name, OutputStream out) {
            super(name, name);
            this.name = name;
            this.out  = out;
        }

        @Override
        public void writeByte(byte b) throws IOException {
            out.write(b);
            crc.update(b);
            pos += 1;
        }

        @Override
        public void writeBytes(byte[] b, int offset, int length) throws IOException {
            out.write(b, offset, length);
            crc.update(b, offset, length);
            pos += length;
        }

        @Override
        public long getFilePointer() {
            return pos;
        }

        @Override
        public long getChecksum() throws IOException {
            return crc.getValue();
        }

        @Override
        public void close() throws IOException {
            out.close();
        }

    }

    /**
     * HDFS 上的 IndexInput 实现
     */
    private static class HdfsIndexInput extends IndexInput {

        private final String name;
        private final org.apache.hadoop.fs.FSDataInputStream in;
        private final long length;
        private boolean isClosed = false;

        HdfsIndexInput(String name, org.apache.hadoop.fs.FSDataInputStream in, long length) {
            super(name);
            this.name   = name;
            this.in     = in;
            this.length = length;
        }

        @Override
        public void close() throws IOException {
            if (!isClosed) {
                in.close();
                isClosed = true;
            }
        }

        @Override
        public long getFilePointer() {
            try {
                return in.getPos();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void seek(long pos) throws IOException {
            in.seek(pos);
        }

        @Override
        public long length() {
            return length;
        }

        @Override
        public byte readByte() throws IOException {
            return in.readByte();
        }

        @Override
        public void readBytes(byte[] b, int offset, int len) throws IOException {
            in.readFully(b, offset, len);
        }

        @Override
        public IndexInput slice(String sliceDescription, long offset, long length) throws IOException {
            return new HdfsSliceInput(sliceDescription, in, offset, length);
        }

    }

    /**
     * HDFS 切片读取
     */
    private static class HdfsSliceInput extends IndexInput {

        private final org.apache.hadoop.fs.FSDataInputStream in;
        private final long offset;
        private final long sliceLength;
        private long pos = 0;

        HdfsSliceInput(String desc, org.apache.hadoop.fs.FSDataInputStream in, long offset, long length) {
            super(desc);
            this.in = in;
            this.offset = offset;
            this.sliceLength = length;
        }

        @Override
        public void close() throws IOException {
            // 共享底层流, 不关闭
        }

        @Override
        public long getFilePointer() {
            return pos;
        }

        @Override
        public void seek(long p) throws IOException {
            pos = p;
        }

        @Override
        public long length() {
            return sliceLength;
        }

        @Override
        public byte readByte() throws IOException {
            in.seek(offset + pos);
            pos += 1;
            return in.readByte();
        }

        @Override
        public void readBytes(byte[] b, int off, int len) throws IOException {
            in.seek(offset + pos);
            in.readFully(b, off, len);
            pos += len;
        }

        @Override
        public IndexInput slice(String sliceDescription, long off, long length) throws IOException {
            return new HdfsSliceInput(sliceDescription, in, offset + off, length);
        }

    }

    /**
     * 基于 HDFS 文件的分布式锁
     * 利用 HDFS 的原子创建和删除实现跨进程互斥
     */
    public static class HdfsLock extends Lock {

        private final String lockName;
        private final FileSystem fs;
        private final Path dirPath;
        private Path lockPath;

        HdfsLock(FileSystem fs, Path dirPath, String name) {
            this.fs = fs;
            this.dirPath = dirPath;
            this.lockName = name;
        }

        @Override
        public void close() {
            if (lockPath != null) {
                try {
                    fs.delete(lockPath, false);
                } catch (IOException e) {
                    CoreLogger.trace("Failed to release HDFS lock {}", lockName);
                }
                lockPath = null;
            }
        }

        @Override
        public void ensureValid() throws IOException {
            if (lockPath == null || !fs.exists(lockPath)) {
                throw new AlreadyClosedException("HDFS lock lost: " + lockName);
            }
        }

        public void obtain() throws IOException {
            Path lp = new Path(dirPath, lockName);
            try {
                // 确保父目录存在
                if (!fs.exists(dirPath)) {
                    CoreLogger.trace("Creating lock parent directory: {}", dirPath);
                    if (!fs.mkdirs(dirPath)) {
                        throw new IOException("Failed to create directory: " + dirPath);
                    }
                }
                // 原子创建, create with overwrite=false in HDFS is atomic
                if (fs.exists(lp)) {
                    throw new IOException("Lock already exists: " + lockName);
                }
                CoreLogger.trace("Creating lock: {}", lp);
                OutputStream out = fs.create(lp, false);
                out.write(io.github.ihongs.Core.SERVER_ID.getBytes("UTF-8"));
                out.close();
                lockPath = lp;
                CoreLogger.trace("Lock created: {}", lp);
            } catch (org.apache.hadoop.fs.FileAlreadyExistsException e) {
                throw new IOException("Lock already exists: " + lockName, e);
            }
        }

    }

}
