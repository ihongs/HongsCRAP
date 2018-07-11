package app.hongs.serv.centra;

import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Select;
import app.hongs.db.DB;
import app.hongs.serv.matrix.Form;
import app.hongs.serv.matrix.FormAction;

/**
 *
 * @author Hongs
 */
@Action("centra/medium/matrix/form")
public class MatrixFormAction extends FormAction {
    
    protected final Form model;

    public MatrixFormAction() throws HongsException {
        model = (Form) DB.getInstance("medium").getModel("matrix_form");
    }
    
    @Action("list")
    @Select(conf="centra/medium/matrix", form="form")
    @Override
    public void getList(ActionHelper helper)
    throws HongsException {
        super.getList(helper);
    }
    
    @Action("info")
    @Select(conf="centra/medium/matrix", form="form")
    @Override
    public void getInfo(ActionHelper helper)
    throws HongsException {
        super.getInfo(helper);
    }

}
