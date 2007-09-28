package remuco;

import remuco.util.Log;

public final class UserException extends Exception {

	private String error, details;
	
	
	public UserException(String error, String details) {
		
		Log.asssert(error);
		Log.asssert(error.length());
		
		this.error = error;
		
		if (details == null) details = "";
			
		this.details = details;
		
		Log.ln("[UE] " + error + " (" + details + ")");
		
	}


	public String getError() {
		return error;
	}


	public String getDetails() {
		return details;
	}
	
	
	
	
}
