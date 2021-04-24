package tp1.server.resources;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
import tp1.api.engine.AbstractSpreadsheet;
import tp1.api.service.rest.RestSpreadsheets;
import tp1.api.service.rest.RestUsers;
import tp1.impl.engine.SpreadsheetEngineImpl;
import tp1.server.Discovery;
import tp1.util.CellRange;

@Singleton
public class SpreadsheetsResource implements RestSpreadsheets {

	private final static int MAX_RETRIES = 3;
	private final static long RETRY_PERIOD = 1000;
	private final static int CONNECTION_TIMEOUT = 1000;
	private final static int REPLY_TIMEOUT = 600;
	public static final int PORT = 8080;

	private final Map<String, Spreadsheet> sheets = new HashMap<String, Spreadsheet>();
	private final Map<String, Set<String>> usersList = new HashMap<String, Set<String>>();
	private final Map<String, Set<String>> sharedList = new HashMap<String, Set<String>>();
	private Discovery discovery;
	private String domain;
	private String uri;
	private int ID;

	private static Logger Log = Logger.getLogger(SpreadsheetsResource.class.getName());

	public SpreadsheetsResource(Discovery discovery, String domain, String uri) {
		this.ID = 0;
		this.discovery = discovery;
		this.domain = domain;
		this.uri = uri;
	}

