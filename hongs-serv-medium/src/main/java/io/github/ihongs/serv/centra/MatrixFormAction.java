package io.github.ihongs.serv.centra;

import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.Select;
import io.github.ihongs.db.DB;
import io.github.ihongs.serv.matrix.Form;
import io.github.ihongs.serv.matrix.FormAction;

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
