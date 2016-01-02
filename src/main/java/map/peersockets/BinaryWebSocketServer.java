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
	}

	@OnClose
	public void onClose(Session session) {
	  sessions.remove(session);
	}
	
	@OnMessage
	public void onMessage(ByteBuffer byteBuffer) {
	  for (Session session : sessions) {
	    try {
	      session.getBasicRemote().sendBinary(byteBuffer);
	    } catch (IOException ex) {
	      Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.SEVERE, null, ex);
	    }
	  }
	}
}
