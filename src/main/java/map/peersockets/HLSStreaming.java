package map.peersockets;

import java.io.File;
import java.text.DecimalFormat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@Path("streaming")
public class HLSStreaming {

	@Produces({ "application/x-mpegURL" })
    @GET
    @Path("playlist.m3u8")
    public String playlist(@QueryParam("sid") String hlsId) {

		Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "playlist.m3u8 : " + hlsId);
		  
		StringBuilder ret = new StringBuilder("#EXTM3U\r\n" +
				"#EXT-X-PLAYLIST-TYPE:VOD\r\n" +
				"#EXT-X-TARGETDURATION:3\r\n" +
				"#EXT-X-VERSION:3\r\n" +
				"#EXT-X-MEDIA-SEQUENCE:0\r\n" +
				"#EXT-X-MAP:URI=moov.mp4\r\n");
		
		 DecimalFormat decimalFormat=new DecimalFormat("#");
		 
		String sid = BinaryWebSocketServer.streamingSessions.get(hlsId);
		 
		for(String nsi : BinaryWebSocketServer.streamingRequests.keySet()) {
			
			if(BinaryWebSocketServer.streamingRequests.get(nsi).compareTo(sid) == 0) {
				
				Double length = BinaryWebSocketServer.hlsNSIs.get(nsi);
				
				int loc = 0;
				
				while (length > 3) {
					
					ret.append("\r\n#EXTINF:3.0,\r\n");
					ret.append("https://app.peerstreamit.com/HLS/streaming/chunk.mp4?sid=" + hlsId + "&loc=" + decimalFormat.format(loc));
					
					length -= 3;
					loc += 3;
				}

				if(length > 0) {
					ret.append("\r\n#EXTINF:"+decimalFormat.format(length)+".0,\r\n");
					ret.append("https://app.peerstreamit.com/HLS/streaming/chunk.mp4?sid=" + hlsId + "&loc=" + decimalFormat.format(loc));
				}
			}
			
			
		}
		
    	ret.append("#EXT-X-ENDLIST");
    	
    	return ret.toString();
    }
	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    @GET 
    @Path("chunk.mp4")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void getFile(@QueryParam("sid") String hlsId, @QueryParam("loc") String loc, @Context HttpServletRequest requestContext,
            @Context HttpServletResponse response,
            @Suspended AsyncResponse asyncResponse) {
    	

		String sid = BinaryWebSocketServer.streamingSessions.get(hlsId);
		
    	String nsinf = null;
    	
    	for(String nsi : BinaryWebSocketServer.streamingRequests.keySet()) {
			
			if(BinaryWebSocketServer.streamingRequests.get(nsi) != null && BinaryWebSocketServer.streamingRequests.get(nsi).compareTo(sid) == 0) {
				
				nsinf = nsi;
				break;
				
			}
			
		}
		
		final String nsi = nsinf;
		

		Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "chunk.mp4 : " + sid + " : " + nsi);
    	
    	if(BinaryWebSocketServer.streamingSessions.values().contains(sid) == true) {
  		  
  		  String destSessionId = null;
  		  
  		  for(String ss : BinaryWebSocketServer.streamingSessions.keySet()) {
  			  
  			  if(BinaryWebSocketServer.streamingSessions.get(ss).compareTo(sid) == 0) {
  				  
  				  destSessionId = ss;
  				  break;
  			  }
  		  }
  		  
  		  if(destSessionId != null) {
  		
  			BinaryWebSocketServer.sendSessionMessage("STREAMHLS:" + ":" + nsi + ":" + loc, destSessionId);
  			
  			checkForBytes(nsi + ":" + loc, asyncResponse);
  		  } else { 
  	  		asyncResponse.cancel();
  	  	  }
  	  } else {
  		asyncResponse.cancel();
  	  }
    	
	}
    
    private void checkForBytes(String hlsId, AsyncResponse asyncResponse) {
    	
		scheduler.schedule(new Runnable() {
			@Override
			public void run() {
				
				if(BinaryWebSocketServer.hlsFragsFinal.containsKey(hlsId) == true) {
					
					asyncResponse.resume(Response.ok(BinaryWebSocketServer.hlsFragsFinal.remove(hlsId), MediaType.APPLICATION_OCTET_STREAM).build());
					
				} else {
					checkForBytes(hlsId, asyncResponse);
				}
			}
			}, 1, TimeUnit.SECONDS);
    	
    }
}
