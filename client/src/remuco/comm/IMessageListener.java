package remuco.comm;

/**
 * Interface for classes interested in received messages.
 * 
 * @see Connection#Connection(String, IConnectionListener, IMessageListener)
 * 
 * @author Oben Sonne
 */
public interface IMessageListener {

	public void notifyMessage(Connection conn, Message m);

}
