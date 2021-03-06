package tp1.server;


import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import tp1.server.resources.UsersResource;

public class UsersServer {

	private static Logger Log = Logger.getLogger(UsersServer.class.getName());

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
	}
	
	public static final int PORT = 8080;
	public static final String SERVICE = "users";
	
	public static void main(String[] args) {
		try {
		String domain = args[0];
		String ip = InetAddress.getLocalHost().getHostAddress();
			
		ResourceConfig config = new ResourceConfig();
		config.register(UsersResource.class);

		String serverURI = String.format("http://%s:%s/rest", ip, PORT);
		JdkHttpServerFactory.createHttpServer( URI.create(serverURI), config);
	
		Log.info(String.format("%s Server ready @ %s\n",  domain+":"+SERVICE, serverURI));
		
		//More code can be executed here...
		
		
		Discovery discovery = new Discovery(Discovery.DISCOVERY_ADDR, domain+":"+SERVICE, serverURI);
		discovery.start();
		
		} catch( Exception e) {
			Log.severe(e.getMessage());
		}
	}
	
}
