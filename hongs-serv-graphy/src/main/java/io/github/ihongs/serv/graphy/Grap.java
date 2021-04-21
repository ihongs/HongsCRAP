package io.github.ihongs.serv.graphy;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.dh.graphs.GraphsRecord;
import io.github.ihongs.serv.matrix.Data;
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

    private Set<String> gpCols = null;

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
        String name = Grap.class.getName() +":"+ conf +"."+ form;
        if (core.isset(name)) {
            inst = (Grap) core.get(name);
        } else {
            inst = new Grap(conf , form);
            core.set( name , inst );
        }
        return inst;
    }

    /**
     * 图谱字段
     * 无图谱字段时返回空集合
     * @return
     */
    public Set <String> getGrapable() {
        if (null != gpCols) {
            return  gpCols;
        }
        gpCols = getCaseNames("grapable");
        return gpCols;
    }

    /**
     * 获取模型
     * 无图谱字段将返回 null
     * @return
     */
    public GraphsRecord getGraph() {
        if (null != graph) {
            return  graph;
        }

        Set able  = getGrapable ( );
        if (able != null && ! able.isEmpty()) {
            graph = new Graph(this);
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
                return   super.getFields();
            }
            catch (NullPointerException e) {
                Map fz ;
                Map fs =  that.getFields();
                Set gs =  that.getGrapable ( );
                if (gs != null) {
                    fz =  new LinkedHashMap( );
                    for(Object fn : gs) {
                        Object fc = fs.get(fn);
                        if (fc != null) {
                            fz.put (fn  ,  fc);
                        }
                    }
                } else {
                    fz =  fs ;
                }
                setFields(fz);
                return    fz ;
            }
        }
    }
}
