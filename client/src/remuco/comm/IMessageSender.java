package remuco.comm;

public interface IMessageSender {

	/**
	 * Send a message to the server.
	 * 
	 * @param m
	 *            the message
	 */
	public void sendMessage(Message m);

}
