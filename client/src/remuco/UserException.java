package remuco;

/**
 * An exception containing a error describing message suitable to display it to
 * the user.
 * <p>
 * <em>Note:</em> The precompiled exceptions do not provide valid stack traces.
 * They only provide some textual information.
 * 
 * @author Oben Sonne
 * 
 */
public final class UserException extends Exception {

	private String error, details;

	public UserException(String error, String details) {

		this(error, details, null);
	}

	/**
	 * Create a new user exception.
	 * 
	 * @param error
	 *            the error title
	 * @param details
	 *            optional details of the error
	 * @param ex
	 *            an optional exception, whose message will be included in the
	 *            details
	 */
	public UserException(String error, String details, Exception ex) {

		this.error = error;

		if (details == null) {
			if (ex != null && ex.getMessage() != null) {
				this.details = ex.getMessage();
			} else {
				this.details = "";
			}
		} else {
			if (ex != null && ex.getMessage() != null) {
				if (details.endsWith(".")) {
					details = details.substring(0, details.length() - 1);
				}
				this.details = details + " (" + ex.getMessage() + ").";
			} else {
				this.details = details;
			}
		}

	}

	public String getDetails() {
		return details;
	}

	public String getError() {
		return error;
	}

}
