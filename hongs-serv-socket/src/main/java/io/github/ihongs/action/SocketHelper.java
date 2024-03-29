package io.github.ihongs.action;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CoreRoster;
import io.github.ihongs.CruxExemption;
import io.github.ihongs.server.init.Initer;
import io.github.ihongs.util.Dist;
import io.github.ihongs.util.Inst;
import io.github.ihongs.util.Synt;

import java.io.Writer;
import java.io.OutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.TimeZone;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import javax.websocket.Session;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

/**
 * WebSocket 助手类
 *
 * <code>
 *  // 事件处理器示例:
 *  @ServerEndpoint(value="/sock/path/{xxx}", configurator=SocketHelper.Config.class)
 *  public class Xxxx {
 *      @OnYyyy
 *      public void onYyyy(Session zz) {
 *          try (
 *              SocketHelper sh = SocketHelper.getInstance(zz);
 *          ) {
 *              // TODO: Something ...
 *          }
 *          catch (Error|Exception ex) {
 *              CoreLogger.error ( ex);
 *          }
 *      }
 *  }
 * </code>
 * <pre>
 *  编辑 defines.properties,
 *  在 apply.sock 配置中加入 前面定义的类的完整路径
 *  在 jetty.init 配置中加入 io.github.ihongs.action.ScoketHelper.Loader
 * </pre>
 *
 * @author Hongs
 */
public class  SocketHelper extends ActionHelper implements AutoCloseable {

    protected SocketHelper(Map data, Map prop) {
        super(data, prop, null, null);

        /**
         * 放入 UserProperties 中以便随时取用
         */
        prop.put(SocketHelper.class.getName(), this);
        prop.put(ActionHelper.class.getName(), this);
    }

    /**
     * 更新环境
     * @param core
     * @param sess
     */
    protected void updateHelper(Core core, Session sess) {
        Map prop = sess.getUserProperties( );
        prop.put(Core.class.getName(), core);
        prop.put(Session.class.getName(), sess);

        /**
         * 放入 Core 中以便在动作线程内随时取用
         */
        core.set(SocketHelper.class.getName(), this);
        core.set(ActionHelper.class.getName(), this);

        /**
         * 在 Jetty 中(其他的容器还没试)
         * 每次事件都有可能在不同的线程中
         * Core 却是为常规 Servlet 设计的
         * 故需每次事件判断 Core 与会话匹配否
         * 不匹配则重新设置 Core 动作环境信息
         */
        Core.ACTION_TIME.set(System.currentTimeMillis());
        Core.ACTION_NAME.set(Synt.declare(prop.get("ACTION_NAME"), ""));
        Core.ACTION_LANG.set(Synt.declare(prop.get("ACTION_LANG"), ""));
        Core.ACTION_ZONE.set(Synt.declare(prop.get("ACTION_ZONE"), ""));
        Core.CLIENT_ADDR.set(Synt.declare(prop.get("CLIENT_ADDR"), ""));
    }