	@Override
	public String createSpreadsheet(Spreadsheet sheet, String password) {

		Log.info("createSpreadsheet : " + sheet);

		// 400 - sheet null
		if (sheet == null || sheet.getSheetId() != null || sheet.getSheetURL() != null || sheet.getRows() <= 0
				|| sheet.getColumns() <= 0 || !sheet.getSharedWith().isEmpty()
				|| sheet.getRows() != sheet.getRawValues().length
				|| sheet.getColumns() != sheet.getRawValues()[0].length) {
			Log.info("Spreadsheet object null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		// 400 - password invalid valid
		String owner = sheet.getOwner();
		if (userAuth(owner, password) != 1) {
			Log.info("Invalid password.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		String newSheetId = "" + ID++;
		synchronized (this) {
			if (!usersList.containsKey(owner))
				usersList.put(owner, new HashSet<String>());

			usersList.get(owner).add(newSheetId);
		}
		sheet.setSheetId(newSheetId);
		sheet.setSheetURL(String.format("%s/rest/sheets/%s", uri, newSheetId));
		// e.g - "http://srv1:8080/rest/sheets/4684354

		synchronized (this) {

			// Add the sheet to the map of users
			sheets.put(newSheetId, sheet);

		}

		return newSheetId;
	}

	@Override
	public void deleteSpreadsheet(String sheetId, String password) {

		// 400 - incorrect values
		if (sheetId == null) {
			Log.info("UserId or passwrod null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		Spreadsheet sheet;
		String owner;

		synchronized (this) {// searches for sheet

			sheet = sheets.get(sheetId);

			// 404 - sheet doesnt exist
			if (sheet == null) {
				Log.info("Sheet does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}

			// 403
			if (password == null) {
				Log.info("Pass null.");
				throw new WebApplicationException(Status.FORBIDDEN);
			}
			owner = sheet.getOwner();
			int auth = userAuth(owner, password);
			if (auth == 0) {// 403 - wrong password
				Log.info("Spreadsheet object invalid.");
				throw new WebApplicationException(Status.FORBIDDEN);
			} else if (auth == -1) { // 404 - userId doesnt exist
				removeUsersSpreadsheet(owner);
				throw new WebApplicationException(Status.NOT_FOUND);
			}

			// 204 - removes sheet

			sheets.remove(sheetId);
			if (usersList.containsKey(owner)) {
				if (usersList.get(owner).contains(sheetId))
					usersList.get(owner).remove(sheetId);
			}
			Set<String> shared = sheet.getSharedWith();
			for (String thisShare : shared) {
				if (sharedList.containsKey(thisShare)) {
					if (sharedList.get(thisShare).contains(sheetId))
						sharedList.get(thisShare).remove(sheetId);
				}

			}

		}

	}

	@Override
	public Spreadsheet getSpreadsheet(String sheetId, String userId, String password) {

		// 400 - incorrect values
		if (sheetId == null || userId == null) {
			Log.info("SheetId, UserId or passwrod null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		Spreadsheet sheet;
		int auth;
		String owner;

		synchronized (this) {// searches for sheet

			sheet = sheets.get(sheetId);

			// 404 - sheet doesnt exist
			if (sheet == null) {
				Log.info("Sheet does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}

			owner = sheet.getOwner();
			if (userAuth(owner, "") == -1) {
				removeUsersSpreadsheet(owner);
				throw new WebApplicationException(Status.NOT_FOUND);
			}

			auth = userAuth(userId, password);

			// 403 - wrong password
			if (!sheet.getOwner().equals(userId) && !sheet.getSharedWith().contains(userId + "@" + domain))
				throw new WebApplicationException(Status.FORBIDDEN);
		}
		if (auth == 0) {// 403 - wrong password

			Log.info("Spreadsheet object invalid.");
			throw new WebApplicationException(Status.FORBIDDEN);

		} else if (auth == -1) { // 404 - userId doesnt exist
			removeUsersSpreadsheet(userId);
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		return sheet;// send 200?
	}

	@Override
	public String[][] getSpreadsheetValues(String sheetId, String userId, String password) {

		// 400 - incorrect values
		if (sheetId == null || password == null || userId == null) {
			Log.info("SheetId, UserId or passwrod null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		Spreadsheet sheet;
		String[][] values;
		String owner;

		synchronized (this) {// searches for sheet

			sheet = sheets.get(sheetId);

		}

		int auth = userAuth(userId, password);

		// 404 - sheet doesnt exist
		if (sheet == null || auth == -1) {
			if (auth == -1)
				removeUsersSpreadsheet(userId);
			Log.info("Sheet does not exist.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}
		owner = sheet.getOwner();
		if (userAuth(owner, "") == -1) {
			removeUsersSpreadsheet(owner);
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		// 403 - user not shared, user not owner, incorrect pass
		if ((!sheet.getSharedWith().contains(userId + "@" + domain) && !sheet.getOwner().equals(userId)) || auth == 0) {
			Log.info("Spreadsheet id object invalid.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}

		// 200 - success

		try {
			values = SpreadsheetEngineImpl.getInstance().computeSpreadsheetValues(new AbstractSpreadsheet() {
				@Override
				public int rows() {
					return sheet.getRows();
				}

				@Override
				public int columns() {
					return sheet.getColumns();
				}

				@Override
				public String sheetId() {
					return sheet.getSheetId();
				}

				@Override
				public String cellRawValue(int row, int col) {
					try {
						return sheet.getRawValues()[row][col];
					} catch (IndexOutOfBoundsException e) {
						return "#ERROR?";
					}
				}

				@Override
				public String[][] getRangeValues(String sheetURL, String range) {
					ClientConfig config = new ClientConfig();
					// how much time until we timeout when opening the TCP connection to the server
					config.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
					// how much time do we wait for the reply of the server after sending the
					// request
					config.property(ClientProperties.READ_TIMEOUT, REPLY_TIMEOUT);
					Client client = ClientBuilder.newClient(config);
					WebTarget target = client.target(sheetURL);
					short retries = 0;

					while (retries < MAX_RETRIES) {

						try {
							Response r = target.queryParam("userId", userId).queryParam("password", password).request()
									.accept(MediaType.APPLICATION_JSON).get();
							if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity()) {
								Spreadsheet remoteSheet = r.readEntity(Spreadsheet.class);
								if (!remoteSheet.getSharedWith().contains(userId + "@" + domain))
									throw new WebApplicationException(Status.FORBIDDEN);
								CellRange valueRange = new CellRange(range);
								return valueRange.extractRangeValuesFrom(remoteSheet.getRawValues());

							}

							retries++;
						} catch (ProcessingException pe) {

							retries++;
							try {
								Thread.sleep(RETRY_PERIOD);
							} catch (InterruptedException e) {
								// nothing to be done here, if this happens we will just retry sooner.
							}

						}

					}
					throw new WebApplicationException(Status.BAD_REQUEST);
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return values;

	}

	public void updateCell(String sheetId, String cell, String rawValue, String userId, String password) {
		// -----Checks
		// If Ids are null
		int auth = userAuth(userId, password);
		if (sheetId == null || userId == null || cell == null || auth == -1) {
			if (auth == -1)
				removeUsersSpreadsheet(userId);
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		Spreadsheet sheet;
		String owner;
		synchronized (this) {
			// If sheets do not exist
			if (!sheets.containsKey(sheetId))
				throw new WebApplicationException(Status.NOT_FOUND);
			sheet = sheets.get(sheetId);
			owner = sheet.getOwner();
			if (userAuth(owner, "") == -1) {
				removeUsersSpreadsheet(owner);
				throw new WebApplicationException(Status.NOT_FOUND);
			}
			// If password does not match owners password
			if (auth == 0)
				throw new WebApplicationException(Status.FORBIDDEN);
			// If user has no permission
			if (!userId.equals(owner) && !sheet.getSharedWith().contains(userId + "@" + domain))
				throw new WebApplicationException(Status.FORBIDDEN);
			sheet.setCellRawValue(cell, rawValue);

		}

	}

	public void shareSpreadsheet(String sheetId, String userId, String password) {
		// -----Checks
		// If null ids or no @
		if (sheetId == null || userId == null || !userId.contains("@")) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		synchronized (this) {
			// If ids do not exist
			if (userAuth(userId, "") == -1 || !sheets.containsKey(sheetId)) {
				removeUsersSpreadsheet(userId);
				throw new WebApplicationException(Status.NOT_FOUND);
			}
			Spreadsheet thisSheet = sheets.get(sheetId);
			String ownerId = thisSheet.getOwner();
			if (userAuth(ownerId, "") == -1) {
				removeUsersSpreadsheet(ownerId);
				throw new WebApplicationException(Status.NOT_FOUND);
			}
			// If user to share is owner
			if (userId.equals(ownerId + "@" + domain))
				throw new WebApplicationException(Status.BAD_REQUEST);
			int auth = userAuth(ownerId, password);
			// If password is wrong
			if (auth == 0)
				throw new WebApplicationException(Status.FORBIDDEN);
			else if (auth == 1) {
				Set<String> shared = thisSheet.getSharedWith();
				// if already shared
				if (shared.contains(userId))
					throw new WebApplicationException(Status.CONFLICT);
				shared.add(userId);
				if (!sharedList.containsKey(userId))
					sharedList.put(userId, new HashSet<String>());
				sharedList.get(userId).add(sheetId);
			}
		}

	}

	public void unshareSpreadsheet(String sheetId, String userId, String password) {
		{ // -----Checks
			// If null ids or no @
			if (sheetId == null || userId == null || !userId.contains("@")) {
				throw new WebApplicationException(Status.BAD_REQUEST);
			}
			synchronized (this) {
				// If ids do not exist
				if (userAuth(userId, "") == -1 || !sheets.containsKey(sheetId)) {
					removeUsersSpreadsheet(userId);
					throw new WebApplicationException(Status.NOT_FOUND);
				}
				Spreadsheet thisSheet = sheets.get(sheetId);
				String ownerId = thisSheet.getOwner();
				if (userAuth(ownerId, "") == -1) {
					removeUsersSpreadsheet(ownerId);
					throw new WebApplicationException(Status.NOT_FOUND);
				}
				// If user to share is owner
				if (userId.equals(ownerId + "@" + domain))
					throw new WebApplicationException(Status.BAD_REQUEST);
				int auth = userAuth(ownerId, password);
				// If password is wrong
				if (auth == 0)
					throw new WebApplicationException(Status.FORBIDDEN);
				else if (auth == 1) {
					Set<String> shared = thisSheet.getSharedWith();
					// if already shared
					if (!shared.contains(userId))
						throw new WebApplicationException(Status.NOT_FOUND);
					shared.remove(userId);
					if (sharedList.containsKey(userId))
						if (sharedList.get(userId).contains(sheetId))
							sharedList.get(userId).remove(sheetId);

				}
			}

		}

	}

	private URI discoverySearch(String service) {
		URI[] uriList = discovery.knownUrisOf(service);
		if (uriList.length > 0)
			return uriList[0];

		return null;
	}

	private void removeUsersSpreadsheet(String userId) {

		if (usersList.containsKey(userId)) {
			Set<String> usersSheets = usersList.get(userId);
			
			for (String thisSheet : usersSheets) {
				if (sheets.containsKey(thisSheet))
					sheets.remove(thisSheet);
			}
			
			
			usersList.remove(userId);

		}
		if(sharedList.containsKey(userId)) {
			Set<String> sharedSheets = sharedList.get(userId);
			for (String thisSheet : sharedSheets) {
				if (sheets.containsKey(thisSheet))
					if (sheets.get(thisSheet).getSharedWith().contains(userId))
						sheets.get(thisSheet).getSharedWith().remove(userId);
			}
			sharedList.remove(userId);
		}
	}

	/*
	 * Return 1: User exists, correct password; 0: User exists, wrong password; -1:
	 * User does not exist, or other
	 */
	private int userAuth(String userId, String password) {

		ClientConfig config = new ClientConfig();
		// how much time until we timeout when opening the TCP connection to the server
		config.property(ClientProperties.CONNECT_TIMEOUT, CONNECTION_TIMEOUT);
		// how much time do we wait for the reply of the server after sending the
		// request
		config.property(ClientProperties.READ_TIMEOUT, REPLY_TIMEOUT);
		Client client = ClientBuilder.newClient(config);
		int delimiter = userId.indexOf('@');
		String user, userDomain;
		if (delimiter != -1) {
			user = userId.substring(0, delimiter);
			userDomain = userId.substring(delimiter + 1);
		} else {
			user = userId;
			userDomain = domain;
		}
		URI serverUrl = discoverySearch(userDomain + ":Users");
		WebTarget target = client.target(serverUrl).path(RestUsers.PATH);

		short retries = 0;

		while (retries < MAX_RETRIES) {

			try {
				Response r = target.path(user).queryParam("password", password).request()
						.accept(MediaType.APPLICATION_JSON).get();
				if (r.getStatus() == Status.NOT_FOUND.getStatusCode())
					return -1;
				// User u = r.readEntity(User.class);
				if (r.getStatus() == Status.FORBIDDEN.getStatusCode())
					return 0;
				if (r.getStatus() == Status.OK.getStatusCode() && r.hasEntity())
					return 1;
				retries++;
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
		return -1;
	}
}
