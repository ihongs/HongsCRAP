package app.hongs.serv.manage.mesage;

import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.anno.Action;
import app.hongs.db.DBAction;

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
