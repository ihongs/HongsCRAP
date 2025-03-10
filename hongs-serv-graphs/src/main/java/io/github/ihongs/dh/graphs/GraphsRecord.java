package io.github.ihongs.dh.graphs;

import static io.github.ihongs.Cnst.ID_KEY;
import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CruxException;
import io.github.ihongs.CruxExemption;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.dh.IEntity;
import io.github.ihongs.dh.IReflux;
import io.github.ihongs.dh.JFigure;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Dist;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.types.Node;
import org.neo4j.driver.v1.types.Relationship;

/**
 * 图存储模型
 *
 * Neo4J 的一个简单逻辑封装.
 * 本想用嵌入方案但有些问题,
 * 如索引依赖的库方法冲突等,
 * 故仅支持连接外部查询服务.
 *
 * 此模型是建立在核心的表单规则之上,
 * 利用了 enum,form,fork 的三个别名,
 * 分别为 type,part,pick 这三种类型,
 * 对应的是标签,关联,无中间节点关联.
 *
 * @author Hongs
 */
public class GraphsRecord extends JFigure implements IEntity, IReflux, AutoCloseable {

    protected boolean REFLUX_MODE = false;

    protected final static Pattern INJECT_RULE = Pattern.compile("\\$(\\w+)");
//  protected final static Pattern UPDATE_RULE = Pattern.compile("(^|\\s)(CREATE|UPADTE|DELETE|REMOVE|SET)\\s");

    /**
     * 关联关系方向, 0 出, 1 进, 2 双向
     */
    public static final String RD_KEY = "dir";
    /**
     * 关联关系标签
     */
    public static final String RL_KEY = "lab";
    /**
     * 关联节点标签
     */
    public static final String RT_KEY = "tab";

    private Session     db = null;
    private Transaction tx = null;

    public GraphsRecord(Map form) {
        setFields(form);
    }

    /**
     * 获取实例
     * 存储为 conf/form 表单为 conf.form
     * 表单缺失则尝试获取 conf/form.form
     * 实例生命周期将交由 Core 维护
     * @param conf
     * @param form
     * @return
     * @throws CruxException
     */
    public static GraphsRecord getInstance(String conf, String form)
    throws CruxException {
        String code = GraphsRecord.class.getName( ) +":"+ conf +":"+ form;
        Core   core = Core.getInstance( );
        GraphsRecord  inst = (GraphsRecord) core.get(code);
        if (inst == null) {
            Map  fxrm = FormSet.getInstance(conf).getForm(form);
            inst = new GraphsRecord(fxrm);
            core.set( code, inst );
        }
        return inst;
    }

    /**
     * 预定义标签
     * 自动写入并作限制条件
     * @return
     */
    public String getLabel() {
        return Synt.asString ( getParams().get("label") );
    }

    /**
     * 连接数据库
     * 注意不会自动开启事务
     * @return
     */
    public Session open() {
        if (db == null) {
            db  = Conn.getInstance(getParams()).session();
        }
        return db;
    }

    /**
     * 连接数据库
     * 本方法会开启事务模式
     * @return
     */
    public Transaction conn() {
        begin(  );
        return tx;
    }

    @Override
    public void close() throws Exception {
        if (db != null) {
            try {
                // 默认退出时提交
                if (tx != null) {
                    try {
                        commit();
                    } catch (Error er) {
                        cancel();
                        throw er;
                    }
                }
            } finally {
                db.close( );
                db  = null ;
            }
        }
    }

    @Override
    public void begin( ) {
        if (tx == null ) {
            tx  = open().beginTransaction();
        }
        REFLUX_MODE = true;
    }

    @Override
    public void commit() {
        if (tx != null ) {
            tx.success();
            tx.close();
            tx  = null;
        }
        REFLUX_MODE = false;
    }

    @Override
    public void cancel() {
        if (tx != null ) {
            tx.failure();
            tx.close();
            tx  = null;
        }
        REFLUX_MODE = false;
    }

    /**
     * 查询
     * @param rd
     * @param ca
     * @return
     * @throws CruxException
     */
    public Map search(Map rd, Case ca) throws CruxException {
        // 条件
        ca.where(rd);

        // 排序
        Set<String> ob = Synt.toTerms(rd.get(Cnst.OB_KEY));
        if (ob != null && !ob.isEmpty( )) {
            ca.order(ob);
        }

        // 搜索
        Set<String> wd = Synt.toWords(rd.get(Cnst.WD_KEY));
        if (wd != null && !wd.isEmpty( )) {
            finds(ca, wd, getRschable( ));
        }

        // 分页
        int rn = Synt.declare(rd.get(Cnst.RN_KEY), Cnst.RN_DEF);
        int pn = Synt.declare(rd.get(Cnst.PN_KEY), Cnst.PN_DEF);
        int sn = rn * (pn - 1);
        if (sn >= 0 && rn > 0) {
            ca.limit(sn , rn );
        }

        // 结果
        Set<String> rb = Synt.toTerms(rd.get(Cnst.RB_KEY));
        Set<String> ab = Synt.toTerms(rd.get(Cnst.AB_KEY));
        Map sd  = new HashMap ();
        Map pms = ca.getParams();
        if (pn  > 0) {
            StatementResult sr = run( ca.toString(), pms );
            sd.put("list", toList(sr, rb, ab));
        }
        if (rn  > 0) {
            StatementResult sr = run( ca.toCounts(), pms );
            sd.put("page", toPage(sr, rn, pn));
        }
        return sd;
    }

    /**
     * 查询
     * @param rd
     * @return
     * @throws CruxException
     */
    @Override
    public Map search(Map rd) throws CruxException {
        Map  f  = getFields();
        Case ca = new Case(f);

        ca.match("(n)");
        ca.retur( "n" );

        // 限制当前表的标签
        String pd = getLabel();
        if (null != pd && ! pd.isEmpty()) {
            ca.where("n:" + nquotes (pd));
        }

        return search(rd, ca);
    }