    /**
     * 构建环境
     * @param core
     * @param sess
     */
    protected void createHelper(Core core, Session sess) {
        Map prop = sess.getUserProperties( );
        prop.put(Core.class.getName(), core);
        prop.put(Session.class.getName(), sess);

        /**
         * 放入 Core 中以便在动作线程内随时取用
         */
        core.set(SocketHelper.class.getName(), this);
        core.set(ActionHelper.class.getName(), this);

        /**
         * 按照 Servlet 过程一样初始化动作环境
         */

        Core.ACTION_TIME.set(System.currentTimeMillis());

        String name = sess . getRequestURI( ).getPath( );
        if (name.length() >= Core.SERV_PATH.length()+1 ) {
            Core.ACTION_NAME.set(name.substring(Core.SERV_PATH.length() + 1));
        } else {
            throw new CruxExemption("Wrong web socket uri: "+ name );
        }

        InetSocketAddress addr = (InetSocketAddress)
            getAttribute( "javax.websocket.endpoint.remoteAddress" );
        if (addr != null) {
            Core.CLIENT_ADDR.set(addr.getAddress().getHostAddress());
        }

        CoreConfig conf = core.got(CoreConfig.class);

        Core.ACTION_LANG.set(conf.getProperty("core.language.default", Cnst.LANG_DEF));
        if (conf.getProperty("core.language.probing", false)) {
            /**
             * 语言可以记录到 Session 里
             */
            name = conf.getProperty("core.language.session", Cnst.LANG_KEY);
            name = (String) this.getSessibute(name);

            /**
             * 通过 WebSocket Headers 提取语言选项
             */
            if (name == null || name.length() == 0) {
                do {
                    Map <String, List<String>> headers;
                    headers  = ( Map <String, List<String>> )
                        this.getAttribute(SocketHelper.class.getName()+".httpHeaders");
                    if (headers == null) {
                        break ;
                    }
                    List<String> headerz;
                    headerz  = headers.get("Accept-Language");
                    if (headerz == null) {
                        break ;
                    }
                    name = headerz.isEmpty() ? headerz.get(0): null;
                } while(false);
            }

            /**
             * 检查是否是支持的语言
             */
            if (name != null) {
                name  = CoreLocale.getAcceptLanguage(name);
            if (name != null) {
                Core.ACTION_LANG.set(name);
            }
            }
        }

        Core.ACTION_ZONE.set(conf.getProperty("core.timezone.default", Cnst.ZONE_DEF));
        if (conf.getProperty("core.timezone.probing", false)) {
            /**
             * 时区可以记录到 Session 里
             */
            name = conf.getProperty("core.timezone.session", Cnst.ZONE_KEY);
            name = (String) this.getSessibute(name);

            /**
             * 通过 WebSocket Headers 提取时区选项
             */
            if (name == null || name.length() == 0) {
                do {
                    Map <String, List<String>> headers;
                    headers  = ( Map <String, List<String>> )
                        this.getAttribute(SocketHelper.class.getName()+".httpHeaders");
                    if (headers == null) {
                        break ;
                    }
                    List<String> headerz;
                    headerz  = headers.get(/**/ "X-Timezone");
                    if (headerz == null) {
                        break ;
                    }
                    name = headerz.isEmpty() ? headerz.get(0): null;
                } while(false);
            }

            /**
             * 检查是否是正确的时区
             */
            if (name != null) {
                name  = TimeZone.getTimeZone(name).getID();
//          if (zone != null) {
                Core.ACTION_ZONE.set(name);
//          }
            }
        }

        /**
         * 写入当前会话备用
         */
        setAttribute("ACTION_TIME", Core.ACTION_TIME.get());
        setAttribute("ACTION_NAME", Core.ACTION_NAME.get());
        setAttribute("ACTION_LANG", Core.ACTION_LANG.get());
        setAttribute("ACTION_ZONE", Core.ACTION_ZONE.get());
        setAttribute("CLIENT_ADDR", Core.CLIENT_ADDR.get());
    }

    /**
     * 销毁环境
     */
    @Override
    public void close() {
        Core core = getCore();
        String hn = SocketHelper.class.getName( );
        String kn = ActionHelper.class.getName( );

        if (!core.exists(kn)) {
            return;
        }

        try {
            this.flush();
        } catch (CruxExemption e) {
            CoreLogger . error(e);
        }

        if (4 == (4 & Core.DEBUG)) {
            long time = System.currentTimeMillis(  ) - Core.ACTION_TIME.get( );
            String dn = Synt.declare(core.remove(hn + ":event"), "...");
            StringBuilder sb = new StringBuilder(dn);
              sb.append("\r\n\tACTION_NAME : ").append(Core.ACTION_NAME.get())
                .append("\r\n\tACTION_TIME : ").append(Core.ACTION_TIME.get())
                .append("\r\n\tACTION_LANG : ").append(Core.ACTION_LANG.get())
                .append("\r\n\tACTION_ZONE : ").append(Core.ACTION_ZONE.get())
                .append("\r\n\tObjects     : ").append(core.toString ( /**/ ))
                .append("\r\n\tRuntime     : ").append(Inst.  phrase ( time ));
            CoreLogger.debug(sb.toString());
        }

        // 先移出自身, 规避递归调用导致死循环
        core.remove(kn);
        core.remove(hn);

        core.reset();
    }

    /**
     * @see  close
     * @deprecated
     */
    public void destroy() {
        close();
    }

    public Core getCore() {
        return (Core) getAttribute(Core.class.getName());
    }

    public Session getSockSession() {
        return (Session) getAttribute(Session.class.getName());
    }

    public HttpSession getHttpSession() {
        return (HttpSession) getAttribute(HttpSession.class.getName());
    }

    /**
     * @deprecated 必须通过 Session 获取, 总是抛出异常
     * @throws UnsupportedOperationException
     * @return 
     */
    public static SocketHelper newInstance() {
        throw new UnsupportedOperationException("Unsupported get instance without session");
    }

    /**
     * @deprecated 必须通过 Session 获取, 总是抛出异常
     * @throws UnsupportedOperationException
     * @return 
     */
    public static SocketHelper getInstance() {
        throw new UnsupportedOperationException("Unsupported get instance without session");
    }

    /**
     * 获取实例, 登记事件
     * @param sess
     * @param name
     * @return
     */
    public static SocketHelper getInstance(Session sess , String name) {
        SocketHelper inst = getInstance(sess);
        inst.getCore().set(SocketHelper.class.getName()+":event",name);
        return inst;
    }

