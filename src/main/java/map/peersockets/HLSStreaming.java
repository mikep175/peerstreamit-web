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
				"#EXT-X-TARGETDURATION:10\r\n" +
				"#EXT-X-VERSION:4\r\n" +
				"#EXT-X-MEDIA-SEQUENCE:0\r\n" +
				"#EXT-X-MAP:URI=moov.mp4\r\n");
		
		 DecimalFormat decimalFormat=new DecimalFormat("#");
		 
		String sid = BinaryWebSocketServer.streamingSessions.get(hlsId);
		 
		for(String nsi : BinaryWebSocketServer.streamingRequests.keySet()) {
			
			if(BinaryWebSocketServer.streamingRequests.get(nsi).compareTo(sid) == 0) {
				
				Double length = BinaryWebSocketServer.hlsNSIs.get(nsi);
				
				int loc = 0;
				
				while (length > 10) {
					
					ret.append("#EXTINF:10.0,\r\n");
					ret.append("https://app.peerstreamit.com/HLS/streaming/chunk.mp4?sid=" + sid + "&loc=" + decimalFormat.format(loc));
					
					length -= 10;
					loc += 10;
				}

				if(length > 0) {
					ret.append("#EXTINF:"+decimalFormat.format(length)+".0,\r\n");
					ret.append("https://app.peerstreamit.com/HLS/streaming/chunk.mp4?sid=" + sid + "&loc=" + decimalFormat.format(loc));
				}
			}
			
			
		}
		
    	ret.append("#EXT-X-ENDLIST");
    	
    	return ret.toString();
    }
	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(100);
    @GET 
    @Path("chunk.mp4")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void getFile(@QueryParam("sid") String sid, @QueryParam("loc") String loc, @Context HttpServletRequest requestContext,
            @Context HttpServletResponse response,
            @Suspended AsyncResponse asyncResponse) {
    	

    	String nsinf = null;
    	
		for(String nsitemp : BinaryWebSocketServer.streamingRequests.keySet()) {
			
			if(BinaryWebSocketServer.streamingRequests.get(nsitemp) == sid) {
				
				nsinf = nsitemp;
				break;
				
			}
			
		}
		
		final String nsi = nsinf;
    	
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
  		  }
  	  }
    	
	}
    
    private void checkForBytes(String hlsId, AsyncResponse asyncResponse) {
    	
		scheduler.schedule(new Runnable() {
			@Override
			public void run() {
				
				if(BinaryWebSocketServer.hlsFrags.containsKey(hlsId) == true) {
					
					asyncResponse.resume(Response.ok(BinaryWebSocketServer.hlsFrags.get(hlsId), MediaType.APPLICATION_OCTET_STREAM).build());
					
				} else {
					checkForBytes(hlsId, asyncResponse);
				}
			}
			}, 1, TimeUnit.SECONDS);
    	
    }
}
