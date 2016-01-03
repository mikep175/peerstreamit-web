package map.peersockets;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/sockets")
public class BinaryWebSocketServer {
    
	private
	static final Set<Session> sessions =
	Collections.synchronizedSet(new HashSet<Session>());
	
	@OnOpen
	public void onOpen(Session session) {
	  sessions.add(session);
	  Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "New Session detected.");
	}

	@OnClose
	public void onClose(Session session) {
	  sessions.remove(session);
	  Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "Session closed.");
	}
	
	@OnMessage
	public void onMessage(String byteBuffer) {
	
	  Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "Message received.");
		
	  for (Session session : sessions) {
	    try {
	      session.getBasicRemote().sendText(byteBuffer);
	    } catch (IOException ex) {
	      Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.SEVERE, null, ex);
	    }
	  }
	}
}