    /**
     * 获取实例
     * @param sess
     * @return
     */
    public static SocketHelper getInstance(Session sess) {
        Core         core = Core.getInstance();
        Map          prop = sess.getUserProperties ();
        SocketHelper hepr = (SocketHelper) prop.get(SocketHelper.class.getName());
        SocketHelper hepc = (SocketHelper) core.get(SocketHelper.class.getName());

        if (hepr == null) {
            /**
             * 提取和整理请求数据
             * 且合并路径上的参数
             */
            Map data  = (Map) prop.get(SocketHelper.class.getName()+".httpRequest");
            if (data == null) {
                data  = sess.getRequestParameterMap();
                data  = ActionHelper.parseParan(data);
            }   data.putAll(sess.getPathParameters());

            hepr = new SocketHelper(data, prop);
            hepr.createHelper(core, sess);
        } else
        if (hepc == null || ! hepc.equals(hepr)) {
            hepr.updateHelper(core, sess);
        }

        /**
         * 设置会话基础参数
         */
        CoreConfig cc = CoreConfig.getInstance();
        int nn = cc.getProperty("core.socket.max.idle.timeout", 0);
        if (nn != 0) sess.setMaxIdleTimeout            (nn);
        nn = cc.getProperty("core.socket.max.txt.msg.buf.size", 0);
        if (nn != 0) sess.setMaxTextMessageBufferSize  (nn);
        nn = cc.getProperty("core.socket.max.bin.msg.buf.size", 0);
        if (nn != 0) sess.setMaxBinaryMessageBufferSize(nn);

        return hepr;
    }

    /**
     * 获取 HttpSession
     * 获取 WebSocket Session 请使用 getAttribute
     * @param name
     * @return
     */
    @Override
    public Object getSessibute(String  name) {
        HttpSession hsess = getHttpSession();
        if (null != hsess) {
            return  hsess.getAttribute(name);
        } else {
            return  null ;
        }
    }

    /**
     * 设置 HttpSession
     * 设置 WebSocket Session 请使用 setAttribute
     * @param name
     * @param value
     */
    @Override
    public void setSessibute(String name, Object value) {
        HttpSession hsess = getHttpSession();
        if (null != hsess) {
        if (null != value) {
            hsess.setAttribute(name , value);
        } else {
            hsess.removeAttribute(name);
        }}
    }

    /**
     * 获取 Cookies
     * @param name
     * @return
     */
    @Override
    public String getCookibute(String name) {
        Map head = (Map ) getAttribute(SocketHelper.class.getName() + ".httpHeaders");
        if (head == null) {
            return  null;
        }
        List<String> cook = (List) head.get("Cookie");
        if (cook == null) {
            return  null;
        }

        name = encode(name);

        for(String cok : cook) {
            String key ;
            int beg = 0;
            int end = 0;
            while ((end = cok.indexOf("=", beg)) != -1) {
                key = cok . substring(beg, end).trim( );
                beg = end + 1;
                if (! key.equals(name)) {
                    beg = cok.indexOf(";", beg);
                    if (beg == -1) {
                        break;
                    }
                } else {
                    end = cok.indexOf(";", beg);
                    if (end == -1) {
                        return decode(cok.substring(beg/**/).trim());
                    } else {
                        return decode(cok.substring(beg,end).trim());
                    }
                }
            }
        }

        return null;
    }

    /**
     * @deprecated 不支持写 Cookie
     * @param name
     * @param value
     */
    @Override
    public void setCookibute(String name, String value) {
        throw new UnsupportedOperationException("Can not set cookie in web socket");
    }

    /**
     * @deprecated 不支持写 Cookie
     * @param name
     * @param value
     */
    @Override
    public void setCookibute(String name, String value, int life, String path, String host, boolean httpOnly, boolean secuOnly) {
        throw new UnsupportedOperationException("Can not set cookie in web socket");
    }

    /**
     * 获取输出流
     * @return
     */
    @Override
    public OutputStream getOutputStream() {
        Session sess = getSockSession();
        if (null != sess) {
            try {
                return sess.getBasicRemote().getSendStream();
            } catch (IOException ex) {
                throw new CruxExemption(ex, 1110, "Can not get socket stream.");
            }
        }
        return super.getOutputStream();
    }

    /**
     * 获取输出器
     * @return
     */
    @Override
    public Writer getOutputWriter() {
        Session sess = getSockSession();
        if (null != sess) {
            try {
                return sess.getBasicRemote().getSendWriter();
            } catch (IOException ex) {
                throw new CruxExemption(ex, 1110, "Can not get socket writer.");
            }
        }
        return super.getOutputWriter();
    }

    /**
     * 输出数据
     */
    @Override
    public void flush() {
        Map dat  = getResponseData( );
        if (dat != null) {
            super.reply( (Map) null );
            write(Dist.toString(dat));
        }
    }

