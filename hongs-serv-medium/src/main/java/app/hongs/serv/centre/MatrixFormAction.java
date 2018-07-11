package app.hongs.serv.centre;

import app.hongs.CoreLocale;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.CommitSuccess;
import app.hongs.action.anno.Preset;
import app.hongs.action.anno.Select;
import app.hongs.db.DB;
import app.hongs.serv.matrix.Form;
import app.hongs.serv.matrix.FormAction;
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
