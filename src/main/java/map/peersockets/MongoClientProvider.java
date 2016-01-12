package map.peersockets;

import java.net.UnknownHostException;
import java.util.Arrays;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
 
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
 
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.CONTAINER)
public class MongoClientProvider {
		
	private MongoClient mongoClient = null;
		
	@Lock(LockType.READ)
	public MongoClient getMongoClient(){	
		return mongoClient;
	}
	
	@PostConstruct
	public void init() {
		
		MongoCredential credential = MongoCredential.createCredential(System.getenv("OPENSHIFT_MONGODB_DB_USERNAME"),
				"peersockets",
				System.getenv("OPENSHIFT_MONGODB_DB_PASSWORD").toCharArray());
		
		mongoClient = new MongoClient(new ServerAddress(
				System.getenv("OPENSHIFT_MONGODB_DB_HOST"), Integer.valueOf(System.getenv("OPENSHIFT_MONGODB_DB_PORT"))),
				Arrays.asList(credential));//System.getenv("OPENSHIFT_MONGODB_DB_HOST"), Integer.valueOf(System.getenv("OPENSHIFT_MONGODB_DB_PORT")), credential);
	}
		
}