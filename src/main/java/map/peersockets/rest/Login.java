package map.peersockets.rest;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

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
import map.peersockets.PasswordEncryptionService;

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
		
		MongoCollection<Document> collection = db.getCollection("users");

		// insert a document
		Document document = new Document("userId", "map");
		
		PasswordEncryptionService pes = new PasswordEncryptionService();
		
		try {
			
			byte[] salt = pes.generateSalt();
			
			document.put("password", pes.getEncryptedPassword("wordpass", salt));
			document.put("salt", salt);
			
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		collection.insertOne(document);
		
		return true;
		
		
    }

}
