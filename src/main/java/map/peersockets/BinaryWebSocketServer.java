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

import javax.servlet.http.HttpSession;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint(value = "/sockets", configurator=ServletAwareConfig.class)
public class BinaryWebSocketServer {

	static final Map<String, Double> hlsNSIs =
			(Map<String, Double>)Collections.synchronizedMap(new HashMap<String, Double>());

	static final Map<String, String> hlsSessions =
			(Map<String, String>)Collections.synchronizedMap(new HashMap<String, String>());

	static final Map<String, byte[]> hlsFragsFinal =
			(Map<String, byte[]>)Collections.synchronizedMap(new HashMap<String, byte[]>());
	
	static final Map<String, byte[]> hlsFrags =
			(Map<String, byte[]>)Collections.synchronizedMap(new HashMap<String, byte[]>());
	
	private static final Map<String, String> challengeRequests =
			(Map<String, String>)Collections.synchronizedMap(new HashMap<String, String>());
	
	public static final Map<String, String> streamingRequests =
			(Map<String, String>)Collections.synchronizedMap(new HashMap<String, String>());
	
	static final Map<String, String> streamingSessions =
			(Map<String, String>)Collections.synchronizedMap(new HashMap<String, String>());
	
	private static final Map<String, List<String>> listeningKeys =
	(Map<String, List<String>>)Collections.synchronizedMap(new HashMap<String, List<String>>());

	private static final Set<Session> sessions =
			(Set<Session>)Collections.synchronizedSet(new HashSet<Session>());
	
	private String nextSessionId() {
		
		SecureRandom random = new SecureRandom();
		
	    return new BigInteger(130, random).toString(32);
	}
	
