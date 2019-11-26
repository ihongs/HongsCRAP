package io.github.ihongs.serv.graphy;

import io.github.ihongs.HongsException;
import io.github.ihongs.cmdlet.anno.Cmdlet;
import io.github.ihongs.serv.matrix.Data;
import io.github.ihongs.serv.matrix.DataCmdlet;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据操作命令
 * @author hong
 */
@Cmdlet("matrix.data")
public class GrapCmdlet {

    @Cmdlet("revert")
    public static void revert(String[] args) throws HongsException, InterruptedException {
        DataCmdlet.revert(args, new Inst());
    }

    @Cmdlet("update")
    public static void update(String[] args) throws HongsException, InterruptedException {
        DataCmdlet.update(args, new Inst());
    }

    @Cmdlet("delete")
    public static void delete(String[] args) throws HongsException, InterruptedException {
        DataCmdlet.delete(args, new Inst());
    }

    @Cmdlet("search")
    public static void search(String[] args) throws HongsException {
        DataCmdlet.search(args, new Inst());
    }

    @Cmdlet("uproot")
    public static void uproot(String[] args) throws HongsException {
        DataCmdlet.uproot(args, new Inst());
    }

    private static class Inst extends DataCmdlet.Inst {

        @Override
        public Data getInstance(String conf, String form) {
            return  Grap . getInstance(conf, form);
        }

        @Override
        public Set<String> getAllPaths() {
            return  null ;
        }

    }

}
