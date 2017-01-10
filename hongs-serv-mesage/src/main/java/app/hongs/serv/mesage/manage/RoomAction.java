package app.hongs.serv.mesage.manage;

import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.anno.Action;
import app.hongs.db.DBAction;

/**
 *
 * @author Hongs
 */
@Action("manage/mesage/room")
public class RoomAction extends DBAction {
    
    @Override
    public void initiate(ActionHelper helper, ActionRunner runner) {
        this.mod = "mesage";
        this.ent = "room";
    }

}