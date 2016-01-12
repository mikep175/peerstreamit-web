package map.peersockets.rest;

import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.bson.Document;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import map.peersockets.MongoClientProvider;

@RequestScoped
@Path("/login")
@Produces({ "application/xml", "application/json" })
@Consumes({ "application/xml", "application/json" })
public class Login {

	@EJB
	MongoClientProvider mongoClientProvider;
	
	@POST
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public boolean login(LoginRequest req) {
        
		MongoClient mClient = mongoClientProvider.getMongoClient();
		
		MongoDatabase db = mClient.getDatabase("peersockets");
		
		db.createCollection("users");
		
		MongoCollection<Document> collection = db.getCollection("users");

		// insert a document
		Document document = new Document("userId", 1);
		
		document.put("mp", "mp");
		
		collection.insertOne(document);
		
		return true;
		
		
    }

}