    /**
     * 输出文本
     * @param txt
     */
    @Override
    public void write(String txt) {
        Session sess = getSockSession();
        if (null == sess) {
            throw new CruxExemption(1110, "Session not exist.");
        }
        if (! sess.isOpen()) {
            throw new CruxExemption(1110, "Session is closed.");
        }
        try {
            sess.getBasicRemote().sendText(txt);
        } catch ( IOException e ) {
            throw new CruxExemption(e, 1110, "Can not send to remote.");
        }
    }

    /**
     * @deprecated WebSocket 中不支持
     */
    @Override
    public void write(String ct, String txt) {
        throw new UnsupportedOperationException("Can not set content-type in web socket");
    }

    /**
     * @deprecated WebSocket 中不支持
     */
    @Override
    public void error(int sc, String msg) {
        throw new UnsupportedOperationException("Can not send error in web socket");
    }

    /**
     * @deprecated WebSocket 中不支持
     */
    @Override
    public void ensue(int sc, String url) {
        throw new UnsupportedOperationException("Can not exec ensue in web socket");
    }

    /**
     * @deprecated WebSocket 中不支持
     */
    @Override
    public void ensue(int sc, String url, String msg) {
        throw new UnsupportedOperationException("Can not exec ensue in web socket");
    }

    private String encode(String n) {
        try {
            return URLEncoder.encode(n, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    private String decode(String v) {
        try {
            return URLDecoder.decode(v, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    /**
     * WebSocket 配置器
     * 用于初始化请求环境和记录 HttpSession 等
     * 用于 \@ServerEndpoint(value="/xxx" configurator=SocketHelper.config)
     */
    static public class Config extends ServerEndpointConfig.Configurator {
        @Override
        public void modifyHandshake(
           ServerEndpointConfig   config,
                HandshakeRequest  request,
                HandshakeResponse response)
        {
            Map head = request.getHeaders();
            Map data = request.getParameterMap(/**/);
                data = ActionHelper.parseParan(data);

            Map prop = config.getUserProperties();
            prop.put(SocketHelper.class.getName()+".httpHeaders", head);
            prop.put(SocketHelper.class.getName()+".httpRequest", data);
            prop.put( HttpSession.class.getName(), request.getHttpSession());
        }
    }

    /**
     * WebSocket 加载器
     * 使用 defines.properties 设置 apply.sock 来告知 ServletContext 要加载哪些 WebSocket 类
     * 多个类名使用分号";"分隔
     */
    static public class Loader implements Initer {

        @Override
        public void init(ServletContextHandler context) {
            ServerContainer cont;
            try {
                cont = WebSocketServerContainerInitializer.configureContext( context );
            } catch (ServletException ex) {
                throw new CruxExemption(ex);
            }

            String pkgx  = CoreConfig.getInstance("defines").getProperty("apply.sock");
            if  (  pkgx != null ) {
                String[]   pkgs = pkgx.split(";");
                for(String pkgn : pkgs) {
                    pkgn = pkgn.trim  ( );
                    if  (  pkgn.length( ) == 0  ) {
                        continue;
                    }

                    Set<String> clss = getClss(pkgn);
                    for(String  clsn : clss) {
                        Class   clso = getClso(clsn);

                        ServerEndpoint anno = (ServerEndpoint) clso.getAnnotation(ServerEndpoint.class);
                        if (anno != null) {
                            try {
                              cont.addEndpoint(clso);
                            } catch ( Exception ex ) {
                              throw new CruxExemption(ex);
                            }
                        }
                    }
                }
            }
        }

        private Class getClso(String clsn) {
            Class  clso;
            try {
                clso = Class.forName(clsn);
            } catch (ClassNotFoundException ex ) {
                throw new CruxExemption(ex, "Can not find class '" + clsn + "'.");
            }
            return clso;
        }

        private Set<String> getClss(String pkgn) {
            Set<String> clss;

            if (pkgn.endsWith(".**")) {
                pkgn = pkgn.substring(0, pkgn.length() - 3);
                try {
                    clss = CoreRoster.getClassNames(pkgn, true );
                } catch (IOException ex) {
                    throw new CruxExemption(ex, "Can not load package '" + pkgn + "'.");
                }
                if (clss == null) {
                    throw new CruxExemption("Can not find package '" + pkgn + "'.");
                }
            } else
            if (pkgn.endsWith(".*" )) {
                pkgn = pkgn.substring(0, pkgn.length() - 2);
                try {
                    clss = CoreRoster.getClassNames(pkgn, false);
                } catch (IOException ex) {
                    throw new CruxExemption(ex, "Can not load package '" + pkgn + "'.");
                }
                if (clss == null) {
                    throw new CruxExemption("Can not find package '" + pkgn + "'.");
                }
            } else {
                clss = new HashSet();
                clss.add  (  pkgn  );
            }

            return clss;
        }

    }

}
