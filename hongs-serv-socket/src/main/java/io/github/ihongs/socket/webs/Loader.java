package io.github.ihongs.socket.webs;

import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreRoster;
import io.github.ihongs.CruxExemption;
import io.github.ihongs.server.init.Initer;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;

/**
 * WebSocket 加载器
 * 使用 defines.properties 设置 apply.sock 来告知 ServletContext 要加载哪些 WebSocket 类
 * 多个类名使用分号";"分隔
 */
public class Loader implements Initer {

    @Override
    public void init(ServletContextHandler context) {
        JakartaWebSocketServletContainerInitializer.configure(context, (servletContext, cont) -> {
            String pkgx = CoreConfig.getInstance("defines").getProperty("apply.sock");
            if (pkgx != null) {
                String[] pkgs = pkgx.split(";");
                for (String pkgn : pkgs) {
                    pkgn = pkgn.trim();
                    if (pkgn.length() == 0) {
                        continue;
                    }
                    Set<String> clss = getClss(pkgn);
                    for (String clsn : clss) {
                        Class clso = getClso(clsn);
                        ServerEndpoint anno = (ServerEndpoint) clso.getAnnotation(ServerEndpoint.class);
                        if (anno != null) {
                            try {
                                cont.addEndpoint(clso);
                            } catch (Exception ex) {
                                throw new CruxExemption(ex);
                            }
                        }
                    }
                }
            }
        });
    }

    private Class getClso(String clsn) {
        Class clso;
        try {
            clso = Class.forName(clsn);
        } catch (ClassNotFoundException ex) {
            throw new CruxExemption(ex, "Can not find class '" + clsn + "'.");
        }
        return clso;
    }

    private Set<String> getClss(String pkgn) {
        Set<String> clss;
        if (pkgn.endsWith(".**")) {
            pkgn = pkgn.substring(0, pkgn.length() - 3);
            try {
                clss = CoreRoster.getClassNames(pkgn, true);
            } catch (IOException ex) {
                throw new CruxExemption(ex, "Can not load package '" + pkgn + "'.");
            }
            if (clss == null) {
                throw new CruxExemption("Can not find package '" + pkgn + "'.");
            }
        } else if (pkgn.endsWith(".*")) {
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
            clss.add(pkgn);
        }
        return clss;
    }
    
}