    /**
     * 详情
     * @param rd
     * @return
     * @throws CruxException
     */
    @Override
    public Map recite(Map rd) throws CruxException {
        Map  sd = search (rd);
        List list = (List) sd.get("list");
        Map  page = (Map ) sd.get("page");
        Map  info = !list.isEmpty()?(Map) list.get(0) : new HashMap(0);

        return Synt.mapOf(
            "info", info,
            "page", page
        );
    }

    /**
     * 新建
     * @param rd
     * @return
     * @throws CruxException
     */
    @Override
    public String create(Map rd) throws CruxException {
        return add(rd);
    }

    /**
     * 修改(可批量)
     * @param rd
     * @return
     * @throws CruxException
     */
    @Override
    public int update(Map rd) throws CruxException {
        Map rd2 = new HashMap(rd);
        Set ids = Synt.asSet (rd2.remove(ID_KEY));
        for(Object od : ids) {
            String id = Synt.asString(od);
            try {
                put(id, rd2);
            }
            catch (NullPointerException ex) {
                throw new CruxException(404, "Can not udpate for id: "+id);
            }
        }
        return ids.size();
    }

    /**
     * 删除(可批量)
     * @param rd
     * @return
     * @throws CruxException
     */
    @Override
    public int delete(Map rd) throws CruxException {
        Set ids = Synt.asSet (rd.get (ID_KEY));
        for(Object od : ids) {
            String id = Synt.asString(od);
            try {
                del(id);
            }
            catch (NullPointerException ex) {
                throw new CruxException(404, "Can not delete for id: "+id);
            }
        }
        return ids.size();
    }

    /**
     * 新建
     * @param info
     * @return
     */
    public String add(Map info) {
        String  id = Core.newIdentity( );
        info.remove(ID_KEY);
        addNode(id      );
        setNode(id, info);
        return  id;
    }

    /**
     * 设置
     * @param id
     * @param info
     * @return 1
     */
    public int set(String id, Map info) {
       Node node = getNode(id);
        if (node == null) {
            addNode( id );
        }
        setNode(id, info);
        return 1;
    }

    /**
     * 修改
     * @param id
     * @param info
     * @return 1
     */
    public int put(String id, Map info) {
       Node node = getNode(id);
        if (node == null) {
            throw new NullPointerException("Can not set node '"+id+"', it is not exists");
        }
        setNode(id, info);
        return 1;
    }

    /**
     * 删除
     * @param id
     * @return 1
     */
    public int del(String id) {
       Node node = getNode(id);
        if (node == null) {
            throw new NullPointerException("Can not del node '"+id+"', it is not exists");
        }
        delNode(id      );
        return 1;
    }

    /**
     * 执行查询
     * @param cql
     * @return
     */
    public StatementResult run(String cql) {
        if (4 == (4 & Core.DEBUG)) {
            // 调试用日志
            CoreLogger.debug("GraphsRecord.run: "+cql);
        }
        if (REFLUX_MODE ) { // && UPDATE_RULE.matcher(cql).find()) {
            return conn().run(cql);
        } else {
            return open().run(cql);
        }
    }

    /**
     * 执行查询
     * @param cql
     * @param pms
     * @return
     */
    public StatementResult run(String cql, Map pms) {
        if (4 == (4 & Core.DEBUG)) {
            // 调试用日志
            CoreLogger.debug("GraphsRecord.run: "+injects(cql, pms));
        }
        if (REFLUX_MODE ) { // && UPDATE_RULE.matcher(cql).find()) {
            return conn().run(cql, pms);
        } else {
            return open().run(cql, pms);
        }
    }

    /**
     * 删除节点
     * @param id
     */
    public void delNode(String id) {
        String pd  = getLabel();
        pd  =  pd != null && ! pd.isEmpty() ? ":" + nquotes(pd) : "";
        run("MATCH (n"+pd+" {id:$id})-[r]-() DELETE r", Synt.mapOf("id", id));
        run("MATCH (n"+pd+" {id:$id}) "  +  "DELETE n", Synt.mapOf("id", id));
    }

    /**
     * 获取节点
     * @param id
     * @return
     */
    public Node getNode(String id) {
        String pd  = getLabel();
        pd  =  pd != null && ! pd.isEmpty() ? ":" + nquotes(pd) : "";
        StatementResult rs = run( "MATCH (n"+pd+" {id:$id}) RETURN n LIMIT 1", Synt.mapOf("id", id));
        return rs.hasNext()
             ? rs.next(   )
                 .get ("n")
                 .asNode( )
             : null;
    }

    /**
     * 创建节点
     * @param id
     * @return
     */
    public Node addNode(String id) {
        String pd  = getLabel();
        pd  =  pd != null && ! pd.isEmpty() ? ":" + nquotes(pd) : "";
        StatementResult rs = run("CREATE (n"+pd+" {id:$id}) RETURN n LIMIT 1", Synt.mapOf("id", id));
        return rs.hasNext()
             ? rs.next(   )
                 .get ("n")
                 .asNode( )
             : null;
    }

