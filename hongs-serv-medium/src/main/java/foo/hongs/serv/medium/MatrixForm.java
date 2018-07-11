package foo.hongs.serv.medium;

import foo.hongs.HongsException;
import foo.hongs.db.DB;
import foo.hongs.db.Table;
import foo.hongs.serv.matrix.Form;

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
