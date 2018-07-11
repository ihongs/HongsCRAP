package foo.hongs.serv.centre;

import foo.hongs.CoreLocale;
import foo.hongs.HongsException;
import foo.hongs.action.ActionHelper;
import foo.hongs.action.anno.Action;
import foo.hongs.action.anno.CommitSuccess;
import foo.hongs.action.anno.Preset;
import foo.hongs.action.anno.Select;
import foo.hongs.db.DB;
import foo.hongs.serv.matrix.Form;
import foo.hongs.serv.matrix.FormAction;
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
