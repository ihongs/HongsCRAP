package io.github.ihongs.serv.centre;

import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.Assign;
import io.github.ihongs.db.DBAction;

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
