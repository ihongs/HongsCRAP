package foo.hongs.serv.centre;

import foo.hongs.Cnst;
import foo.hongs.HongsException;
import foo.hongs.action.ActionHelper;
import foo.hongs.action.FormSet;
import foo.hongs.action.anno.Action;
import foo.hongs.action.anno.Assign;
import foo.hongs.action.anno.CommitSuccess;
import foo.hongs.action.anno.Verify;
import foo.hongs.db.DB;
import foo.hongs.db.DBAction;
import foo.hongs.db.Model;
import foo.hongs.serv.medium.Mlink;
import foo.hongs.serv.medium.Mstat;
import foo.hongs.util.Synt;
import java.util.Map;

/**
 *
 * @author Hongs
 */
@Action("centre/medium/impress")
@Assign(conf="medium", name="impress")
public class ImpressAction extends DBAction {

    @Override
    public void isExists(ActionHelper helper) {

    }
    @Override
    public void isUnique(ActionHelper helper) {

    }
    @Override
    public void update(ActionHelper helper) {
        // 禁止更新
    }
    @Override
    public void delete(ActionHelper helper) {
        // 禁止删除
    }

    @Action("create")
    @Verify(conf="", form="")
    @CommitSuccess
    @Override
    public void create(ActionHelper helper)
    throws HongsException {
        try {
            super.create(helper);
            helper.reply("欢迎光临");
        } catch (HongsException ex ) {
            helper.reply("谢谢光临");
        }
    }

    @Override
    protected Model  getEntity(ActionHelper helper)
    throws HongsException {
        String link, linkId;
        link   = helper.getParameter("link"   );
        linkId = helper.getParameter("link_id");
        if (link == null || linkId == null) {
            throw new HongsException(0x1100, "link and link_id required");
        }
        Mlink model = (Mlink) DB.getInstance("medium").getModel("impress");
        model.setLink  (link  );
        model.setLinkId(linkId);
        return model;
    }

    @Override
    protected  Map   getReqMap(ActionHelper helper, Model ett, String opr, Map req)
    throws HongsException {
        Object sid = helper.getRequest().getSession().getId();
        Object uid = helper.getSessibute(Cnst.UID_SES);
        req = super.getReqMap( helper, ett, opr, req );
        if (uid == null) {
            req.put("user_id", "" );
            req.put("sess_id", sid);
        } else {
            req.put("user_id", uid);
            req.put("sess_id", "" );
        }
        if ("delete".equals(opr)) {
            req.put(Cnst.AR_KEY, Synt.mapOf("", Synt.mapOf(
                "user_id" , uid,
                "state"   , 1
            )));
        }
        return req;
    }

    @Override
    protected String getRspMsg(ActionHelper helper, Model ett, String opr, int num)
    throws HongsException {
        if (num <= 0) {
            return "操作失败";
        }

        Mlink  lin = (Mlink) ett;
        Mstat  sta = (Mstat) ett.db.getModel("statist");
        Map    ena = FormSet.getInstance( "medium" )
                            .getEnum("statist_link");
        String lnk = lin.getLink(  );
        String lid = lin.getLinkId();
               sta.setLink   ( lnk );
               sta.setLinkId ( lid );
        if (ena.containsKey(lnk)) {
        if ("create".equals(opr)) {
            sta.add("impress_count", num);
            return "记录成功";
        }
        }

        return super.getRspMsg(helper, ett, opr, num);
    }
    
}
