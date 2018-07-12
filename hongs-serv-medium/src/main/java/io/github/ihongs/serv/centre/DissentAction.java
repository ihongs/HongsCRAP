package io.github.ihongs.serv.centre;

import io.github.ihongs.Cnst;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.Assign;
import io.github.ihongs.action.anno.CommitSuccess;
import io.github.ihongs.action.anno.Verify;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.DBAction;
import io.github.ihongs.db.Model;
import io.github.ihongs.serv.medium.Mlink;
import io.github.ihongs.serv.medium.Mstat;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.verify.Capts;
import io.github.ihongs.util.verify.Wrongs;
import java.util.Map;
import java.util.Set;

/**
 * 评论动作
 * @author Hongs
 */
@Action("centre/medium/dissent")
@Assign(conf="medium", name="dissent")
public class DissentAction extends DBAction {

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
    
    @Action("create")
    @Verify(conf="", form="")
    @CommitSuccess
    @Override
    public void create(ActionHelper helper)
    throws HongsException {
        // 举报必须要验证码
        Map rd = helper.getRequestData();
        try {
            new io.github.ihongs.util.verify.Verify()
               .addRule("capt" , new Capts( ))
               .verify (  rd  );
        }
        catch (Wrongs wr) {
           byte md = 0;
            Set ab = Synt.toTerms(rd.get(Cnst.AB_KEY));
            if (ab.contains(".errs")) md = 1;
           else
            if (ab.contains("!errs")) md = 2;
            helper.reply( wr.toReply( md ) );
            return;
        }

        try {
            super.create(helper);
        } catch ( HongsException ex ) {
        if ( ex.getErrno() == 0x104e) {
            helper.fault("您已经举报过了, 请等候处理或查看结果");
        } else {
            throw  ex;
        }
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
        Mlink model = (Mlink) DB.getInstance("medium").getModel("dissent");
        model.setLink  (link  );
        model.setLinkId(linkId);
        return model;
    }

    @Override
    protected  Map   getReqMap(ActionHelper helper, Model ett, String opr, Map req)
    throws HongsException {
        Object uid = helper.getSessibute(Cnst.UID_SES);
        req = super.getReqMap( helper, ett, opr, req );
        req.put("user_id" , uid);
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
        if (num == 0) {
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
            sta.add("dissent_count", num);
            return "举报成功";
        }
        if ("delete".equals(opr)) {
            int unm = 0 - num;
            sta.put("dissent_count", unm);
            return "取消举报";
        }
        }

        return super.getRspMsg(helper, ett, opr, num);
    }

}
