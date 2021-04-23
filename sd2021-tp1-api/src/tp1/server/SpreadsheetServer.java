package tp1.server;


import java.net.InetAddress;
import java.net.URI;
import java.util.logging.Logger;

import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import tp1.server.resources.SpreadsheetsResource;

public class SpreadsheetServer {

	private static Logger Log = Logger.getLogger(SpreadsheetServer.class.getName());

	static {
		System.setProperty("java.net.preferIPv4Stack", "true");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s\n");
	}
	
	public static final int PORT = 8080;
	public static final String SERVICE = "SpreadsheetServerService";
	
	
	public static void main(String[] args) {
		try {
		String ip = InetAddress.getLocalHost().getHostAddress();
			
		ResourceConfig config = new ResourceConfig();
		config.register(SpreadsheetsResource.class);

		String serverURI = String.format("http://%s:%s/rest", ip, PORT);
		JdkHttpServerFactory.createHttpServer( URI.create(serverURI), config);
	
		Log.info(String.format("%s Server ready @ %s\n",  SERVICE, serverURI));
		
		//More code can be executed here...
		
		
		Discovery discovery = new Discovery(Discovery.DISCOVERY_ADDR, SERVICE, "http://" + ip);
		discovery.start();
		
		} catch( Exception e) {
			Log.severe(e.getMessage());
		}
	}
	
}