	@OnOpen
	public void onOpen(Session session, EndpointConfig config) {
		
		if(config.getUserProperties().containsKey("origin") == true) {
			
			session.getUserProperties().put("origin", config.getUserProperties().get("origin"));
		}
		
		if(config.getUserProperties().containsKey("user-agent") == true) {
			
			session.getUserProperties().put("user-agent", config.getUserProperties().get("user-agent"));
		}
		
		sessions.add(session);
		Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "New Session detected.");
	}

	@OnClose
	public void onClose(Session senderSession) {
		
		 if(streamingSessions.containsKey(senderSession.getId()) == true) {
			   
			  String destSessionId = streamingSessions.remove(senderSession.getId());
			  
			  for (Session session : sessions) {
			    try {
			    	if(session.getId().compareTo(destSessionId) == 0) {
			    		session.getBasicRemote().sendText("PSIKILL");
			    		Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "String sent.");
			    	}
			    } catch (IOException ex) {
			      Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.SEVERE, null, ex);
			    }
			  }
		 }
		 
		 else if(streamingSessions.values().contains(senderSession.getId()) == true) {
			  

			  String destSessionId = null;
			  
			  for(String ss : streamingSessions.keySet()) {
				  
				  if(streamingSessions.get(ss).compareTo(senderSession.getId()) == 0) {
					  
					  destSessionId = ss;
					  break;
				  }
			  }
			  
			  if(destSessionId != null) {
				  
				  streamingSessions.remove(destSessionId);
				  sendSessionMessage("PSIKILL", destSessionId);
			  }
		  }
		
		sessions.remove(senderSession);
		
		if(listeningKeys.containsKey(senderSession.getId()) == true) {
			
			listeningKeys.remove(senderSession.getId());
		}
		
		 
		
	    Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "Session closed.");
	}
	
	@OnMessage 
	public void onMessage(String message, Session senderSession) {
	
	  Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "Message received: " + message);
		
	   
	  //notify ps of psikey
	  if(message.indexOf("PSISERVKEY:") == 0) {
		  
		  
		  if(listeningKeys.containsKey(senderSession.getId()) == false) {
			  
			  listeningKeys.put(senderSession.getId(), new ArrayList<String>());
			  
		  }
		  
		  listeningKeys.get(senderSession.getId()).add(message.substring(11));
		  Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "Key Listening: " + senderSession.getId() + " - " + message.substring(11));
		  return;
	  }
	//notify ps of psikey remove
	  else if(message.indexOf("PSIREMOVESERVKEY:") == 0) {

		  if(streamingSessions.values().contains(senderSession.getId()) == true) {
			  

			  String destSessionId = null;
			  
			  for(String ss : streamingSessions.keySet()) {
				  
				  if(streamingSessions.get(ss).compareTo(senderSession.getId()) == 0) {
					  
					  destSessionId = ss;
					  break;
				  }
			  }
			  
			  if(destSessionId != null) {
				  
				  streamingSessions.remove(destSessionId);
				  sendSessionMessage("PSIKILL", destSessionId);
			  }
		  }
		  
		  if(listeningKeys.containsKey(senderSession.getId()) == true) {
			  
			  listeningKeys.remove(senderSession.getId());
			  
		  }
		  
		  listeningKeys.get(senderSession.getId()).add(message.substring(11));
		  Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "Key Listening: " + senderSession.getId() + " - " + message.substring(11));
		  return;
	  }
	  //check psikey for availability
	  else if(message.indexOf("PSICLIKEY:") == 0) {
		  
		  String psiKey = message.substring(10);
		  
		  
		  boolean hls = false;
		  
		  if(psiKey.indexOf(":HLS") >= 0) {
			  
			  hls = true;
			  psiKey = psiKey.replaceAll(":HLS", "");
			  Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "HLS Detected: " + senderSession.getId() + " - " + message.substring(11));
		  }
		  
		  boolean found = false;
		  
		  for(Map.Entry<String, List<String>> lk : listeningKeys.entrySet()) {
			  
			  if(lk.getValue().contains(psiKey) == true) {
				  
				  for (Session session : sessions) {
					    try {
					    	if(session.getId().compareTo(lk.getKey()) == 0) {
					    		
					    		String nsi = nextSessionId();
					    		
					    		streamingRequests.put(nsi, senderSession.getId());
					    		
					    		String origin = " ";
					    		
				    			String userAgent = " ";
					    		
					    		 for (Session sup : sessions) {
					 			    	if(sup.getId().compareTo(senderSession.getId()) == 0) {
					 			    		
					 			    		
								    		if(sup.getUserProperties().containsKey("origin") == true) {
								    			
								    			origin = (String) sup.getUserProperties().get("origin");
								    		}
								    		
								    		if(sup.getUserProperties().containsKey("user-agent") == true) {
								    			
								    			userAgent = (String) sup.getUserProperties().get("user-agent");
								    		}
					 			   
					 			    	}  
					    		 }
					    		
					    		session.getBasicRemote().sendText("PSICLIKEY:" + psiKey + ":" + nsi + ":" + origin + ":" + userAgent + ":" + (hls ? "1" : "0"));
					    		
					    		Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "Key found: " + message + " - " + nsi);
					    		
					    		found = true;
					    	}
					    } catch (IOException ex) {
					      Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
					    }
				  }
				  
			  }
			  
		  }
		  
		  if(found == false) {
			  try { 
				  senderSession.getBasicRemote().sendText("NOT");
				  
			  } catch (IOException ex) {
			      Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.SEVERE,  ex.getMessage(), ex);
			  }
		  }
		  
	  }
	  
	  //psikey requires pw
	  else if(message.indexOf("PSICHALLENGE:") == 0) {
		  
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
	  
	  //psikey max streams reached
	  else if(message.indexOf("PSIMAX:") == 0) {
		  
		  String nsi = message.substring(7);
		  String sid = streamingRequests.get(nsi);
		  
		  for (Session session : sessions) {
			    try {
			    	if(session.getId().compareTo(sid) == 0) {
			    		
			    		challengeRequests.put(sid, senderSession.getId());
			    		
			    		session.getBasicRemote().sendText("PSIMAX");
			    	}
			    } catch (IOException ex) {
			      Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.SEVERE, null, ex);
			    }
		  }
		  
		  //password challenge
	  }
	  
	  //psikey pw attempt
	  else if(message.indexOf("PSIAUTH:") == 0) {
		  
		  String pw = message.substring(8);
		  String sid = challengeRequests.get(senderSession.getId());
		  
		  for (Session session : sessions) {
			    try {
			    	
			    	if(session.getId().compareTo(sid) == 0) {
			    		
			    		String nsi = null;
			    		
			    		for(Entry<String, String> sr : streamingRequests.entrySet()) {
			    			
			    			if(sr.getValue().compareTo(senderSession.getId()) == 0) {
			    				
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
	  else if(message.indexOf("PSIAUTHREJECTED:") == 0) {
		  
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
	  else if(message.indexOf("PSIHLSINIT:") == 0) {
		  
		  String[] raw = message.substring(11).split(":");
		  
		  String nsi = raw[0];

		  String sid = streamingRequests.get(nsi);

  		  String id = nextSessionId();
  		  
		  for (Session session : sessions) {
			    try {
			    	if(session.getId().compareTo(sid) == 0) {
			    		
			    		
			    		hlsNSIs.put(nsi, new Double(raw[1]));
			  		    streamingSessions.put(sid, senderSession.getId());
			  		    streamingSessions.put(id, streamingRequests.get(nsi));
			    		session.getBasicRemote().sendText("HLSKEY:" + id);
			    	}
			    } catch (IOException ex) {
			      Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.SEVERE, null, ex);
			    }
		  }
		  
		  Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "PSIHLSINIT : " + id + " - " + nsi);
	  }
	  //new socket set to handle streaming
	  else if(message.indexOf("PSISTREAMHLS:") == 0) {
		  
		  String[] raw = message.substring(13).split(":");
		  
		  String nsi = raw[0];
		  
		  String sid = streamingRequests.get(nsi);
		  
		  hlsSessions.put(senderSession.getId(), nsi + ":" + raw[1]);
		  streamingSessions.put(senderSession.getId(), streamingRequests.get(nsi));
		  
		  Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "PSISTREAMHLS : " + senderSession.getId() + " - " + nsi);
	  }
	  //new socket set to handle streaming
	  else if(message.indexOf("PSIENDSTREAMHLS:") == 0) {
		  
		  String nsi = message.substring(16);
		  
		  String sid = streamingRequests.get(nsi);
		  
		  String hlsId = "";
		  
		  for(String key : streamingSessions.keySet()) {
			  
			  if(streamingSessions.get(key).compareTo(sid) == 0) {
				  
				  hlsId = key;
				  break;
				  
			  }
			  
		  }

		  if(hlsFrags.containsKey(hlsId)) {
			  
			  hlsFragsFinal.put(hlsId, hlsFrags.remove(hlsId));
			  
		  }
		  
		  Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "PSISTREAMHLS : " + senderSession.getId() + " - " + nsi);
	  }
	  //new socket set to handle streaming
	  else if(message.indexOf("PSISTREAM:") == 0) {
		  
		  String nsi = message.substring(10);
		  
		  streamingSessions.put(senderSession.getId(), streamingRequests.get(nsi));
		  Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "PSISTREAM : " + senderSession.getId() + " - " + nsi);
	  }
	  else if(message.indexOf("PSIWAIT:") == 0) {
		  
		  String nsi = message.substring(8);
		   
		  String destId = streamingSessions.remove(senderSession.getId());
		  
		  streamingSessions.put("NSI_WAIT:" + nsi, destId);
		  
		  Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "PSIWAIT : " + senderSession.getId() + " - " + nsi);
	  }
	  else if(message.indexOf("PSIRECONN:") == 0) {
		  
		  String nsi = message.substring(10);
		  
		  String destId = streamingSessions.remove("NSI_WAIT:" + nsi);
		  
		  streamingSessions.put(senderSession.getId(), destId);
		  
		  Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "PSIRECONN : " + senderSession.getId() + " - " + nsi);
	  }
	  else if(streamingSessions.containsKey(senderSession.getId()) == true) {
		  
		  String destSessionId = streamingSessions.get(senderSession.getId());
		  
		  sendSessionMessage(message, destSessionId);
		  
	  }	  
	  else if(streamingSessions.values().contains(senderSession.getId()) == true) {
		  
		  String destSessionId = null;
		  
		  for(String ss : streamingSessions.keySet()) {
			  
			  if(streamingSessions.get(ss).compareTo(senderSession.getId()) == 0) {
				  
				  destSessionId = ss;
				  break;
			  }
		  }
		  
		  if(destSessionId != null) {
		
			  sendSessionMessage(message, destSessionId);
		  }
	  }
	}

	static void sendSessionMessage(String message, String destSessionId) {
		for (Session session : sessions) {
		    try {
		    	if(session.getId().compareTo(destSessionId) == 0) {
		    		session.getBasicRemote().sendText(message);
		    		Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "String sent:" + message);
		    	}
		    } catch (IOException ex) {
		      Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.SEVERE, null, ex);
		    }
		  }
	}
	
	@OnMessage
	public void onMessage(ByteBuffer byteBuffer, boolean last, Session senderSession) {
	
	  Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "Binary received.");

	  if(hlsSessions.containsKey(senderSession.getId()) == true) {

		  String hlsId = hlsSessions.get(senderSession.getId());
		  byte[] b = new byte[byteBuffer.remaining()];
		  byteBuffer.get(b);
		  
		  if(hlsFrags.containsKey(hlsId)) {
			  
			  byte[] a = hlsFrags.get(hlsId);
			  
			  byte[] c = new byte[a.length + b.length];
			  System.arraycopy(a, 0, c, 0, a.length);
			  System.arraycopy(b, 0, c, a.length, b.length);
			  hlsFrags.put(hlsId, c);
		  } else {

			  hlsFrags.put(hlsId, b);
		  }
		  
		  
	  }
	  else {
		  String destSessionId = streamingSessions.get(senderSession.getId());
		  
		  if(destSessionId != null) {
			  
				  for (Session session : sessions) {
				    try {
				    	if(session.getId().compareTo(destSessionId) == 0) {
				    		session.getBasicRemote().sendBinary(byteBuffer);
				    		Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "Binary sent.");
				    	}
				    } catch (IOException ex) {
				      Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.SEVERE, null, ex);
				    }
				  }
			  }
		  }
	}
}
