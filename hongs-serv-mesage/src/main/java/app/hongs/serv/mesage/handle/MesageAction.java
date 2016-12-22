package app.hongs.serv.mesage.handle;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.VerifyHelper;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Permit;
import app.hongs.action.anno.Select;
import app.hongs.db.DB;
import app.hongs.db.util.FetchCase;
import app.hongs.db.Model;
import app.hongs.db.Table;
import app.hongs.serv.mesage.Mesage;
import app.hongs.serv.mesage.MesageHelper;
import app.hongs.serv.mesage.MesageWorker;
import app.hongs.util.Data;
import app.hongs.util.Synt;
import app.hongs.util.verify.Wrongs;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 消息处理器
 * @author Hongs
 */
@Action("handle/mesage")
public class MesageAction {

    @Action("retrieve")
    @Permit(conf="$", role={"", "handle", "manage"})
    @Select(conf="mesage", form="message")
    public void retrieve(ActionHelper helper) throws HongsException {
        DB      db = DB.getInstance("mesage");
        Model  mod = db.getModel   ( "note" );
        Map    req = helper.getRequestData( );

        // 别名映射
        if (req.containsKey("uid")) {
            req.put("user_id", req.get("uid"));
        }
        if (req.containsKey("rid")) {
            req.put("room_id", req.get("rid"));
        }
        if (req.containsKey("time")) {
            req.put("stime", req.get("time"));
        }

        // rid 不能为空
        String rid = helper.getParameter("rid");
        if (rid == null || "".equals(rid)) {
            helper.fault("Parameter 'rid' can not be empty");
            return;
        }
        // 检查操作权限
        // TODO:

        Map rsp = mod.retrieve(req);

        /**
         * 直接取出消息数据列表
         * 扔掉用于索引的数据项
         */
        List<Map> list = null;
        if (rsp.containsKey("list")) {
            list = (List<Map>) rsp.get("list");
        } else
        if (rsp.containsKey("info")) {
            Map info = ( Map ) rsp.get("info");
            list = new ArrayList();
            list.add(info);
        }
        if (list != null) {
            for (Map info : list ) {
                String msg = (String) info.get("msg");
                info.clear();
                info.putAll((Map) Data.toObject(msg));
            }
        }

        helper.reply(rsp);
    }

    @Action("room/retrieve")
    @Permit(conf="$", role={"", "handle", "manage"})
    public void retrieveRoom(ActionHelper helper) throws HongsException {
        DB      db = DB.getInstance( "mesage" );
        Map     rd = helper.getRequestData(   );
        Set    uid = Synt.asTerms(rd.get("uid"));
        String mid = (String) helper.getSessibute(Cnst.UID_SES);

        FetchCase fc = new FetchCase(FetchCase.STRICT);
        // 禁用关联
        fc.setOption("ASSOCS", new HashSet());
        // 自己在内
        Table rm = db.getTable("room_mate");
        fc.join  (rm.tableName, rm.name)
          .by    (FetchCase.LEFT)
          .on    (rm.name+".room_id = room.id")
          .filter(rm.name+".user_id = ?", mid );
        // 全部字段
        rd.remove(Cnst.RB_KEY);

        Model   md = db.getModel ("room");
        Map     ro = md.retrieve (rd, fc);
        List    rs = (List)ro.get("list");

        // 追加用户信息
        joinUserInfo(db , rs , mid , uid);

        helper.reply(ro);
    }

    @Action("user/retrieve")
    @Permit(conf="$", role={"", "handle", "manage"})
    public void retrieveUser(ActionHelper helper) throws HongsException {
        DB      db = DB.getInstance( "mesage" );
        String uid = helper.getParameter("id" );
        String mid = (String) helper.getSessibute(Cnst.UID_SES);
        String rid ;
        Map     ro ;
        Map     rx ;

        if (uid == null || "".equals(uid)) {
            helper.fault( "ID 不能为空" );
        }

        /**
         * 任何两个用户需要聊天
         * 都必须创建一个聊天室
         */
        ro = db.fetchCase()
            .from  (db.getTable("room_mate").tableName)
            .filter("uid = ? AND state = ?" , uid , 1 )
            .select("rid")
            .one   ();
        if (ro == null || ro.isEmpty()) {
            // 检查好友关系
            // TODO:

            // 不存在则创建
            Set<Map> rs = new HashSet();

            ro = new HashMap();
            ro.put("user_id", mid);
            rs.add(ro);

            ro = new HashMap();
            ro.put("user_id", uid);
            rs.add(ro);

            ro = new HashMap();
            ro.put("users",rs);
            ro.put("state", 1);

            rid = db.getModel("room").add(ro);
        } else {
            rid = ( String )  ro.get( "rid" );
        }

        // 查询会话信息
        ro = db.fetchCase()
            .from  (db.getTable("room").tableName)
            .filter("id = ? AND state > ?", rid,0)
            .one   ();
        if (ro == null || ro.isEmpty()) {
            helper.fault("会话不存在");
        }

        // 追加好友信息
        List<Map> rs = new ArrayList();
                  rs.add(ro /* Usr */);
        joinUserInfo(db, rs, mid,null);

        rx = new HashMap();
        rx.put("info", ro);

        helper.reply(rx);
    }

