package io.github.ihongs.db.diff;

import io.github.ihongs.CruxException;
import io.github.ihongs.combat.CombatHelper;
import io.github.ihongs.combat.anno.Combat;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 数据库结构对比命令
 * 此工具仅适合 MySQL
 * @author Hongs
 */
@Combat("common.db.diff")
public class DBComb {

    @Combat("__main__")
    public static void exec(String[] args) throws CruxException {
        Map opts = CombatHelper.getOpts(args,
            "from=s", "to=s", "sync:b", "del-tables:b", "del-fields:b"
        );

        String  fr = (String ) opts.get("from");
        String  to = (String ) opts.get( "to" );
        boolean sc = (boolean) opts.get("sync");
        boolean dt = (boolean) opts.get("del-tables");
        boolean df = (boolean) opts.get("del-fields");

        Pattern patt = Pattern.compile("\\.\\w+$");
        Matcher frMt = patt.matcher(fr);
        Matcher toMt = patt.matcher(to);

        if (frMt.find() && toMt.find()) {
            DB    frDb = DB.getInstance(fr.substring(0 , frMt.start()));
            Table frTb = frDb.getTable (fr.substring(frMt.start() + 1));
            DB    toDb = DB.getInstance(to.substring(0 , toMt.start()));
            Table toTb = toDb.getTable (to.substring(toMt.start() + 1));

            if (sc) {
                new TableDiff(frTb).syncSlaver(toTb, df);
            } else {
                new TableDiff(frTb).deffSlaver(toTb, df);
            }
        } else {
            DB    frDb = DB.getInstance(fr);
            DB    toDb = DB.getInstance(fr);

            if (sc) {
                new DBDiff(frDb).syncSlaver(toDb, null, null, dt, df);
            } else {
                new DBDiff(frDb).deffSlaver(toDb, null, null, dt, df);
            }
        }
    }

}
