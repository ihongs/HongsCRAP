package io.github.ihongs.serv.medium;

import io.github.ihongs.HongsException;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;
import io.github.ihongs.serv.matrix.Form;

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
