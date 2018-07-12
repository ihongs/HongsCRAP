package io.github.ihongs.serv.centra;

import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.Assign;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.DBAction;
import io.github.ihongs.db.Model;
import io.github.ihongs.serv.medium.Mlink;

/**
 * 举报
 * @author Hongs
 */
@Action("centra/medium/dissent")
@Assign(conf="medium", name="dissent")
public class DissentAction extends DBAction {

    @Override
    public void create(ActionHelper helper) {
        // 禁止添加
    }
    @Override
    public void delete(ActionHelper helper) {
        // 禁止删除
    }
    @Override
    public void isExists(ActionHelper helper) {

    }
    @Override
    public void isUnique(ActionHelper helper) {

    }

    @Override
    public Model getEntity(ActionHelper helper) throws HongsException {
        Mlink mod =  (Mlink) DB.getInstance("medium").getModel("dissent");
        mod.setLink  (helper.getParameter("link"   ));
        mod.setLinkId(helper.getParameter("link_id"));
        return mod;
    }

}
