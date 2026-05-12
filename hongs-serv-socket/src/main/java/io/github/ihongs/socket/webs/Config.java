package io.github.ihongs.socket.webs;

import io.github.ihongs.action.SocketHelper;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;
import java.util.Map;

/**
 * WebSocket 配置器
 * 用于初始化请求环境和记录 HttpSession 等
 * 用于 \@ServerEndpoint(value="/xxx" configurator=SocketHelper.config)
 */
public class Config extends ServerEndpointConfig.Configurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
        Map head = request.getHeaders();
        Map data = request.getParameterMap();
        Map prop = config.getUserProperties();
        prop.put(SocketHelper.class.getName() + ".httpHeaders", head);
        prop.put(SocketHelper.class.getName() + ".httpRequest", data);
        prop.put(HttpSession.class.getName(), request.getHttpSession());
    }
    
}
