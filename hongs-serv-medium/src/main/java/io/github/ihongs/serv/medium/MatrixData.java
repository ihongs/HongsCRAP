package io.github.ihongs.serv.medium;

import io.github.ihongs.HongsException;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Model;
import io.github.ihongs.db.Table;
import io.github.ihongs.serv.matrix.Data;
import io.github.ihongs.util.Synt;

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
