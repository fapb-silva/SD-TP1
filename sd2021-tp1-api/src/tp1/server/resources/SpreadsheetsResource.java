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

@Singleton
public class SpreadsheetsResource implements RestSpreadsheets{
	
	private final Map<String,Spreadsheet> sheets = new HashMap<String, Spreadsheet>();
	
	private static Logger Log = Logger.getLogger(SpreadsheetsResource.class.getName());

	@Override
	public String createSpreadsheet(Spreadsheet sheet, String password) {
		// TODO Henrique
		return null;
	}

	@Override
	public void deleteSpreadsheet(String sheetId, String password) {
		// TODO Henrique
		
	}

	@Override
	public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) {
		// TODO Henrique
		return null;
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
