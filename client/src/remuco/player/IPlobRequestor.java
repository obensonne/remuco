package remuco.player;

public interface IPlobRequestor {

	/**
	 * Response to a previously requested plob.
	 * 
	 * @param plob
	 *            the requested plob
	 */
	public void handlePlob(Plob plob);

}
