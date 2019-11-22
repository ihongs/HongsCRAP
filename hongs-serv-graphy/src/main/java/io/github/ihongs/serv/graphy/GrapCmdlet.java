package io.github.ihongs.serv.graphy;

import io.github.ihongs.HongsException;
import io.github.ihongs.cmdlet.anno.Cmdlet;
import io.github.ihongs.serv.matrix.Data;
import io.github.ihongs.serv.matrix.DataCmdlet;
import io.github.ihongs.serv.matrix.DataCmdlet.DataFactor;

/**
 * 数据操作命令
 * @author hong
 */
@Cmdlet("matrix.data")
public class GrapCmdlet {

    private static class GrapFactor extends DataFactor {
        @Override
        public Data getInstance(String conf, String form) {
        return Grap.getInstance(       conf,        form);
        }
    }

    @Cmdlet("revert")
    public static void revert(String[] args) throws HongsException, InterruptedException {
        DataCmdlet.revert(args, new GrapFactor());
    }

    @Cmdlet("update")
    public static void update(String[] args) throws HongsException, InterruptedException {
        DataCmdlet.update(args, new GrapFactor());
    }

    @Cmdlet("delete")
    public static void delete(String[] args) throws HongsException, InterruptedException {
        DataCmdlet.delete(args, new GrapFactor());
    }

    @Cmdlet("search")
    public static void search(String[] args) throws HongsException {
        DataCmdlet.search(args, new GrapFactor());
    }

    @Cmdlet("uproot")
    public static void uproot(String[] args) throws HongsException {
        DataCmdlet.uproot(args, new GrapFactor());
    }

}
