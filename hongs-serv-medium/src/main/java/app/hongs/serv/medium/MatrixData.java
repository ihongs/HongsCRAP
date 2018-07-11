package app.hongs.serv.medium;

import app.hongs.HongsException;
import app.hongs.db.DB;
import app.hongs.db.Model;
import app.hongs.db.Table;
import app.hongs.serv.matrix.Data;
import app.hongs.util.Synt;

/**
 *
 * @author Hongs
 */
public class MatrixData extends Data {
    
    public MatrixData(String conf, String form) throws HongsException {
        super( conf  ,  form
             , conf.replaceFirst("^(centre)/", "centra/")
             , conf.replaceFirst("^(centre|centra)/", "")
             , conf+"."+form);
    }
    
    public Model getModel() throws HongsException {
        String tn = Synt.declare(getParams().get("table.name"), "medium.matrix_data");
        if ("".equals(tn) || "none".equals(tn)) {
            return null;
        }
        return DB.getInstance("matrix").getModel(tn);
    }

    public Table getTable() throws HongsException {
        String tn = Synt.declare(getParams().get("table.name"), "medium.matrix_data");
        if ("".equals(tn) || "none".equals(tn)) {
            return null;
        }
        return DB.getInstance("matrix").getTable(tn);
    }

}
