package foo.hongs.serv.centra;

import foo.hongs.HongsException;
import foo.hongs.action.ActionHelper;
import foo.hongs.action.anno.Action;
import foo.hongs.action.anno.Select;
import foo.hongs.db.DB;
import foo.hongs.serv.matrix.Form;
import foo.hongs.serv.matrix.FormAction;

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
