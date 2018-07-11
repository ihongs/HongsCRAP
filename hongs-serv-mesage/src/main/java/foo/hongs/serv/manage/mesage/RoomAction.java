package foo.hongs.serv.manage.mesage;

import foo.hongs.action.ActionHelper;
import foo.hongs.action.ActionRunner;
import foo.hongs.action.anno.Action;
import foo.hongs.db.DBAction;

/**
 *
 * @author Hongs
 */
@Action("manage/mesage/room")
public class RoomAction extends DBAction {
    
    @Override
    public void acting(ActionHelper helper, ActionRunner runner) {
        runner.setModule("mesage");
        runner.setEntity( "room" );
    }

}
