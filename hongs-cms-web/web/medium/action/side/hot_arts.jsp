<%@page import="foo.hongs.HongsException"%>
<%@page import="foo.hongs.db.DB"%>
<%@page import="foo.hongs.db.FetchCase"%>
<%@page import="foo.hongs.db.Model"%>
<%@page import="foo.hongs.db.Table"%>
<%@page import="foo.hongs.db.link.Loop"%>
<%@page import="foo.hongs.serv.medium.ABaseModel"%>
<%@page import="java.util.HashSet"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Set"%>
<%@page contentType="text/html" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%!
    List<Map> getHotArts(Model mod, String sid) throws HongsException {
        Table  sec = mod.db.getTable("section");
        Table  seg = mod.db.getTable("segment");
        Set<String> sids = new HashSet();
        Set<String> sidz = new HashSet();
        sidz.add(sid);
        
        do {
            Set<String> sidx = new HashSet();
            Loop roll = sec.fetchCase()
                .where  ("pid IN (?)", sidz)
                .select ("id")
                .oll    ();
            Map row;
            while (roll.hasNext()) {
                row = roll.next();
                sid = (String) row.get("id");
                sids.add(sid);
                sidx.add(sid);
            }
            sidz = sidx;
        } while (! sidz.isEmpty());
        
        FetchCase fc = mod.table.fetchCase()
                .where  ("state > ?", 0)
                .assort ("score DESC")
                .select ("id, name")
                .limit  (10);
        fc.join   (seg.tableName, seg.name )
          .on     ("link_id = :id" )
          .where  ("sect_id IN (?)" , sids );
        
        return fc.all();
    }
%>
<%
    ABaseModel  mod = (ABaseModel) DB.getInstance("medium").getModel("article");
                mod.setType("default");
    String      sid = request.getParameter("sid");
    String     root = request.getContextPath();
    List<Map>  arts = getHotArts(mod, sid);
%>
<%if (arts != null && !arts.isEmpty()) {%>
<!-- 热门文章 -->
<div>
    <h4>热门文章</h4>
    <%for (Map info : arts) {%>
    <div><a href="<%=root%>/medium/article/<%=info.get("id")%>"><%=info.get("name")%></a></div>
    <%} /*End for*/%>
</div>
<%} /*End if*/%>