package remuco;

import remuco.util.Log;

/**
 * An exception containing a error describing message suitable to display it to
 * the user.
 * 
 * @author Christian Buennig
 * 
 */
public final class UserException extends Exception {

	private String error, details;

	public UserException(String error, String details) {

		Log.asssert(this, error);
		Log.asssert(this, error.length());

		this.error = error;

		if (details == null)
			details = "";

		this.details = details;

		//Log.ln("[UE] " + error + " (" + details + ")");

	}

	public String getError() {
		return error;
	}

	public String getDetails() {
		return details;
	}

}
