package remuco.player;

public interface IPlobRequestor {

	/**
	 * Response to a previously requested plob. The plob is the same used as an
	 * argument when requesting the plob with
	 * {@link Player#reqPlob(String, Plob, IPlobRequestor)} but with updated
	 * content.
	 * 
	 * @param p
	 *            the previously requested plob
	 */
	public void handlePlob(Plob p);

}
