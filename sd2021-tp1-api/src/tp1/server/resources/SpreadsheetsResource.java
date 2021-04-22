package tp1.server.resources;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.Spreadsheet;
import tp1.api.User;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.clients.users.GetUserClient;

@Singleton
public class SpreadsheetsResource implements RestSpreadsheets{
	
	private final Map<String,Spreadsheet> sheets = new HashMap<String, Spreadsheet>();
	
	private static Logger Log = Logger.getLogger(SpreadsheetsResource.class.getName());

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
			//synchronized???passcheck??
			// Check if password valid, if not return HTTP BAD_REQUEST (400)
			if (GetUserClient == password) {
				Log.info("Spreadsheet object invalid.");
				throw new WebApplicationException(Status.BAD_REQUEST);
			}
			
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
		
		if (sheetId == null || password == null) {
			Log.info("UserId or passwrod null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		
		// Check if password valid, if not return HTTP BAD_REQUEST (400)
		if (GetUserClient.getPassword == password) {
			Log.info("Spreadsheet object invalid.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		
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
		
		synchronized (this) {
		
			//if sheetId is not stored throw
			if(!sheets.containsKey(sheetId) || GetUserClient == null) {
				Log.info("Sheet does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}
			
		}
		
		// Check if password valid, if not return HTTP BAD_REQUEST (400)
		if (GetUserClient.getPassword == password) {
			Log.info("Spreadsheet object invalid.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}
		
		
		
		return sheets.get(sheetId);//send200?
	}

	@Override
	public String[][] getSpreadsheetValues(String sheetId, String userId, String password) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
		// TODO Francisco
		
	}

	@Override
	public void shareSpreadsheet(String sheetId, String userId, String password) {
		// TODO Francisco
		
	}

	@Override
	public void unshareSpreadsheet(String sheetId, String userId, String password) {
		// TODO Francisco
		
	}

}
