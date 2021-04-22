package tp1.clients.users;


import java.io.IOException;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.glassfish.jersey.client.ClientConfig;

import tp1.api.User;
import tp1.api.service.rest.RestUsers;
import tp1.server.Discovery;

public class GetUserClient {

	public static void main(String[] args) throws IOException {
		
		if( args.length != 3) {
			System.err.println( "Use: java sd2021.aula2.clients.GetUserClient url userId password");
			return;
		}
		
		String serverUrl = args[0];
		String userId = args[1];
		String password = args[2];
		
		System.out.println("Sending request to server.");
		
		ClientConfig config = new ClientConfig();
		Client client = ClientBuilder.newClient(config);
		
		WebTarget target = client.target( serverUrl ).path( RestUsers.PATH );
		
		Response r = target.path( userId).queryParam("password", password).request()
				.accept(MediaType.APPLICATION_JSON)
				.get();

		if( r.getStatus() == Status.OK.getStatusCode() && r.hasEntity() ) {
			System.out.println("Success:");
			User u = r.readEntity(User.class);
			System.out.println( "User : " + u);
			
			//HENRIQUE-TODO
			
		} else
			System.out.println("Error, HTTP error status: " + r.getStatus() );
		
		//HENRIQUE
		Discovery discovery = new Discovery(Discovery.DISCOVERY_ADDR, "GetUserClient", "");//nao mandam links pq nao vao ser acedidos
        discovery.start();

	}
	
	
}