    /**
     * 设置节点
     * @param id
     * @param info
     */
    public void setNode(String id, Map info) {
        if (id == null) {
            throw new NullPointerException("GraphsRecord.setNode: id can not be null");
        }

        Map<String, Map> flds = getFields();
        Set<String>      keys = new HashSet();
        Set<String>      labs = new HashSet();
        Set<String>      rals = new HashSet();
        Set<Map>         rels = new HashSet();
        Map              vals = new HashMap();
        StringBuilder    cqls = new StringBuilder();
        String           pd   = getLabel( );

        if (pd != null && ! pd.isEmpty( ) ) {
            pd  = ":" + nquotes ( pd );
        } else {
            pd  =  "" ;
        }

        cqls.append("MATCH (n")
            .append(    pd    )
            .append(" {id:$id}) SET ");

        // 写入新数据
        for(Object ot : info.entrySet( )) {
            Map.Entry et = (Map.Entry) ot;
            String fn = Synt.asString(
                        et.getKey( ) );
            Object fv = et.getValue( );
            Map    fc = flds.get( fn );

            if (fc == null || "@".equals(fn)) {
                continue;
            }

            Object ft = fc.get("__type__");

            if ("type".equals(ft)) {
                try {
                    String conf = Synt.asString(fc.get("conf"));
                    String name = Synt.asString(fc.get("enum"));
                    Map    anum = FormSet.getInstance ( conf ).getEnum(name);
                    labs.addAll(anum.keySet());
                } catch ( CruxException e) {
                    throw e.toExemption( );
                }

                // 可选的标签
                Set las = Synt.asSet( fv );
                if (fv != null)
                for(Object fv2 : las) {
                    String fv3 = Synt.asString(fv2);
                    String fv4 = nquotes (fv3);
                    labs.remove( fv3);
                    cqls.append("n:")
                        .append( fv4)
                        .append(", ");
                }
            } else
            if ("part".equals(ft)) {
                Set res;
            //  Set ids = new HashSet( );
                int    rd = Synt.declare(fc.get(RD_KEY), 2 );
                String rl = Synt.declare(fc.get(RL_KEY), "");
                String rt = Synt.declare(fc.get(RT_KEY), "");
                if (Synt.declare(fc.get("__repeated__"), false)) {
                    res = Synt.asSet(fv);
                } else {
                    res = Synt.setOf(fv);
                }
                if (fv != null)
                for(Object fv2 : res) {
                    Map  reo = Synt.asMap(fv2);
            //      ids .add(reo.get(ID_KEY) );
                    reo .put(RD_KEY, rd);
                    reo .put(RL_KEY, rl);
                    reo .put(RT_KEY, rt);
                    rels.add(reo);
                }

                String rn = "r";
                if (rl.length() != 0) {
                    rn = rn + ":" + nquotes(rl);
                }
                switch (rd) {
                    case 0:
                        rn =  "-[" + rn + "]->";
                        break;
                    case 1:
                        rn = "<-[" + rn + "]-" ;
                        break;
                    default:
                        rn =  "-[" + rn + "]-" ;
                        break;
                }
                rals.add("MATCH (n"+ pd +" {id:$id})"+rn+"(m) DELETE r");
            } else
            if ("pick".equals(ft)) {
                Set res;
            //  Set ids = new HashSet( );
                int    rd = Synt.declare(fc.get(RD_KEY), 2 );
                String rl = Synt.declare(fc.get(RL_KEY), "");
                String rt = Synt.declare(fc.get(RT_KEY), "");
                if (Synt.declare(fc.get("__repeated__"), false)) {
                    res = Synt.asSet(fv);
                } else {
                    res = Synt.setOf(fv);
                }
                if (fv != null)
                for(Object fv2 : res) {
                    Map  reo = Synt.mapOf(ID_KEY, fv2 );
                    reo .put(RD_KEY, rd);
                    reo .put(RL_KEY, rl);
                    reo .put(RT_KEY, rt);
            //      ids .add(fv2);
                    rels.add(reo);
                }

                String rn = "r";
                if (rl.length() != 0) {
                    rn = rn + ":" + nquotes(rl);
                }
                switch (rd) {
                    case 0:
                        rn =  "-[" + rn + "]->";
                        break;
                    case 1:
                        rn = "<-[" + rn + "]-" ;
                        break;
                    default:
                        rn =  "-[" + rn + "]-" ;
                        break;
                }
                rals.add("MATCH (n"+ pd +" {id:$id})"+rn+"(m) DELETE r");
            } else
            {
                /**
                 * 满足条件:
                 * 名称必须符合命名规范
                 * 且字段未被标识为忽略
                 */
                if (fv != null) {
                    String k = (vals.size()) + "";
                    cqls.append("n." )
                        .append(nquotes(fn))
                        .append( "=$")
                        .append(k    )
                        .append( ", ");
                    vals.put   (k, fv);
                    keys.remove(   fn);
                } else {
                    keys.add   (   fn);
                }
            }
        }

        // 去掉结尾的逗号
        if (!vals.isEmpty()) {
            cqls.setLength(cqls.length() - 2);
        } else {
            cqls.setLength(cqls.length() - 5);
        }

        keys.remove(ID_KEY);

        // 删除多余的属性
        if (!keys.isEmpty()) {
            cqls.append(" REMOVE ");
            for(String key : keys) {
                cqls.append("n." )
                    .append(nquotes(key))
                    .append( ", ");
            }
            cqls.setLength(cqls.length() - 2);
        }

        // 删除多余的标签
        if (!labs.isEmpty()) {
            cqls.append(" REMOVE ");
            for(String lab : labs) {
                cqls.append("n:" )
                    .append(nquotes(lab))
                    .append( ", ");
            }
            cqls.setLength(cqls.length() - 2);
        }

        // 18 是 MATCH (rn {id:$id}) 的长度
        if (cqls.length() > 18) {
            vals.put("id" , id);
            run(cqls.toString(), vals);
        }

        // 删除关系待重建
        if (!rals.isEmpty()) {
            for(String cql : rals) {
                run(cql, Synt.mapOf("id",id));
            }
        }

        // 重设节点的关系
        if (!rels.isEmpty()) {
            for(Map rel : rels) {
                String la , ra;
                String fk = Synt.asString(rel.get(ID_KEY));
                String rl = Synt.asString(rel.get(RL_KEY));
                String rt = Synt.asString(rel.get(RT_KEY));
                if (Synt.declare(rel.get(RD_KEY), 0) != 1) {
                    ra = "->"; la = "-";
                } else {
                    la = "<-"; ra = "-";
                }

                // 关联标签
                if (null != rt && rt.length() != 0) {
                    rt = nquotes( rt );
                    rt =  ":"  +  rt  ;
                }

                vals.clear();
                vals.put("nid", id);
                vals.put("mid", fk);
                cqls.setLength( 0 );
                cqls.append("MATCH (n").append(pd).append(" {id:$nid}) ")
                    .append("MATCH (m").append(rt).append(" {id:$mid}) ")
                    .append("CREATE ")
                    .append("(n)" )
                    .append(  la  )
                    .append("[r"  );

                // 添加标签
                if (null != rl && rl.length() != 0) {
                    rl = nquotes( rl );
                    cqls.append ( ":");
                    cqls.append ( rl );
                }

                // 添加属性
                cqls.append("{");
                for(Object ot : rel.entrySet()) {
                    Map.Entry et = (Map.Entry)ot;
                    String fn = Synt.asString(et.getKey());
                    if (! ID_KEY.equals(fn)
                    &&  ! RD_KEY.equals(fn)
                    &&  ! RL_KEY.equals(fn)
                    &&  ! RT_KEY.equals(fn)) {
                        String k =""+vals.size();
                        Object v = et.getValue();
                        cqls.append(nquotes(fn))
                            .append(":$")
                            .append(k)
                            .append(", ");
                        vals.put(k, v);
                    }
                }
                if (vals.size( ) == 2) {
                    cqls.setLength(cqls.length() - 1); // 去掉括号
                } else {
                    cqls.setLength(cqls.length() - 2); // 去掉逗号
                    cqls.append( "}" );
                }

                cqls.append(  "]" )
                    .append(  ra  )
                    .append("(m)" );

                run(cqls.toString(), vals);
            }
        }
    }

