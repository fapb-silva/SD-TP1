package tp1.server.resources;


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
	
	private static Logger Log = Logger.getLogger(SpreadsheetsResource.class.getName());

	public SpreadsheetsResource(Discovery discovery) {
		// TODO
		this.discovery = discovery;
	}

	@Override
	public String createSpreadsheet(Spreadsheet sheet, String password) {
		// TODO Henrique
		Log.info("createSpreadsheet : " + sheet);
		
		
		// Check if sheet is valid, if not return HTTP BAD_REQUEST (400)
		if (sheet.getSheetId() == null) {
			Log.info("Spreadsheet object invalid.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		
		
		//missing checks
		
		
		synchronized (this) {
			/*-----------------------------------------
			//synchronized???passcheck??
			// Check if password valid, if not return HTTP BAD_REQUEST (400)
			if (GetUserClient == password) {
				Log.info("Spreadsheet object invalid.");
				throw new WebApplicationException(Status.BAD_REQUEST);
			}
			*/
			// Check if userId does not exist exists, if not return HTTP CONFLICT (409)
			if (sheets.containsKey(sheet.getSheetId())) {
				Log.info("sheet already exists.");
				throw new WebApplicationException(Status.BAD_REQUEST);
			}

			// Add the sheet to the map of users

			sheets.put(sheet.getSheetId(), sheet);
		}		
		
		return sheet.getSheetId();
	}

	@Override
	public void deleteSpreadsheet(String sheetId, String password) {
		// TODO Henrique
		
		//400 - incorrect values
		if (sheetId == null || password == null) {
			Log.info("UserId or passwrod null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		/*--------------------------------
		// Check if password valid, if not return HTTP BAD_REQUEST (400)
		if (GetUserClient.getPassword == password) {
			Log.info("Spreadsheet object invalid.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		*/
		//if sheetId is not stored throw
		if(!sheets.containsKey(sheetId)) {
			Log.info("Sheet does not exist.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		
		sheets.remove(sheetId);
		
		throw new WebApplicationException(Status.NO_CONTENT);
		
	}

	@Override
	public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) {
		// TODO Henrique
		
		//400 - incorrect values
		if (sheetId == null || password == null || userId == null) {
			Log.info("SheetId, UserId or passwrod null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		
		// 404 - not_found
		synchronized (this) {
		
			//if sheetId is not stored throw
			if(!sheets.containsKey(sheetId) /*|| GetUserClient == null*/) {
				Log.info("Sheet does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}
			
		}
		/*
		// 403 - incorrect password
		if (GetUserClient.getPassword == password) {
			Log.info("Spreadsheet object invalid.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		*/
		
		
		return sheets.get(sheetId);//send200?
	}

	@Override
	public String[][] getSpreadsheetValues(String sheetId, String userId, String password) {
		// TODO 
		
		//400 - incorrect values
		if (sheetId == null || password == null || userId == null) {
			Log.info("SheetId, UserId or passwrod null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		
		// 404 - not_found
		Spreadsheet sheet;
		synchronized (this) {
			
			//if sheetId is not stored throw
			if(!sheets.containsKey(sheetId)/* || GetUserClient == null*/) {
				Log.info("Sheet does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}else 
				sheet = sheets.get(sheetId);  
			
		}
		
		// 403 - user not shared, user not owner, incorrect pass
		if (!sheet.getSharedWith().contains(userId) || sheet.getOwner() != userId /*|| GetUserClient.getPassword == password*/) {
			Log.info("Spreadsheet object invalid.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		
		// 200 - sucess
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
		//-----Checks 
		
		
		// 200 - sucess
	}

	@Override
	public void unshareSpreadsheet(String sheetId, String userId, String password) {
		// TODO Francisco
		
	}
	
	private boolean checkPassword(String userId, String password) {

		//tratamento do user
		
		
		
		ClientConfig config = new ClientConfig();
		//how much time until we timeout when opening the TCP connection to the server
		config.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
		//how much time do we wait for the reply of the server after sending the request
		config.property(ClientProperties.READ_TIMEOUT, REPLY_TIMEOUT);
		Client client = ClientBuilder.newClient(config);

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

	private String discoverySearch() {
		
	}
}
