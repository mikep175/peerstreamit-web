package map.peersockets;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;


@Path("streaming")
public class HLSStreaming {

	@Produces(MediaType.TEXT_PLAIN)
    @GET
    @Path("test")
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
