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
	public static final String SERVICE = "sheets";
	
	
	public static void main(String[] args) {
		try {
		String domain = args[0];
		//String ip = InetAddress.getLocalHost().getHostAddress();
		String ip = SERVICE+"."+domain;
		String serverURI = String.format("http://%s:%s/rest", ip, PORT);	
		
		Discovery discovery = new Discovery(Discovery.DISCOVERY_ADDR, domain +":"+ SERVICE, serverURI);
		
		ResourceConfig config = new ResourceConfig();
		SpreadsheetsResource sheetResource = new SpreadsheetsResource(discovery, domain);
		config.register(sheetResource);

		JdkHttpServerFactory.createHttpServer( URI.create(serverURI), config);
	
		Log.info(String.format("%s Server ready @ %s\n",  domain+":"+SERVICE, serverURI));
		
		//More code can be executed here...
		
		discovery.start();
		
		} catch( Exception e) {
			Log.severe(e.getMessage());
		}
	}
	
}
