package io.github.ihongs.serv.manage.mesage;

import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.db.DBAction;

/**
 *
 * @author Hongs
 */
@Action("manage/mesage/user/term")
public class UserTermAction extends DBAction {

    @Override
    public void acting(ActionHelper helper, ActionRunner runner) {
        runner.setModule("mesage");
        runner.setEntity("user_term");
    }

}
