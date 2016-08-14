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

@ServerEndpoint(value = "/blastsockets", configurator=ServletAwareConfig.class)
public class BlastWebSocketServer {

	
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
		Logger.getLogger(BlastWebSocketServer.class.getName()).log(Level.INFO, "New Session detected.");
	}

	@OnClose
	public void onClose(Session senderSession) {
		
		sessions.remove(senderSession);
		
		if(listeningKeys.containsKey(senderSession.getId()) == true) {
			
			listeningKeys.remove(senderSession.getId());
		}
		
	    Logger.getLogger(BlastWebSocketServer.class.getName()).log(Level.INFO, "Session closed.");
	}
	
	@OnMessage 
	public void onMessage(String message, Session senderSession) {
	
	  Logger.getLogger(BlastWebSocketServer.class.getName()).log(Level.INFO, "Message received: " + message);

	  //notify ps of psikey
	  if(message.indexOf("PSILOC:") == 0) {
		  
		  String[] raw = message.split(":");

		  if(listeningKeys.containsKey(senderSession.getId()) == false) {
			  ArrayList<String> keys = new ArrayList<String>();
			  keys.add(raw[1]);
			  keys.add(raw[2]);
			  keys.add(raw[3]);
			  listeningKeys.put(senderSession.getId(), keys);
		  }

		  listeningKeys.get(senderSession.getId()).set(0, raw[1]);
		  listeningKeys.get(senderSession.getId()).set(1, raw[2]);
		  listeningKeys.get(senderSession.getId()).set(2, raw[3]);
		  Logger.getLogger(BlastWebSocketServer.class.getName()).log(Level.INFO, "LOC: " + senderSession.getId() + " - " + message);
		  return;
	  }
	  //notify ps of psikey
	  if(message.indexOf("PSIBLAST:") == 0) {
		  
		  if(listeningKeys.containsKey(senderSession.getId()) == true) {

			  String[] raw = message.split(":");
			  
			  double latSender = Double.parseDouble(listeningKeys.get(senderSession.getId()).get(0));
			  double lonSender = Double.parseDouble(listeningKeys.get(senderSession.getId()).get(1));
			  
			  double radSender = Double.parseDouble(raw[1]);

			  for(String key : listeningKeys.keySet()) {
				  
				  if(key != senderSession.getId()) {

					  double lat = Double.parseDouble(listeningKeys.get(key).get(0));
					  double lon = Double.parseDouble(listeningKeys.get(key).get(1));
					  double rad = Double.parseDouble(listeningKeys.get(key).get(2));
					  
					  double distance = distance(lat, lon, latSender, lonSender, "M");
					  
					  if(radSender > distance && rad > distance) {
						  sendSessionMessage(raw[2] + ":" + raw[3], key);
					  }
				  }
			  }
			  
		  }
		  Logger.getLogger(BlastWebSocketServer.class.getName()).log(Level.INFO, "BLAST: " + senderSession.getId() + " - " + message);
		  return;
	  }
	}

	private static double distance(double lat1, double lon1, double lat2, double lon2, String unit) {
		double theta = lon1 - lon2;
		double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
		dist = Math.acos(dist);
		dist = rad2deg(dist);
		dist = dist * 60 * 1.1515;
		if (unit == "K") {
			dist = dist * 1.609344;
		} else if (unit == "N") {
			dist = dist * 0.8684;
		}

		return (dist);
	}
	
	private static double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}

	/*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
	/*::	This function converts radians to decimal degrees						 :*/
	/*:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::*/
	private static double rad2deg(double rad) {
		return (rad * 180 / Math.PI);
	}
	
	static void sendSessionMessage(String message, String destSessionId) {
		for (Session session : sessions) {
		    try {
		    	if(session.getId().compareTo(destSessionId) == 0) {
		    		session.getBasicRemote().sendText(message);
		    		Logger.getLogger(BlastWebSocketServer.class.getName()).log(Level.INFO, "String sent:" + message);
		    	}
		    } catch (IOException ex) {
		      Logger.getLogger(BlastWebSocketServer.class.getName()).log(Level.SEVERE, null, ex);
		    }
		  }
	}
	
}
