package tp1.clients.users;

import java.io.IOException;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.glassfish.jersey.client.ClientConfig;

import tp1.api.User;
import tp1.api.service.rest.RestUsers;
import tp1.server.Discovery;

public class SearchUserClient {

	public static void main(String[] args) throws IOException {
		
		if( args.length != 2) {
			System.err.println( "Use: java sd2021.aula2.clients.SearchUserClient url query");
			return;
		}
		
		String serverUrl = args[0];
		String query = args[1];
		
		System.out.println("Sending request to server.");
		
		ClientConfig config = new ClientConfig();
		Client client = ClientBuilder.newClient(config);
		
		WebTarget target = client.target( serverUrl ).path( RestUsers.PATH );
		
		Response r = target.path("/").queryParam("query", query).request()
				.accept(MediaType.APPLICATION_JSON)
				.get();

		if( r.getStatus() == Status.OK.getStatusCode() && r.hasEntity() ) {
			List<User> users = r.readEntity(new GenericType<List<User>>() {});
			System.out.println("Success: (" + users.size() + " users)");
			users.stream().forEach( u -> System.out.println( u));
		} else
			System.out.println("Error, HTTP error status: " + r.getStatus() );

		//HENRIQUE
		Discovery discovery = new Discovery(Discovery.DISCOVERY_ADDR, "SearchUserClient", "");//nao mandam links pq nao vao ser acedidos
        discovery.start();
	}
	
}
