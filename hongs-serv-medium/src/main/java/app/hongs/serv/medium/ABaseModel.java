package app.hongs.serv.medium;

import app.hongs.HongsException;
import app.hongs.db.Model;
import app.hongs.db.Table;
import app.hongs.db.util.FetchCase;
import app.hongs.util.Synt;
import java.util.Map;

/**
 * 基础表模型
 * 类似 article,section
 * @author Hongs
 */
public class ABaseModel extends Model {
    
    protected String type;

    public ABaseModel(Table table) throws HongsException {
        super(table);
    }
    
    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String add(Map rd) throws HongsException {
        rd.put("type", type);
        return super.add(rd);
    }

    @Override
    public int put(String id, Map rd, FetchCase fc) throws HongsException {
        rd.put("type", type);
        return super.put(id, rd, fc);
    }

    @Override
    protected void filter(FetchCase fc, Map rd) throws HongsException {
        super.filter(fc, rd);
        String tn = Synt.defoult(fc.getName(), table.name);
        if (type == null) {
            fc.filter("`"+tn+"`.`type` IS NULL");
        } else {
            fc.filter("`"+tn+"`.`type`=?", type);
        }
    }

}
