package foo.hongs.serv.centra;

import foo.hongs.HongsException;
import foo.hongs.action.ActionHelper;
import foo.hongs.action.anno.Action;
import foo.hongs.action.anno.Assign;
import foo.hongs.db.DB;
import foo.hongs.db.DBAction;
import foo.hongs.db.Model;
import foo.hongs.serv.medium.Mlink;

/**
 * 评论
 * @author Hongs
 */
@Action("centra/medium/comment")
@Assign(conf="medium", name="comment")
public class CommentAction extends DBAction {

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
        Mlink mod =  (Mlink) DB.getInstance("medium").getModel("comment");
        mod.setLink  (helper.getParameter("link"   ));
        mod.setLinkId(helper.getParameter("link_id"));
        return mod;
    }

}
