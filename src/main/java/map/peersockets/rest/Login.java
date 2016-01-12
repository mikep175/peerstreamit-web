package map.peersockets.rest;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RequestScoped
@Path("/login")
@Produces({ "application/xml", "application/json" })
@Consumes({ "application/xml", "application/json" })
public class Login {

	@POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public boolean login(LoginRequest req) {
        
		return true;
		
		
    }

}