    protected List<Map> toList(StatementResult sr, Set rb, Set ab) {
        Map<String, Map> flds = getFields( );
        Map<String, Set<String>> labKeys = new HashMap();
        Map<String,     String > ralKeys = new HashMap(); // 简单关联
        Map<String,     String > relKeys = new HashMap(); // 桥接关联
        Set<String             > repKeys = new HashSet(); // 多值字段

        // 返回字段
        if (rb == null
        ||  rb.isEmpty (   )
        ||  rb.contains("-")
        ||  rb.contains("*")) {
            rb = new HashSet(flds.keySet());
            rb.remove  ("@");
        } else {
            rb.retainAll/**/(flds.keySet());
        }

        for(Map.Entry<String, Map> et : flds.entrySet()) {
            Map    fc = et.getValue();
            String fn = et.getKey(  );

            // 不在返回集的不需要处理
            if (! rb.contains(fn)) {
                continue;
            }

            Object ft = fc.get("__type__");

            if ("type".equals(ft)) {
                String conf = (String) fc.get("conf");
                String name = (String) fc.get("enum");
                Set    keys ;
                try {
                    keys = FormSet.getInstance(conf).getEnum(name).keySet();
                } catch ( CruxException e) {
                    throw e.toExemption( );
                }
                labKeys.put(fn, keys);
            } else
            if ("pick".equals(ft)) {
                String name = "r";
                String type = Synt.declare(fc.get(RL_KEY), "");
                int    dirn = Synt.declare(fc.get(RD_KEY), 2 );
                if (type.length() != 0) {
                    name +=  ":" + nquotes(type);
                }
                switch (dirn) {
                    case 0:
                        name =  "-[" + name + "]->";
                        break;
                    case 1:
                        name = "<-[" + name + "]-" ;
                        break;
                    default:
                        name =  "-[" + name + "]-" ;
                        break;
                }
                ralKeys.put(fn, name);
            } else
            if ("part".equals(ft)) {
                String name = "r";
                String type = Synt.declare(fc.get(RL_KEY), "");
                int    dirn = Synt.declare(fc.get(RD_KEY), 2 );
                if (type.length() != 0) {
                    name +=  ":" + nquotes(type);
                }
                switch (dirn) {
                    case 0:
                        name =  "-[" + name + "]->";
                        break;
                    case 1:
                        name = "<-[" + name + "]-" ;
                        break;
                    default:
                        name =  "-[" + name + "]-" ;
                        break;
                }
                relKeys.put(fn, name);
            }

            if (Synt.declare(fc.get("__repeated__"), false)) {
                repKeys.add (fn);
            }
        }

        List<        Map> list = new ArrayList (  );
        Map <String, Map> maps = new  HashMap  (  );

        while (sr.hasNext()) {
            Record rec = sr.next(      );
            Node   nod = rec.get(  "n" ).asNode(  );
            String nid = nod.get(ID_KEY).asString();
            Map    row = new HashMap(  );

            list.add(     row);
            maps.put(nid, row);

            // 节点属性
            for(String fn: (Set<String>) rb) {
                if (relKeys.containsKey(fn)) {
                    if(repKeys.contains(fn)) {
                        row.put(fn, new ArrayList());
                    } else {
                        row.put(fn, new  HashMap ());
                    }
                } else
                if (ralKeys.containsKey(fn)) {
                    if(repKeys.contains(fn)) {
                        row.put(fn, new ArrayList());
                    } else {
                        row.put(fn, null);
                    }
                } else
                if (labKeys.containsKey(fn)) {
                    Set ls =labKeys.get(fn);
                    if(repKeys.contains(fn)) {
                        Set lv = new HashSet();
                        for(String v : nod.labels()) {
                            if (ls.contains(v)) {
                                lv.add (  v  );
                            }
                        }
                        row.put(fn, lv);
                    } else {
                        Object  lv = null ;
                        for(String v : nod.labels()) {
                            if (ls.contains(v)) {
                                lv = v ; break;
                            }
                        }
                        row.put(fn, lv);
                    }
                } else
                if (nod.containsKey(fn)) {
                    Object cv = nod.get(fn).asObject();
                    /*
                    if (STRING_MODE && null != cv ) {
                           cv = Synt.asString( cv );
                    }
                    */
                    row.put(fn, cv  );
                } else {
                    row.put(fn, null);
                }
            }
        }

        // 简单关联
        for(Map.Entry<String, String> et : ralKeys.entrySet()) {
            String      fn = et.getKey(  );
            String      ln = et.getValue();
            boolean repeated = Synt.declare(flds.get(fn).get("__repeated__"), false);
            StatementResult rs = run("MATCH (n)"+ln+"(m) WHERE n.id IN $ids RETURN r,n.id,m.id",
                Synt.mapOf("ids", maps.keySet())
            );
            while (rs.hasNext()) {
                Record ro  = rs.next();
                String nid = ro.get("n.id").asString();
                String mid = ro.get("m.id").asString();
                Map row = maps.get(nid);
                if (repeated) {
                    Dict.put(row, mid, fn, null);
                } else {
                    Dict.put(row, mid, fn  /**/);
                }
            }
        }

        // 桥接关联
        for(Map.Entry<String, String> et : relKeys.entrySet()) {
            String      fn = et.getKey(  );
            String      ln = et.getValue();
            boolean repeated = Synt.declare(flds.get(fn).get("__repeated__"), false);
            StatementResult rs = run("MATCH (n)"+ln+"(m) WHERE n.id IN $ids RETURN r,n.id,m.id",
                Synt.mapOf("ids", maps.keySet())
            );
            while (rs.hasNext()) {
                Record ro  = rs.next();
                Relationship re = ro.get("r").asRelationship();
                String nid = ro.get("n.id").asString();
                String mid = ro.get("m.id").asString();
                Map ral = new HashMap();
                // 关系属性
                for(String xn : re.keys( )) {
                    Object cv = re.get (xn).asObject();
                    /*
                    if (STRING_MODE && null != cv ) {
                           cv = Synt.asString( cv );
                    }
                    */
                    ral.put(xn, cv);
                }
                ral.put ( ID_KEY , mid);
                Map row = maps.get(nid);
                if (repeated) {
                    Dict.put(row, ral, fn, null);
                } else {
                    Dict.put(row, ral, fn  /**/);
                }
            }
        }

        return list;
    }

