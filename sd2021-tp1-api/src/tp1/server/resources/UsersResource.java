package tp1.server.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;
import tp1.api.User;
import tp1.api.service.rest.RestUsers;

@Singleton
public class UsersResource implements RestUsers {

	private final Map<String, User> users = new HashMap<String, User>();

	private static Logger Log = Logger.getLogger(UsersResource.class.getName());

	public UsersResource() {
	}

	@Override
	public String createUser(User user) {
		Log.info("createUser : " + user);

		// Check if user is valid, if not return HTTP CONFLICT (409)
		if (user.getUserId() == null || user.getPassword() == null || user.getFullName() == null
				|| user.getEmail() == null) {
			Log.info("User object invalid.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		synchronized (this) {

			// Check if userId does not exist exists, if not return HTTP CONFLICT (409)
			if (users.containsKey(user.getUserId())) {
				Log.info("User already exists.");
				throw new WebApplicationException(Status.CONFLICT);
			}

			// Add the user to the map of users

			users.put(user.getUserId(), user);
		}
		return user.getUserId();

	}

	@Override
	public User getUser(String userId, String password) {
		Log.info("getUser : user = " + userId + "; pwd = " + password);

		// Check if user is valid, if not return HTTP CONFLICT (409)
		if (userId == null) {
			Log.info("UserId is null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		User user;

		synchronized (this) {
			user = users.get(userId);
		}

		// Check if user exists
		if (user == null) {
			Log.info("User does not exist.");
			throw new WebApplicationException(Status.NOT_FOUND);
		}

		// Check if the password is correct
		if (!user.getPassword().equals(password)) {
			Log.info("Password is incorrect.");
			throw new WebApplicationException(Status.FORBIDDEN);
		}

		return user;
	}

	@Override
	public User updateUser(String userId, String password, User user) {
		Log.info("updateUser : user = " + userId + "; pwd = " + password + " ; user = " + user);
		// TODO Complete method
		if (userId == null) {
			Log.info("UserId is null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		User userToUpdate;
		synchronized (this) {
			userToUpdate = users.get(userId);

			// Check if user exists
			if (userToUpdate == null) {
				Log.info("User does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}
			if (!userToUpdate.getPassword().equals(password)) {
				Log.info("Password is incorrect.");
				throw new WebApplicationException(Status.FORBIDDEN);
			}
			if (user != null) {
				String newEmail = user.getEmail();
				String newPassword = user.getPassword();
				String newName = user.getFullName();
				if (newEmail != null)
					userToUpdate.setEmail(newEmail);
				if (newPassword != null)
					userToUpdate.setPassword(newPassword);
				if (newName != null)
					userToUpdate.setFullName(newName);
				users.put(userId, userToUpdate);
			}
		}

		return userToUpdate;
	}

	@Override
	public User deleteUser(String userId, String password) {
		Log.info("deleteUser : user = " + userId + "; pwd = " + password);
		// TODO Complete method
		// Check if user is valid, if not return HTTP CONFLICT (409)
		if (userId == null) {
			Log.info("UserId null.");
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		User user;
		synchronized (this) {
			user = users.get(userId);

			// Check if user exists
			if (user == null) {
				Log.info("User does not exist.");
				throw new WebApplicationException(Status.NOT_FOUND);
			}

			// Check if the password is correct
			if (!user.getPassword().equals(password)) {
				Log.info("Password is incorrect.");
				throw new WebApplicationException(Status.FORBIDDEN);
			}
			users.remove(userId);
		}

		return user;
	}

	@Override
	public List<User> searchUsers(String pattern) {
		Log.info("searchUsers : pattern = " + pattern);
		if(pattern==null) {
			throw new WebApplicationException(Status.BAD_REQUEST);
		}
		// TODO Complete method
		List<User> matchingUsers = new ArrayList<User>();
		synchronized (this) {
			Collection<User> usersSet = users.values();
			for (User user : usersSet) {
				String fullName = user.getFullName().toLowerCase();
				if (fullName.contains(pattern.toLowerCase())) {
					User userToAdd = new User(user.getUserId(), user.getFullName(), user.getEmail(), "");
					matchingUsers.add(userToAdd);
				}
			}
		}
		return matchingUsers;
	}

}
