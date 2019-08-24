package io.github.ihongs.serv.graphy;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.dh.graphs.GraphsRecord;
import io.github.ihongs.serv.matrix.Data;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.document.Document;

/**
 * 混合图模型
 *
 * @author Hongs
 */
public class Grap extends Data {

    private Graph graph = null;

    public Grap(String conf, String form) {
        super(conf, form);
    }

    /**
     * 获取实例
     * 生命周期将交由 Core 维护
     * @param conf
     * @param form
     * @return
     */
    public static Grap getInstance(String conf, String form) {
        Grap  inst;
        Core   core = Core.getInstance();
        String name = Grap.class.getName() +":"+ conf +":"+ form;
        if (core.containsKey(name)) {
            inst = (Grap) core.got(name);
        } else {
            inst = new Grap(conf , form);
            core.put( name , inst );
        }
        return inst;
    }

    /**
     * 获取模型
     * @return
     */
    public GraphsRecord getGraph() {
        if (null != graph) {
            return  graph;
        }

        // 必须明确指定哪些字段需要存入图谱
        Set able  = getCaseNames("grapable");
        if (able != null && !able.isEmpty()) {
            graph = new  Graph  (   this   );
            return  graph;
        }

        return null;
    }

    @Override
    public void setDoc(final String   id ,
                       final Document doc)
    throws HongsException {
        super.setDoc(id, doc);

        // 保存到图谱
        GraphsRecord gr = getGraph();
        if (gr != null) {
            Map rd = new LinkedHashMap();
            padDat(doc, rd, gr.getFields().keySet());
//          String id = (String) rd.get(Cnst.ID_KEY);
            gr.set(id , rd);
        }
    }

    @Override
    public void addDoc(final Document doc)
    throws HongsException {
        super.addDoc(/**/doc);

        // 保存到图谱
        GraphsRecord gr = getGraph();
        if (gr != null) {
            Map rd = new LinkedHashMap();
            padDat(doc, rd, gr.getFields().keySet());
            String id = (String) rd.get(Cnst.ID_KEY);
            gr.set(id , rd);
        }
    }

    @Override
    public void delDoc(final String   id )
    throws HongsException {
        super.delDoc(id /**/);

        // 从图谱删除
        GraphsRecord gr = getGraph();
        if (gr != null) {
        //  gr.del    (id);
            gr.delNode(id);
        }
    }

    private static class Graph extends GraphsRecord  {

        protected Grap that;

        private Graph(Grap that) {
            super(null);

            this.that = that;
        }

        @Override
        public String getLabel() {
            String lb = super.getLabel();

            // 有设置 partId 则用 partId 当 label
            // 最后有 formId 就用 formId 当 label
            if (lb == null) {
                lb  = that.getPartId();
            if (lb == null) {
                lb  = that.getFormId();
            }}

            return lb ;
        }

        @Override
        public Map getFields() {
            try {
                return super.getFields(  );
            }
            catch (NullPointerException e) {
                Map fs  = new LinkedHashMap(that.getFields());
                Set gs  = getCaseNames("grapable");
                if (gs != null) {
                    Iterator it = fs . entrySet( ).iterator();
                    while (it.hasNext()) {
                        Map.Entry et = (Map.Entry) it.next ();
                        Object    fn = et.getKey();
                        if (gs.contains(fn) || "@".equals(fn)) {
                            continue;
                        }
                        it.remove( );
                    }
                }
                setFields(fs);
                return    fs ;
            }
        }
    }
}
