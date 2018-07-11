package app.hongs.serv.centra;

import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Verify;
import java.util.Map;

@Action("centra/medium/upload")
public class UploadAction {
    
    @Action("image/create")
    @Verify(conf="medium", form="image")
    public void imageUpload(ActionHelper helper) {
        Map rd = helper.getRequestData();
        helper.reply(rd);
    }
    
}