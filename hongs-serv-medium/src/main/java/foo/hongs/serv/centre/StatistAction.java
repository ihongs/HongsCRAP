package foo.hongs.serv.centre;

import foo.hongs.action.ActionHelper;
import foo.hongs.action.anno.Action;
import foo.hongs.action.anno.Assign;
import foo.hongs.db.DBAction;

/**
 *
 * @author Hongs
 */
@Action("centre/medium/statist")
@Assign(conf="medium", name="statist")
public class StatistAction extends DBAction {

    @Override
    public void isExists(ActionHelper helper) {

    }
    @Override
    public void isUnique(ActionHelper helper) {

    }
    @Override
    public void create(ActionHelper helper) {
        // 禁止创建
    }
    @Override
    public void update(ActionHelper helper) {
        // 禁止更新
    }
    @Override
    public void delete(ActionHelper helper) {
        // 禁止删除
    }

}
