package map.peersockets;

import java.io.File;
import java.nio.charset.Charset;
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

	@Produces({ "text/plain" })
    @GET
    @Path("master.m3u8")
    public Response master(@QueryParam("sid") String hlsId) {

		Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "master.m3u8 : " + hlsId);
		  
		StringBuilder ret = new StringBuilder("#EXTM3U\n" +
				"#EXT-X-VERSION:6\n" +
				"#EXT-X-INDEPENDENT-SEGMENTS\n" +
				"#EXT-X-MEDIA:TYPE=AUDIO,GROUP-ID=\"audio\",NAME=\"English stereo\",LANGUAGE=\"en\",AUTOSELECT=YES,URI=\"playlist.m3u8?sid=" + hlsId + "\"\n" +
				"#EXT-X-STREAM-INF:BANDWIDTH=628000,CODECS=\"avc1.4dc00d,mp4a.40.2\",RESOLUTION=320x180,AUDIO=\"audio\"\n" +
				"playlist.m3u8?sid=" + hlsId);

		byte[] retBytes = ret.toString().getBytes(Charset.forName("UTF-8"));
		
    	return Response.ok(retBytes, "text/plain").status(200).header("Content-Length", retBytes.length).build();
    }
	
	@Produces({ "text/plain" })
    @GET
    @Path("playlist.m3u8")
    public Response playlist(@QueryParam("sid") String hlsId) {

		Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "playlist.m3u8 : " + hlsId);
		  
		StringBuilder ret = new StringBuilder("#EXTM3U\n" +
				"#EXT-X-TARGETDURATION:4\n" +
				"#EXT-X-VERSION:7\n" +
				"#EXT-X-MEDIA-SEQUENCE:1\n" +
				"#EXT-X-PLAYLIST-TYPE:VOD\n" +
				"#EXT-X-INDEPENDENT-SEGMENTS\n" +
				"#EXT-X-MAP:URI=\"https://app.peerstreamit.com/HLS/streaming/init.mp4?sid=" + hlsId + "\"\n");
		
		 DecimalFormat decimalFormat=new DecimalFormat("#");
		 
		String sid = BinaryWebSocketServer.streamingSessions.get(hlsId);
		 
		for(String nsi : BinaryWebSocketServer.streamingRequests.keySet()) {
			
			if(BinaryWebSocketServer.streamingRequests.get(nsi).compareTo(sid) == 0) {
				
				Double length = BinaryWebSocketServer.hlsNSIs.get(nsi);
				
				int loc = 0;
				
				while (length > 4) {
					
					ret.append("\n#EXTINF:4.0\n");
					ret.append("https://app.peerstreamit.com/HLS/streaming/chunk.m4s?sid=" + hlsId + "&loc=" + decimalFormat.format(loc));
					
					length -= 4;
					loc += 4;
				}

				if(length > 0) {
					ret.append("\n#EXTINF:"+decimalFormat.format(length)+".0\n");
					ret.append("https://app.peerstreamit.com/HLS/streaming/chunk.m4s?sid=" + hlsId + "&loc=" + decimalFormat.format(loc));
				}
			}
			
			
		}
		
    	ret.append("#EXT-X-ENDLIST");

		byte[] retBytes = ret.toString().getBytes(Charset.forName("UTF-8"));
		
    	return Response.ok(retBytes, "text/plain").status(200).header("Content-Length", retBytes.length).build();
    }

    @GET 
    @Path("init.mp4")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
	 public void getInit(@QueryParam("sid") String hlsId, @Context HttpServletRequest requestContext,
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
			

			Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "init.mp4 : " + sid + " : " + nsi);
	    	
	    	if(BinaryWebSocketServer.streamingSessions.containsKey(sid) == true) {

	  			BinaryWebSocketServer.sendSessionMessage("INITHLS:" + nsi, BinaryWebSocketServer.streamingSessions.get(sid));
	  			
	  			checkForBytes(nsi + ":-1", asyncResponse);
	  			
	  	  } else {
	  		asyncResponse.cancel();
	  	  }
	    	
		}
	    
	
	
	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    @GET 
    @Path("chunk.m4s")
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
		

		Logger.getLogger(BinaryWebSocketServer.class.getName()).log(Level.INFO, "chunk.m4s : " + sid + " : " + nsi);
    	
    	if(BinaryWebSocketServer.streamingSessions.containsKey(sid) == true) {

  			BinaryWebSocketServer.sendSessionMessage("STREAMHLS:" + nsi + ":" + loc, BinaryWebSocketServer.streamingSessions.get(sid));
  			
  			checkForBytes(nsi + ":" + loc, asyncResponse);
  			
  	  } else {
  		asyncResponse.cancel();
  	  }
    	
	}
    
    private void checkForBytes(String hlsId, AsyncResponse asyncResponse) {
    	
		scheduler.schedule(new Runnable() {
			@Override
			public void run() {
				
				if(BinaryWebSocketServer.hlsFragsFinal.containsKey(hlsId) == true) {
					
					asyncResponse.resume(Response.ok(BinaryWebSocketServer.hlsFragsFinal.remove(hlsId), "text/plain").build());
					 
				} else {
					checkForBytes(hlsId, asyncResponse);
				}
			}
			}, 1, TimeUnit.SECONDS);
    	
    }
}
