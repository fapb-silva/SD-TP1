package tp1.server.resources;


import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import jakarta.inject.Singleton;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.rest.RestUsers;
import tp1.server.Discovery;

@Singleton
public class SpreadsheetsResource implements RestSpreadsheets{
	
	private final static int MAX_RETRIES = 3;
	private final static long RETRY_PERIOD = 1000;
	private final static int CONNECTION_TIMEOUT = 1000;
	private final static int REPLY_TIMEOUT = 600;
	
	private final Map<String,Spreadsheet> sheets = new HashMap<String, Spreadsheet>();
	private Discovery discovery;
	private String domain;
	private int ID;
	
	private static Logger Log = Logger.getLogger(SpreadsheetsResource.class.getName());

	public SpreadsheetsResource(Discovery discovery, String domain) {
		this.ID = 0;
		this.discovery = discovery;
		this.domain = domain;
	}

	@Override
	public String createSpreadsheet(Spreadsheet sheet, String password) {
		
		Log.info("createSpreadsheet : " + sheet);
		
		// 400 - sheet null
		if (sheet == null) {
			Log.info("Spreadsheet object null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		
		
		// 400 - password invalid valid
		if (!checkPassword(sheet.getOwner(), password)) {
			Log.info("Invalid password.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
			
		
		synchronized (this) {
			
//			// Check if userId does not exist exists, if not return HTTP CONFLICT (400)
//			if (sheets.containsKey(sheet.getSheetId())) {
//				Log.info("sheet already exists.");
//				throw new WebApplicationException(Status.BAD_REQUEST);
//			}

			// Add the sheet to the map of users
			String newSheetId = "" + ID++; 
			sheet.setSheetId(newSheetId);
			//sheet.setSheetURL();
			sheets.put(newSheetId, sheet);
		}		
		
		return sheet.getSheetId();
	}

	@Override
	public void deleteSpreadsheet(String sheetId, String password) {
		
		//400 - incorrect values
		if (sheetId == null || password == null) {
			Log.info("UserId or passwrod null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		
		Spreadsheet sheet;
		
		synchronized (this) {//searches for sheet
			
			sheet = sheets.get(sheetId);
			
		}
		
		// 404 - sheet doesnt exist
		if(sheet == null) {
			Log.info("Sheet does not exist.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		
		// 403 - wrong password
		if (checkPassword(sheet.getOwner(), password)) {
			Log.info("Spreadsheet object invalid.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		
		synchronized (this) {// 204 - removes sheet
			
			sheets.remove(sheetId);
			
		}
		
	}

	@Override
	public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) {
		
		//400 - incorrect values
		if (sheetId == null || password == null || userId == null) {
			Log.info("SheetId, UserId or passwrod null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		
		Spreadsheet sheet;
		
		synchronized (this) {//searches for sheet
			
			sheet = sheets.get(sheetId);
			
		}
		
		// 404 - sheet doesnt exist
		if(sheet == null) {
			Log.info("Sheet does not exist.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		
		// 403 - wrong password
		if (checkPassword(sheet.getOwner(), password)) {
			Log.info("Spreadsheet object invalid.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		
		synchronized (this) {
		
			return sheets.get(sheetId);//send 200?
		
		}
	}

	@Override
	public String[][] getSpreadsheetValues(String sheetId, String userId, String password) {
		
		//400 - incorrect values
		if (sheetId == null || password == null || userId == null) {
			Log.info("SheetId, UserId or passwrod null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		
		Spreadsheet sheet;
		
		synchronized (this) {//searches for sheet
			
			sheet = sheets.get(sheetId);
			
		}
		
		// 404 - sheet doesnt exist
		if(sheet == null) {
			Log.info("Sheet does not exist.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		
		// 403 - user not shared, user not owner, incorrect pass
		if (!sheet.getSharedWith().contains(userId) || sheet.getOwner() != userId || !checkPassword(userId, password)) {
			Log.info("Spreadsheet id object invalid.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		
		// 200 - success
		return sheet.getRawValues();
		
	}

	@Override
	public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
		//-----Checks
		
		Spreadsheet sheet = sheets.get(sheetId);
		
		// 200 - sucess
		sheet.setCellRawValue(cell, rawValue);
		
	}

	@Override
	public void shareSpreadsheet(String sheetId, String userId, String password) {
		// -----Checks
		if (sheetId == null || userId == null || password == null) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		synchronized (this) {

		}

		// 200 - sucess

	}

	@Override
	public void unshareSpreadsheet(String sheetId, String userId, String password) {
		// TODO Francisco
		
	}
	
	private boolean checkPassword(String userId, String password) {
		
		ClientConfig config = new ClientConfig();
		//how much time until we timeout when opening the TCP connection to the server
		config.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
		//how much time do we wait for the reply of the server after sending the request
		config.property(ClientProperties.READ_TIMEOUT, REPLY_TIMEOUT);
		Client client = ClientBuilder.newClient(config);
		
		URI serverUrl = discoverySearch( domain + ":Users");
		WebTarget target = client.target( serverUrl ).path( RestUsers.PATH );

		short retries = 0;

		while(retries < MAX_RETRIES) {
			
			try {
			Response r = target.path( userId).queryParam("password", password).request()
					.accept(MediaType.APPLICATION_JSON)
					.get();
			
			//User u = r.readEntity(User.class);
			return r.getStatus() == Status.OK.getStatusCode() && r.hasEntity();

			} catch (ProcessingException pe) {
				
				retries++;
				try { Thread.sleep( RETRY_PERIOD ); } catch (InterruptedException e) {
					//nothing to be done here, if this happens we will just retry sooner.
				}
				
			}
			
		}
		//
		return false;
	}

	private URI discoverySearch(String service) {
        URI[] uriList = discovery.knownUrisOf(service);
        if(uriList.length>0) 
        return uriList[0];
        
        return null;
    }
	
	private boolean userExists(String userId) {

        ClientConfig config = new ClientConfig();
        // how much time until we timeout when opening the TCP connection to the server
        config.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
        // how much time do we wait for the reply of the server after sending the
        // request
        config.property(ClientProperties.READ_TIMEOUT, REPLY_TIMEOUT);
        Client client = ClientBuilder.newClient(config);
        int delimiter = userId.indexOf('@');
        String user, userDomain;
        if(delimiter!=-1) {
            user = userId.substring(0,delimiter);
            userDomain = userId.substring(delimiter+1);
        }
        user = userId;
        userDomain = domain;
        URI serverUrl = discoverySearch(userDomain + ":Users");
        WebTarget target = client.target(serverUrl).path(RestUsers.PATH);

        short retries = 0;

        while (retries < MAX_RETRIES) {

            try {
                Response r = target.path("/").queryParam("query", user).request().accept(MediaType.APPLICATION_JSON)
                        .get();

                if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity()) {
                    List<User> users = r.readEntity(new GenericType<List<User>>() {
                    });
                    for(User thisUser : users) {
                        if(thisUser.getUserId() == user) return true;
                    }
                }

            } catch (ProcessingException pe) {

                retries++;
                try {
                    Thread.sleep(RETRY_PERIOD);
                } catch (InterruptedException e) {
                    // nothing to be done here, if this happens we will just retry sooner.
                }

            }

        }
        //
        return false;
    }
}
