package io.github.ihongs.serv.centra;

import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.Verify;
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