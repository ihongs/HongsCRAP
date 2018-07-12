package io.github.ihongs.serv.centre;

import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.CommitSuccess;
import io.github.ihongs.action.anno.Preset;
import io.github.ihongs.action.anno.Select;
import io.github.ihongs.db.DB;
import io.github.ihongs.serv.matrix.Form;
import io.github.ihongs.serv.matrix.FormAction;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Hongs
 */
@Action("centre/medium/matrix/form")
public class MatrixFormAction extends FormAction {
    
    protected final Form model;

    public MatrixFormAction() throws HongsException {
        model = (Form) DB.getInstance("medium_matrix").getModel("form");
    }
    
    @Action("list")
    @Select(conf="centre/medium/matrix", form="form")
    @Preset(conf="centre/medium/matrix", form="form")
    @Override
    public void getList(ActionHelper helper)
    throws HongsException {
        super.getList(helper);
    }
    
    @Action("info")
    @Select(conf="centre/medium/matrix", form="form")
    @Preset(conf="centre/medium/matrix", form="form")
    @Override
    public void getInfo(ActionHelper helper)
    throws HongsException {
        super.getInfo(helper);
    }

    @Action("save")
    @Preset(conf="centre/medium/matrix", form="form", defs=":update")
    @Override
    public void doSave(ActionHelper helper)
    throws HongsException {
        super.doSave(helper);
    }

    @Action("delete")
    @Preset(conf="centre/medium/matrix", form="form", defs=":delete")
    @CommitSuccess
    @Override
    public void doDelete(ActionHelper helper)
    throws HongsException {
        super.doDelete(helper);
    }

}
