package remuco.player;

/**
 * For classes, which listen for {@link PlobList}s other than playlist or
 * queue.
 * 
 * @author Oben Sonne
 * 
 */
public interface IPloblistRequestor {

	/**
	 * Response to a previously requested ploblist. The ploblist is the same
	 * used as an argument when requesting the ploblist with
	 * {@link Player#reqPloblist(String, PlobList, IPloblistRequestor)} but with
	 * updated content.
	 * 
	 * @param p
	 *            the previously requested ploblist
	 */
	public void handlePloblist(PlobList ploblist);

}
