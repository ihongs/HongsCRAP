package io.github.ihongs.test;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class TestHdfsClean {
    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        conf.set("dfs.client.use.datanode.hostname", "true");
        FileSystem fs = FileSystem.get(new java.net.URI("hdfs://localhost:9000"), conf);
        Path p = new Path("/hongs/data");
        if (fs.exists(p)) {
            System.out.println("Deleting " + p + "...");
            fs.delete(p, true);
        }
        System.out.println("Done!");
        fs.close();
    }
}