    protected Map toPage(StatementResult sr, int rn, int pn) {
        long tr = 0;
        long tp = 0;
        if (sr.hasNext()) {
            tr = sr.next( ).get ( "c" ).asLong();
            tp = (long)Math.ceil((double) tr/rn);
        }

        Map page = new HashMap( );
        page.put(Cnst.RN_KEY, rn);
        page.put(Cnst.PN_KEY, pn);
        page.put("count", tr);
        page.put("total", tp);

        if (tr == 0) {
            page.put("state", 0); // 没有数据
        } else
        if (tp < pn) {
            page.put("state", 0); // 页码超限
        } else
        {
            page.put("state", 1); // 正常
        }

        return page;
    }

    public static final void finds(Case ca, Set wd, Set wf) {
        if (wd == null || wd.isEmpty()) {
            return;
        }
        if (wf == null || wf.isEmpty()) {
            return;
        }

        StringBuilder fb = new StringBuilder();
        fb.append("(" );
        for(Object fo : wf) {
            String fn = nquotes(fo.toString());
            fb.append("n.").append(fn);
            fb.append( " =~ $wd OR " );
        }
        fb.setLength (fb.length() - 4);
        fb.append(")" );

        StringBuilder wb = new StringBuilder();
        wb.append(".*");
        for(Object fo : wd) {
            String fv = fo.toString( );
            fv = Pattern.quote(  fv  );
            wb.append( fv );
            wb.append(".*");
        }

        ca.where(fb.toString(), Synt.mapOf("wd", wb.toString()));
    }

    /**
     * 转义并包裹字段名
     * @param fn
     * @return
     */
    public  static final String nquotes(String fn) {
        return "`" + Syno.escape(fn, "\\`") + "`";
    }

    /**
     * 转义并包裹字段值
     * @param fn
     * @return
     */
    public  static final String vquotes(String fn) {
        return "'" + Syno.escape(fn, "\\'") + "'";
    }

    /**
     * 将参数注入到串中
     * @param cql
     * @param pms
     * @return
     */
    public  static final String injects(String cql, Map pms) {
        Matcher matcher = INJECT_RULE.matcher( cql );
        StringBuffer sb = new StringBuffer();
        String       st;
        Object       so;

        while (matcher.find()) {
            st = matcher.group(1);
            if (pms.containsKey(st)) {
                so = pms.get(st );
                if ( null != so )
                if (so instanceof Collection
                ||  so instanceof Object[ ]) {
                    StringBuilder op = new StringBuilder();
                    Dist.append ( op , so , true);
                    st = op.toString ( /*JSON*/ );
                } else {
                    st = vquotes( so.toString() );
                }
                else
                st = "null";
            } else {
                st = "null";
            }
            st = Matcher.quoteReplacement(st);
            matcher.appendReplacement(sb, st);
        }
        matcher.appendTail(sb);

        return sb.toString(  );
    }

    @Core.Singleton
    public static class Conn implements AutoCloseable {

        private final  String HREF;
        private        Driver CONN;

