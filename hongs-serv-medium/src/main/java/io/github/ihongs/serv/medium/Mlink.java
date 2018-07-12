package io.github.ihongs.serv.medium;

import io.github.ihongs.HongsException;
import io.github.ihongs.db.Model;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.util.Synt;
import java.util.Map;

/**
 * 链表模型
 * 必须含有 link_id,link 字段, 类似 comment,dissent,endorse,impress,statist
 * @author Hongs
 */
public class Mlink extends Model {

    protected final String LINK   ;
    protected final String LINK_ID;
    private         String link    = null;
    private         String link_id = null;

    public Mlink(Table table) throws HongsException {
        super(table);
        LINK    = Synt.declare(table.getParams().get("field.link"   ), "link"   );
        LINK_ID = Synt.declare(table.getParams().get("field.link_id"), "link_id");
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getLink() {
        return this.link;
    }

    public void setLinkId(String linkId) {
        this.link_id=linkId;
    }

    public String getLinkId() {
        return this.link_id;
    }

    @Override
    public int add(String id, Map rd) throws HongsException {
        if (null != link_id) {
            rd.put( LINK_ID, link_id);
        }
        if (null != link) {
            rd.put( LINK, link );
        }
        return super.add(id, rd);
    }

    @Override
    public int put(String id, Map rd) throws HongsException {
        if (null != link_id) {
            rd.put( LINK_ID, link_id);
        }
        if (null != link) {
            rd.put( LINK, link );
        }
        return super.put(id, rd);
    }

    @Override
    protected void filter(FetchCase fc, Map rd) throws HongsException {
        super.filter(fc, rd);
        String tn = Synt.defoult(fc.getName(  ), table.name);
        if (null != link_id) {
            fc.filter("`"+tn+"`.`"+LINK_ID+"` = ?", link_id);
        } else
        if (!rd.containsKey(LINK_ID)) {
            fc.filter("`"+tn+"`.`"+LINK_ID+"` IS NULL");
        }
        if (null != link) {
            fc.filter("`"+tn+"`.`"+LINK+"` = ?", link );
        } else
        if (!rd.containsKey(LINK)) {
            fc.filter("`"+tn+"`.`"+LINK_ID+"` IS NULL");
        }
    }

}
