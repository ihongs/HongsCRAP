package app.hongs.serv.medium;

import app.hongs.HongsException;
import app.hongs.db.Model;
import app.hongs.db.Table;
import app.hongs.db.util.FetchCase;
import app.hongs.util.Synt;
import java.util.Map;

/**
 * 链表模型
 * 类似 comment,browses,consent,dissent
 * @author Hongs
 */
public class ALinkModel extends Model {

    protected String link = null;
    
    public ALinkModel(Table table) throws HongsException {
        super(table);
    }
    
    public void setLink(String link) {
        this.link = link;
    }

    public String getLink() {
        return this.link;
    }

    @Override
    public String add(Map rd) throws HongsException {
        rd.put("link", link);
        return super.add(rd);
    }

    @Override
    public int put(String id, Map rd, FetchCase fc) throws HongsException {
        rd.put("link", link);
        return super.put(id, rd, fc);
    }

    @Override
    protected void filter(FetchCase fc, Map rd) throws HongsException {
        super.filter(fc, rd);
        String tn = Synt.defoult(fc.getName(), table.name);
        if (link == null) {
            fc.filter("`"+tn+"`.`link` IS NULL");
        } else {
            fc.filter("`"+tn+"`.`link`=?", link);
        }
    }

}