        private Conn ( String href , Map opts ) {
            if (href == null || href.isEmpty()) {
                href = "bolt://localhost:7687" ;
            }

            String user = Synt.declare(opts.get("db-username"), "neo4j");
            String pswd = Synt.declare(opts.get("db-password"), "neo4j");
            int    pool = Synt.declare(opts.get("db-poolsize"),   100  );
            int    life = Synt.declare(opts.get("db-lifetime"),  3600  );
            int    wait = Synt.declare(opts.get("db-atimeout"),    60  ); // 连接池满后等待的时间
            int    cont = Synt.declare(opts.get("db-ctimeout"),    15  ); // 连接时最多等待的时间

            HREF = href ;
            CONN = GraphDatabase.driver (
                   href , AuthTokens.basic(user, pswd) , Config.build( )
               .withMaxConnectionPoolSize       (pool)
               .withMaxConnectionLifetime       (life, TimeUnit.SECONDS)
               .withConnectionTimeout           (cont, TimeUnit.SECONDS)
               .withConnectionAcquisitionTimeout(wait, TimeUnit.SECONDS)
               .toConfig()
            );

            CoreLogger.trace("Connect to graph database {}", HREF);
        }

        @Override
        public  void close( ) throws Exception {
            if (CONN == null) return ;

            CONN.close();
            CONN = null ;

            CoreLogger.trace("Disconnect graph database {}", HREF);
        }

        /**
         * 获取数据连接会话
         * @return
         */
        public Session session() {
            if (CONN != null) {
                return  CONN.session();
            }
            throw  new  NullPointerException("Graph database is closed");
        }

        /**
         * 使用配置名称连接
         * @param conf
         * @param form
         * @return
         */
        public static Conn getInstance(String conf, String form) {
            try {
                Map opts = (Map) FormSet
                      .getInstance(conf)
                      .getForm    (form)
                      .get        ("@" );
                return getInstance(opts);
            } catch ( CruxException e) {
                throw e.toExemption( );
            } catch (NullPointerException e) {
                throw new CruxExemption("Can not find form params in "+conf+"."+form);
            }
        }

        /**
         * 使用配置参数连接
         * @param opts
         * @return
         */
        public static Conn getInstance(final Map opts) {
            final String link = Synt.declare(opts.get("db-link"), "");
            if ( 0 != link.length(  ) ) {
                return openByLink(link);
            } else {
                return openByHref(opts);
            }
        }

        private static Conn openByHref(final Map opts) {
            final String href = Synt.declare(opts.get("db-href"), "");
            final String name = Conn.class.getName(  ) + ":" + href  ;

            return Core.GLOBAL_CORE.got(
                name,
                new Supplier <Conn> () {
                    @Override
                    public Conn get () {
                        return new Conn(href, opts);
                    }
                }
            );
        }

        private static Conn openByLink(String link) {
            String conf, form;

            /**
             * db-link 关联仓库名的取值可以是
             * CONF/NAME.FORM
             * CONF/NAME
             * NAME
             * 后两个的名称同时也是表单的名称
             */
            int p  = link.lastIndexOf( "." );
            if (p != -1) {
                form = link.substring(1 + p);
                conf = link.substring(0 , p);
            } else {
                p  = link.lastIndexOf( "/" );
            if (p != -1) {
                form = link.substring(1 + p);
                conf = link;
            } else {
                conf = link;
                form = link;
            }
            }

            /**
             * 获取关联对象并打开连接仓库连接
             * 需注意可能会报配置不存在的错误
             */
            return getInstance( conf, form );
        }

    }

    public static class Case {

        private static final String[] RELS = new String[] {
                Cnst.EQ_REL, " = " , Cnst.NE_REL, " <> ",
                Cnst.SE_REL, " =~ ", Cnst.NS_REL, " !~ ",
                Cnst.LT_REL, " < " , Cnst.LE_REL, " <= ",
                Cnst.GT_REL, " > " , Cnst.GE_REL, " >= ",
                Cnst.IN_REL, " IN ", Cnst.NO_REL, " NO ",
                Cnst.IS_REL, " IS "
        };

        private final Map fds ;
        private final Map pms = new HashMap();
        private final Set jns = new HashSet();
        private final StringBuilder ret = new StringBuilder();
        private final StringBuilder mat = new StringBuilder();
        private final StringBuilder odr = new StringBuilder();
        private final StringBuilder whr = new StringBuilder();
        private final int[] lmt = {0, 0};

        public Case(Map fields) {
            fds= fields;
        }

        public Case retur(CharSequence str) {
            ret.append(str).append(", ");
            return this;
        }

        public Case match(CharSequence str) {
            mat.append(str).append(", ");
            return this;
        }

        public Case order(CharSequence str) {
            odr.append(str).append(", ");
            return this;
        }

        public Case where(CharSequence str) {
            whr.append(str).append(" AND ");
            return this;
        }

        public Case where(CharSequence str, Map map) {
            whr.append(str).append(" AND ");
            pms.putAll(map);
            return this;
        }

        public Case limit(int skip , int size) {
            lmt[ 0 ] = skip;
            lmt[ 1 ] = size;
            return this;
        }

        public Case order(Set ob) {
            _order(odr, ob);
            return this;
        }

        public Case where(Map rd) {
            _where(whr, rd, 0, 0);
            return this;
        }

        public Case join(String fn) {
            if (jns.contains( fn )) {
                return this ;
            }

            Map fc = (Map ) fds.get(fn);
            if (fc == null) {
                throw new CruxExemption("Field item for "+fn+" is not exists");
            }

            String ft = (String) fc.get("__type__");
            if (! "pick".equals( ft )
            &&  ! "part".equals( ft )) {
                throw new CruxExemption("Field type for "+fn+" is not pick or part");
            }

            String mn = nquotes( fn + "_n" );
            String rn = nquotes( fn + "_r" );
            String rt = Synt.declare(fc.get(RL_KEY), "");
            int    rl = Synt.declare(fc.get(RD_KEY), 0 );
            rt  =  rt.isEmpty()? rn: rn+ ":"+nquotes(rt);
            String ru = "[" + rt + "]";
            String mu = "(" + mn + ")";
            switch(rl) {
                case 0:
                   ru =  "-"+ ru +"->";
                   break;
                case 1:
                   ru = "<-"+ ru +"-" ;
                   break;
                default:
                   ru =  "-"+ ru +"-" ;
                   break;
            }

            match("(n)"+ ru + mu);
            jns.add(fn);
            return this;
        }

