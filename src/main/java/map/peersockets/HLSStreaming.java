package map.peersockets;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


@Path("streaming")
public class HLSStreaming {

	@Produces({ "application/x-mpegURL" })
    @GET
    @Path("sample.m3u8")
    public String test() {
    	return "#EXTM3U\r\n" +
				"#EXT-X-PLAYLIST-TYPE:VOD\r\n" +
				"#EXT-X-TARGETDURATION:10\r\n" +
				"#EXT-X-VERSION:4\r\n" +
				"#EXT-X-MEDIA-SEQUENCE:0\r\n" +
				"#EXT-X-MAP:URI=moov.mp4" +
				"#EXTINF:10.0,\r\n" +
				"https://app.peerstreamit.com/HLS/streaming/test.mp4" +
//				"#EXTINF:10.0," +
//				"http://example.com/movie1/fileSequenceB.ts\r\n" +
//				"#EXTINF:10.0,\r\n" +
//				"http://example.com/movie1/fileSequenceC.ts\r\n" +
//				"#EXTINF:9.0,\r\n" +
//				"http://example.com/movie1/fileSequenceD.ts\r\n" +
				"#EXT-X-ENDLIST";
    }
	private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(100);
    @GET 
    @Path("test.mp4")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public void getFile(@Context HttpServletRequest requestContext,
            @Context HttpServletResponse response,
            @Suspended AsyncResponse asyncResponse) {
		scheduler.schedule(new Runnable() {
			@Override
			public void run() {
				asyncResponse.resume(Response.ok(new byte[0], MediaType.APPLICATION_OCTET_STREAM).build());
			}
			}, 5, TimeUnit.SECONDS);
		}
}
