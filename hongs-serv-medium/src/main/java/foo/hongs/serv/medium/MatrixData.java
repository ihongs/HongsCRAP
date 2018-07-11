package foo.hongs.serv.medium;

import foo.hongs.HongsException;
import foo.hongs.db.DB;
import foo.hongs.db.Model;
import foo.hongs.db.Table;
import foo.hongs.serv.matrix.Data;
import foo.hongs.util.Synt;

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