        public Map getParams() {
            return pms;
        }

        @Override
        public String toString() {
            StringBuilder sql = new StringBuilder( );

            sql.append( " MATCH ");
            if (mat.length() == 0) {
                sql.append( "(n)");
            } else {
                sql.append(mat, 0, mat.length() - 2);
            }

            if (whr.length() != 0) {
                sql.append(" WHERE "   );
                sql.append(whr, 0, whr.length() - 5);
            }

            sql.append(" RETURN ");
            if (ret.length() == 0) {
                sql.append(  "n" );
            } else {
                sql.append(ret, 0, ret.length() - 2);
            }

            if (odr.length() != 0) {
                sql.append(" ORDER BY ");
                sql.append(odr, 0, odr.length() - 2);
            }

            if (lmt[0] != 0) {
                sql.append(" SKIP " ).append(lmt[0]);
            }
            if (lmt[1] != 0) {
                sql.append(" LIMIT ").append(lmt[1]);
            }

            return sql.toString();
        }

        public String toCounts() {
            StringBuilder sql = new StringBuilder( );

            sql.append( " MATCH ");
            if (mat.length() == 0) {
                sql.append( "(n)");
            } else {
                sql.append(mat, 0, mat.length() - 2);
            }

            if (whr.length() != 0) {
                sql.append(" WHERE "   );
                sql.append(whr, 0, whr.length() - 5);
            }

            sql.append(" RETURN count(*) AS c");

            return sql.toString();
        }

        private void _order(StringBuilder odr, Set ob) {
            // 排序
            for(Object  xn : ob) {
                String  ln ;
                String  fn = xn. toString( );

                boolean de ;
                if (fn.startsWith("-")) {
                    de = true ;
                    fn = fn.substring(1);
                } else
                if (fn.  endsWith("!")) {
                    de = true ;
                    fn = fn.substring(0, fn.length() - 1);
                } else {
                    de = false;
                }

                // 桥接关联表字段排序
                int p  = fn.indexOf(".");
                if (p != -1) {
                    ln = fn.substring(0 , p);
                    fn = fn.substring(1 + p);

                    Map fc = (Map) fds.get(ln);
                    if (fc == null || "@".equals(ln)) {
                        continue;
                    }

                    join(ln); // 建立关联关系

                    fc = _subFs ( fc ).get(fn);
                    if (fc == null || "@".equals(fn)) {
                        continue;
                    }

                    ln = ln+"_r";
                } else {
                    Map fc = (Map) fds.get(fn);
                    if (fc == null || "@".equals(fn)) {
                        continue;
                    }

                    ln = /**/"n";
                }

                ln = nquotes(ln);
                fn = nquotes(fn);
                if (de) {
                    fn +=" DESC";
                }
                odr.append(ln).append('.').append(fn).append(',').append(' ');
            }
        }

