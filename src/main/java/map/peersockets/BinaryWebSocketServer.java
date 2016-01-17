package map.peersockets;


import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/sockets")
public class BinaryWebSocketServer {
    
	private static final Map<String, String> challengeRequests =
			(Map<String, String>)Collections.synchronizedMap(new HashMap<String, String>());
	
	private static final Map<String, String> streamingRequests =
			(Map<String, String>)Collections.synchronizedMap(new HashMap<String, String>());
	
	private static final Map<String, String> streamingSessions =
			(Map<String, String>)Collections.synchronizedMap(new HashMap<String, String>());
	
	private static final Map<String, List<String>> listeningKeys =
	(Map<String, List<String>>)Collections.synchronizedMap(new HashMap<String, List<String>>());
	
	private static final (Set<Session> sessions =
			((SSet<Session>)Collections.synchronizedSet(new HashSet<Session>());
	
	private String nextSessionId() {
		
		SecureRandom random = new SecureRandom();
		
	    return new BigInteger(130, random).toString(32);
	}
	
	@OnOpen
	public void onOpen(Session session) {
		
		sessions.add(session);
	    
		Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "New Session detected.");
	}

	@OnClose
	public void onClose(Session session) {
		
		sessions.remove(session);
		
		if(listeningKeys.containsKey(session.getId()) == true) {
			
			listeningKeys.remove(session.getId());
		}
		
	    Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "Session closed.");
	}
	
	@OnMessage 
	public void onMessage(String message, Session senderSession) {
	
	  Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "Message received.");
		
	  
	  //notify ps of psikey
	  if(message.indexOf("PSISERVKEY:") == 0) {
		  
		  if(listeningKeys.containsKey(senderSession.getId()) == false) {
			  
			  listeningKeys.put(senderSession.getId(), new ArrayList<String>());
			  
		  }
		  
		  listeningKeys.get(senderSession.getId()).add(message.substring(11));
		  
		  return;
	  }
	  
	  //check psikey for availability
	  if(message.indexOf("PSICLIKEY:") == 0) {
		  
		  String psiKey = message.substring(10);
		  
		  for(Map.Entry<String, List<String>> lk : listeningKeys.entrySet()) {
			  
			  if(lk.getValue().contains(psiKey) == true) {
				  
				  for (Session session : sessions) {
					    try {
					    	if(session.getId().compareTo(lk.getKey()) == 0) {
					    		
					    		String nsi = nextSessionId();
					    		
					    		streamingRequests.put(nsi, senderSession.getId());
					    		
					    		session.getBasicRemote().sendText(message + ":" + nsi);
					    	}
					    } catch (IOException ex) {
					      Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.SEVERE, null, ex);
					    }
				  }
				  
			  }
			  
		  }
		  
	  }
	  
	  //psikey requires pw
	  if(message.indexOf("PSICHALLENGE:") == 0) {
		  
		  String nsi = message.substring(13);
		  String sid = streamingRequests.get(nsi);
		  
		  for (Session session : sessions) {
			    try {
			    	if(session.getId().compareTo(sid) == 0) {
			    		
			    		challengeRequests.put(sid, senderSession.getId());
			    		
			    		session.getBasicRemote().sendText("PSICHALLENGE");
			    	}
			    } catch (IOException ex) {
			      Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.SEVERE, null, ex);
			    }
		  }
		  
		  //password challenge
	  }
	  
	  //psikey pw attempt
	  if(message.indexOf("PSIAUTH:") == 0) {
		  
		  String pw = message.substring(9);
		  String sid = challengeRequests.get(senderSession.getId());
		  
		  for (Session session : sessions) {
			    try {
			    	
			    	if(session.getId().compareTo(sid) == 0) {
			    		
			    		String nsi = null;
			    		
			    		for(Entry<String, String> sr : streamingRequests.entrySet()) {
			    			
			    			if(sr.getValue().compareTo(sid) == 0) {
			    				
			    				nsi = sr.getKey();
			    				
			    				break;
			    			}
			    			
			    		}
			    		
			    		session.getBasicRemote().sendText("PSIAUTH:" + nsi + ":" + pw);
			    	}
			    } catch (IOException ex) {
			      Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.SEVERE, null, ex);
			    }
		  }
		  
	  }
	  
	  //psikey pw no good
	  if(message.indexOf("PSIAUTHREJECTED:") == 0) {
		  
		  String nsi = message.substring(16);
		  
		  String sid = streamingRequests.get(nsi);
		  
		  for (Session session : sessions) {
			    try {
			    	if(session.getId().compareTo(sid) == 0) {
			    		
			    		challengeRequests.remove(sid);
			    		
			    		session.getBasicRemote().sendText("PSIAUTHREJECTED");
			    	}
			    } catch (IOException ex) {
			      Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.SEVERE, null, ex);
			    }
		  }
	  }
	  
	  //new socket set to handle streaming
	  if(message.indexOf("PSISTREAM:") == 0) {
		  
		  String nsi = message.substring(10);
		  
		  streamingSessions.put(senderSession.getId(), streamingRequests.remove(nsi));
	  }
	  
	}
	
	@OnMessage
	public void onMessage(ByteBuffer byteBuffer, boolean last, Session senderSession) {
	
	  Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "Binary received.");
	  
	  String destSessionId = streamingSessions.get(senderSession.getId());
	  
	  for (Session session : sessions) {
	    try {
	    	if(session.getId().compareTo(destSessionId) == 0) {
	    		session.getBasicRemote().sendBinary(byteBuffer);
	    	}
	    } catch (IOException ex) {
	      Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.SEVERE, null, ex);
	    }
	  }
	}
}
