package foo.hongs.serv.centra;

import foo.hongs.action.ActionHelper;
import foo.hongs.action.anno.Action;
import foo.hongs.action.anno.Verify;
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