package remuco.player;

public interface IPloblistRequestor {

	/**
	 * Response to a previously requested ploblist.
	 * 
	 * @param ploblist
	 *            the requested ploblist
	 */
	public void handlePloblist(PlobList ploblist);

}
