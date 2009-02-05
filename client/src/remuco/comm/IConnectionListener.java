package remuco.comm;

import remuco.UserException;
import remuco.player.PlayerInfo;

/**
 * Interface for classes interested in the state of a {@link Connection}.
 * 
 * @see Connection#Connection(String, IConnectionListener, IMessageListener)
 * 
 * @author Oben Sonne
 */
public interface IConnectionListener {

	/**
	 * Notifies a successful connection.
	 * @param conn the connected connection
	 * @param pinfo information about the connected player
	 */
	public void notifyConnected(Connection conn, PlayerInfo pinfo);

	/**
	 * Notifies a disconnection caused by the reason described in
	 * <i>reason</i>.
	 */
	public void notifyDisconnected(UserException reason);

}
