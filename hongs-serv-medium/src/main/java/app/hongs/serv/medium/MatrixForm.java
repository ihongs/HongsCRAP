package app.hongs.serv.medium;

import app.hongs.HongsException;
import app.hongs.db.DB;
import app.hongs.db.Table;
import app.hongs.serv.matrix.Form;

/**
 *
 * @author Hongs
 */
public class MatrixForm extends Form  {

    public MatrixForm(Table table)
    throws HongsException {
        super(table);
    }

    public MatrixForm() throws HongsException {
        this(DB.getInstance("medium").getTable("matrix_form"));
        this.centra = "centra/medium/matrix/data";
    }

}