        private int _where(StringBuilder whr, Map rd, int pi, int xi) {
            for(Object ot : rd.entrySet( )) {
                Map.Entry et = (Map.Entry)ot;
                String fn = Synt.asString(et.getKey());
                Object fv = et.getValue( );
                Map fc = (Map) fds.get(fn);

                if (fc == null || "@".equals(fn)
                ||  fv == null || "" .equals(fv)) {
                    continue;
                }

                String ft = Synt.asString(fc.get("__type__"));

                if (Cnst.OR_KEY.equals(fn)) {
                    if (xi > 2) {
                        continue;
                    }
                    Set rdz = Synt.asSet(fv);
                    StringBuilder whr2 = new StringBuilder();
                    StringBuilder whr3 = new StringBuilder();
                    for(Object rdo : rdz) {
                        whr3.setLength(0);
                        Map rd2 = Synt.asMap(rdo);
                        pi = _where(whr3, rd2, pi, 2);

                        if (whr3.length() != 0) {
                            whr3.setLength(whr3.length() - 5);
                            whr2.append("(").append(whr3).append(")").append(" OR " );
                        }
                    }
                        if (whr2.length() != 0) {
                            whr2.setLength(whr2.length() - 4);
                            whr .append("(").append(whr2).append(")").append(" AND ");
                        }
                } else
                if (Cnst.AR_KEY.equals(fn)) {
                    if (xi > 1) {
                        continue;
                    }
                    Set rdz = Synt.asSet(fv);
                    StringBuilder whr2 = new StringBuilder();
                    StringBuilder whr3 = new StringBuilder();
                    for(Object rdo : rdz) {
                        whr3.setLength(0);
                        Map rd2 = Synt.asMap(rdo);
                        pi = _where(whr3, rd2, pi, 1);

                        if (whr3.length() != 0) {
                            whr3.setLength(whr3.length() - 5);
                            whr2.append("(").append(whr3).append(")").append(" AND ");
                        }
                    }
                        if (whr2.length() != 0) {
                            whr2.setLength(whr2.length() - 5);
                            whr .append("(").append(whr2).append(")").append(" AND ");
                        }
                } else
                if (Cnst.NR_KEY.equals(fn)) {
                    if (xi > 1) {
                        continue;
                    }
                    Set rdz = Synt.asSet(fv);
                    StringBuilder whr2 = new StringBuilder();
                    StringBuilder whr3 = new StringBuilder();
                    for(Object rdo : rdz) {
                        whr3.setLength(0);
                        Map rd2 = Synt.asMap(rdo);
                        pi = _where(whr3, rd2, pi, 1);

                        if (whr3.length() != 0) {
                            whr3.setLength(whr3.length() - 5);
                            whr2.append("(").append(whr3).append(")").append(" AND ");
                        }
                    }
                        if (whr2.length() != 0) {
                            whr2.setLength(whr2.length() - 5);
                            whr .append("NOT "); // 否定此条件
                            whr .append("(").append(whr2).append(")").append(" AND ");
                        }
                } else
                if ("type".equals(ft)) {
                    Set ibs = Synt.asSet(fv);
                    if (ibs.isEmpty()) {
                        continue;
                    }

                    whr.append("(" );
                    for(Object lb : ibs) {
                        whr.append( "n:" )
                           .append(nquotes(Synt.asString(lb)))
                           .append(" OR ");
                    }
                    whr.setLength(whr.length() - 4);
                    whr.append(") AND ");
                } else
                if ("pick".equals(ft)) {
                    Set ids = Synt.asSet(fv);
                    if (ids.isEmpty()) {
                        continue;
                    }

                    join ( fn );

                    String mn = nquotes (fn + "_n");

                    pi = _cqlIn(whr, pms, mn+"."+ID_KEY, " IN ", ids, pi);
                    whr.append (" AND ");
                } else
                if ("part".equals(ft)) {
                    Map rd2 = Synt.asMap(fv);
                    if (rd2.isEmpty()) {
                        continue;
                    }

                    join ( fn );

                    String mn = nquotes (fn + "_n");
                    String rn = nquotes (fn + "_r");

                    Map fd2 = _subFs(fc);
                    for(Object fn2 : fd2.keySet()) {
                        Object fv2 = rd2.get(fn2);
                        if ( null == fv2 || "".equals(fv2)) {
                            continue;
                        }   fv = fv2;

                        if ( ID_KEY . equals(fn2)) {
                            fn = mn +"."+ nquotes(fn2.toString());
                        } else {
                            fn = rn +"."+ nquotes(fn2.toString());
                        }

                        if (fv instanceof Map) {
                            pi = _cqlRm(whr, pms, fn, ( Map ) fv, pi);
                        } else
                        if (fv instanceof Collection
                        ||  fv instanceof Object [ ]) {
                            Set vz = Synt.asSet(fv);
                                vz.remove("");
                            if (vz.isEmpty())
                                continue;
                            pi = _cqlIn(whr, pms, fn, " IN ", vz, pi);
                        } else {
                            pi = _cqlRn(whr, pms, fn, " = " , fv, pi);
                        }
                        whr.append(" AND ");
                    }
                } else
                {
                        fn = "n."+ nquotes(fn);
                        if (fv instanceof Map) {
                            pi = _cqlRm(whr, pms, fn, ( Map ) fv, pi);
                        } else
                        if (fv instanceof Collection
                        ||  fv instanceof Object [ ]) {
                            Set vz = Synt.asSet(fv);
                                vz.remove("");
                            if (vz.isEmpty())
                                continue;
                            pi = _cqlIn(whr, pms, fn, " IN ", fv, pi);
                        } else {
                            pi = _cqlRn(whr, pms, fn, " = " , fv, pi);
                        }
                        whr.append(" AND ");
                }
            }

            return pi;
        }

        private int _cqlRm(StringBuilder xql, Map pms, String n, Map m, int i) {
            Map w = new HashMap(m);
            int j = i ;
            for(int k = 0; k < RELS.length; k += 2) {
                Object v = w.remove(RELS[k]);
                if (v != null ) {
                    String r = RELS[ k + 1 ];

                    // 单值匹配忽略空串
                    if (k < 16 && "".equals(v)) {
                        continue;
                    }

                    // 模糊匹配转为正则
                    if (k == 4
                    ||  k == 6) {
                        String s = v.toString( ).trim( );
                        s = s.replaceAll( "\\s+", ".*" );
                        v = ".*"+ Pattern.quote(s) +".*";
                    }

                    if (k < 16) {
                        i = _cqlRn(xql, pms, n, r, v, i);
                    } else
                    if (k < 20) {
                        i = _cqlIn(xql, pms, n, r, v, i);
                    } else
                    {
                        i = _cqlIs(xql, pms, n,    v, i);
                    }

                    xql.append(" AND ");
                }
            }
            if (i > j) {
                xql.setLength(xql.length() - 5);
            }
            return i;
        }

        private int _cqlRn(StringBuilder xql, Map pms, String n, String r, Object v, int i) {
            if (" !~ ".equals(r) ) {
                xql.append("NOT ");
                r = " =~ " ;
            }
            pms.put(""+ i, v);
            xql.append( n )
               .append( r )
               .append("$")
               .append( i );
            return  1 + i  ;
        }

        private int _cqlIn(StringBuilder xql, Map pms, String n, String r, Object v, int i) {
            if (" NO ".equals(r) ) {
                xql.append("NOT ");
                r = " IN " ;
            }
            v = Synt.asSet(v);
            pms.put(""+ i, v);
            xql.append( n )
               .append( r )
               .append("$")
               .append( i );
            return  1 + i  ;
        }

        private int _cqlIs(StringBuilder xql, Map pms, String n, Object v, int i) {
            String  s = v.toString().trim();
            String  r = null;
            switch (s) {
                case "EMPTY":
                    r = "=" ;
                    v =  "" ;
                    break;
                case "NOT-EMPTY":
                    r = "<>";
                    v =  "" ;
                    break;
                default:
                    throw new CruxExemption(400, "Unsupported `is`: "+v);
            }
            pms.put(""+ i, v);
            xql.append( n )
               .append( r )
               .append("$")
               .append( i );
            return  1 + i  ;
        }

        private Map<String, Map> _subFs(Map fc) {
            try {
                String conf = (String) fc.get("conf");
                String name = (String) fc.get("form");
                return FormSet.getInstance(conf).getForm(name);
            } catch ( CruxException e) {
                throw e.toExemption( );
            }
        }

    }

}