    @Action("create")
    @Permit(conf="$", role={"", "handle", "manage"})
    @Select(conf="mesage", form="message", mode=2)
    public void create(ActionHelper helper) throws HongsException {
        MesageWorker que = MesageHelper.getWorker();
        VerifyHelper ver = new VerifyHelper( );
        Map      dat = helper.getRequestData();
        byte     mod = Synt.declare(dat.get("md"), (byte) 0);
        Map      tmp ;

        ver.isUpdate( false );
        ver.isPrompt(mod <= 0);

        try {
            // 验证连接数据
            ver.addRulesByForm("mesage", "connect");
            tmp = ver.verify(dat);

            // 提取主题并将连接数据并入消息数据, 清空规则为校验消息做准备
            ver.getRules().clear();
            dat.putAll(tmp);

            // 验证消息数据
            ver.addRulesByForm("mesage", "message");
            tmp = ver.verify(dat);

            String uid = (String) tmp.get("uid");
            String rid = (String) tmp.get("rid");
            String kd = (String) tmp.get("kind");
            long   st = Synt.declare(dat.get("time"), 0L);
            String id = Core.getUniqueId();
            tmp.put("id", id);

            // 送入消息队列
            que.add(new Mesage(id, uid, rid, kd, Data.toString(dat), System.currentTimeMillis()));

            dat = new HashMap(  );
            dat.put("info" , tmp);
        } catch (Wrongs ex ) {
            dat = ex.toReply(mod);
        }

        helper.reply(dat);
    }

    @Action("file/create")
    @Permit(conf="$", role={"", "handle", "manage"})
    public void createFile(ActionHelper helper) {
        helper.reply("",helper.getRequestData());
    }

    /**
     * 为聊天列表关联用户信息
     * 头像及说明来自用户信息
     * 名称依此按群组/好友/用户优先设置
     * @param db   必须是 mesage 数据库
     * @param rs   查到的聊天室结果集合
     * @param mid  当前会话用户ID
     * @param uids 仅查询这些用户
     * @throws HongsException
     */
    public static void joinUserInfo(DB db, List<Map> rs, String mid, Collection uids) throws HongsException {
        Map<Object, List<Map>> ump = new HashMap();
        Map<Object, Map> fmp = new HashMap();
        Map<Object, Map> gmp = new HashMap();
        List<Map> rz; // 临时查询列表

        // 获取到 rid 映射
        for(Map ro : rs) {
            if (Synt.asserts(ro.get("level"), 1) == 1) { // 私聊
                fmp.put(ro.get("id"), ro );
            }
                gmp.put(ro.get("id"), ro );
        }
        Set rids = new HashSet( );
        rids.addAll(fmp.keySet());
        rids.addAll(gmp.keySet());

        /**
         * 获取及构建 uids => 群组/用户 的映射关系
         */
        FetchCase fc = db.fetchCase()
            .from   (db.getTable("room_mate").tableName)
            .orderBy("state DESC")
            .select ("room_id AS rid, user_id AS uid, name, state")
            .filter ("room_id IN (?)", rids);
        if (uids != null && ! uids.isEmpty()) {
          fc.filter ("user_id IN (?)", uids);
        }
        rz = fc.all ();
        for(Map rv : rz) {
            Object rid = rv.get("rid" );
            Object nid = rv.get("uid" );
            Object nam = rv.get("name");

            List<Map> rx; // ump 映射列表
            List<Map> ry; // gmp 成员列表
                 Map  ro; // 聊天室信息头

            // 映射关系列表
            rx = ump.get(nid);
            if (rx == null) {
                rx  = new ArrayList();
                 ump.put(nid , rx );
            }

            // 好友聊天信息
            ro = fmp.get(rid);
            if (ro != null) {
                rx.add( ro);
                ro.put("name", nam);
            }

            // 群组成员列表
            ro = gmp.get(rid);
            if (ro != null) {
                ry = (List) ro.get("users");
                if (ry == null) {
                    ry  = new ArrayList();
                    ro.put("users" , ry );
                }
                rv = new HashMap( );
                rv.put("uid" , nid);
                rv.put("name", nam);
                rv.put("head", "" );
                rv.put("note", "" );
                rv.put("isMe", mid.equals(nid));
                rx.add( rv);
                ry.add( rv);
            }
        }
        uids = ump.keySet();

        // 补充好友名称
        rz = db.fetchCase()
            .from   (db.getTable("user_mate").tableName)
            .filter ("user_id IN (?) AND mate_id = ? AND name != '' AND name IS NOT NULL", uids, mid)
            .select ("user_id AS uid , name")
            .all    ();
        for(Map rv : rz) {
           List<Map> rx = ump.get(rv.remove("uid"));
            for(Map  ro : rx) {
             Object nam = ro.get("name");
                if (nam ==  null || "".equals(nam)) {
                    ro.put("name" , rv.get("name"));
                }
            }
        }

        // 补充用户信息
        rz = db.fetchCase()
            .from   (db.getTable("user").tableName)
            .filter ("id IN (?)", uids )
            .select ("id AS uid, name, note, head")
            .all    ();
        for(Map rv : rz) {
           List<Map> rx = ump.get(rv.remove("uid"));
            for(Map  ro : rx) {
             Object nam = ro.get("name");
                if (nam ==  null || "".equals(nam)) {
                    ro.put("name" , rv.get("name"));
                }
                ro.put("note", rv.get("note"));
                ro.put("head", rv.get("head"));
            }
        }
    }

}
