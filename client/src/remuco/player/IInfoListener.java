package remuco.player;

/**
 * Interface for all UI components whose representation depends on the player
 * info. They can register to be notified about changes in the player info by
 * calling {@link Info#addChangeListener(IInfoListener)}.
 * 
 * @author Christian Buennig
 * 
 */
public interface IInfoListener {

	/**
	 * Notify a change in the player info.<br>
	 * Intended to be used by user interface elements to update their
	 * representation due to (possibly) changed feature set.
	 * 
	 */
	public void infoChanged();

}
