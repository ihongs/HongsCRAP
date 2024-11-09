package io.github.ihongs.serv.graphy;

import io.github.ihongs.CruxException;
import io.github.ihongs.combat.CombatHelper;
import io.github.ihongs.combat.anno.Combat;
import io.github.ihongs.dh.graphs.GraphsRecord;
import io.github.ihongs.serv.matrix.DataCombat;
import io.github.ihongs.util.Synt;
import java.util.Map;
import org.apache.lucene.document.Document;

/**
 * 数据操作命令
 * @author hong
 */
@Combat("matrix.data")
public class GrapCombat {

    @Combat("revert")
    public static void revert(String[] args) throws CruxException, InterruptedException {
        Map opts = CombatHelper.getOpts(args, new String[] {
            "conf=s",
            "form=s",
            "user:s",
            "memo:s",
            "time:i",
            "bufs:i",
            "truncate:b",
            "cascades:b",
            "includes:b",
            "incloses:b",
            "grapable:b",
            "!A",
            "?Usage: revert --conf CONF_NAME --form FORM_NAME [--time TIMESTAMP] ID0 ID1 ..."
        });

        String conf = (String) opts.get("conf");
        String form = (String) opts.get("form");
        Grap dr = Grap.getInstance(conf, form );

        /**
         * 级联更新操作
         * 默认不作级联
         */
        Casc da = new Casc(
             dr ,
             Synt.declare (opts.get("cascades"), false),
             Synt.declare (opts.get("includes"), false),
             Synt.declare (opts.get("incloses"), false),
             Synt.declare (opts.get("grapable"), false)
        );

        DataCombat.revert(da, opts);
    }

    private static class Casc extends DataCombat.Casc {

        protected final Grap         grap;
        protected final GraphsRecord grec;

        public Casc(Grap grap, boolean cascades, boolean includes, boolean incloses, boolean grapable)
        throws CruxException {
            super(grap , cascades , includes , incloses  );
            this .grec = grapable ? grap.getGraph() : null;
            this .grap = grap;
        }

        @Override
        public void set(String id, Document doc) throws CruxException {
            grap.setDoc(id, doc, grec);
        }

    }

}
