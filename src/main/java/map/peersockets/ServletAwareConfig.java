package map.peersockets;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class ServletAwareConfig extends ServerEndpointConfig.Configurator {

    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
    	try{
	    	Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "Headers:" + request.getHeaders().toString());
	    	Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "Headers:" + response.getHeaders().toString());
	    	if (request.getHeaders().containsKey("user-agent")) {
	    		config.getUserProperties().put("user-agent", request.getHeaders().get("user-agent").get(0)); 
	        }
	    	if (request.getHeaders().containsKey("x-forwarded-for")) { //X-Forwarded-For
	    		config.getUserProperties().put("origin", request.getHeaders().get("x-forwarded-for").get(0)); 
	        }
	    } catch (Exception ex) {
		      Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
		    }
	    }

}