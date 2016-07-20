package map.peersockets;

import java.io.File;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@ApplicationPath("/HLS")
@Produces(MediaType.TEXT_PLAIN)
public class HLS extends Application {
 
    @GET
    @Path("/test")
    public String test() {
    	return "test";
    }
    
//    @GET
//    @Produces(MediaType.APPLICATION_OCTET_STREAM)
//    public Response getFile() {
//      File file = new File();
//      return Response.ok(file, MediaType.APPLICATION_OCTET_STREAM)
//          .header("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"" ) //optional
//          .build();
//    }
}