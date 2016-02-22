package map.peersockets;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class ServletAwareConfig extends ServerEndpointConfig.Configurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
    	
    	if (request.getHeaders().containsKey("user-agent")) {
    		config.getUserProperties().put("user-agent", request.getHeaders().get("user-agent").get(0)); // lower-case!
        }
    	if (request.getHeaders().containsKey("origin")) {
    		config.getUserProperties().put("origin", request.getHeaders().get("origin").get(0)); // lower-case!
        }
    	
    }

}