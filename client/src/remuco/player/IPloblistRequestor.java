package remuco.player;

/**
 * For classes, which listen for {@link PlobList}s other than
 * {@link PlobList#PLID_PLAYLIST} or {@link PlobList#PLID_QUEUE}.
 * 
 * @author Christian Buennig
 * 
 */
public interface IPloblistRequestor {

	public void handlePloblist(PlobList ploblist);
	
}
