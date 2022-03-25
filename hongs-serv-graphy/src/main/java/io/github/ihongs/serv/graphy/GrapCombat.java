package io.github.ihongs.serv.graphy;

import io.github.ihongs.HongsException;
import io.github.ihongs.combat.anno.Combat;
import io.github.ihongs.serv.matrix.Data;
import io.github.ihongs.serv.matrix.DataCombat;
import java.util.Set;

/**
 * 数据操作命令
 * @author hong
 */
@Combat("matrix.data")
public class GrapCombat {

    @Combat("revert")
    public static void revert(String[] args) throws HongsException, InterruptedException {
        DataCombat.revert(args, new Inst());
    }

    @Combat("update")
    public static void update(String[] args) throws HongsException, InterruptedException {
        DataCombat.update(args, new Inst());
    }

    @Combat("delete")
    public static void delete(String[] args) throws HongsException, InterruptedException {
        DataCombat.delete(args, new Inst());
    }

    @Combat("search")
    public static void search(String[] args) throws HongsException {
        DataCombat.search(args, new Inst());
    }

    @Combat("uproot")
    public static void uproot(String[] args) throws HongsException {
        DataCombat.uproot(args, new Inst());
    }

    private static class Inst extends DataCombat.Inst {

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
