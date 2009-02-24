package remuco.comm;

import remuco.util.Log;

/**
 * A message received from or to send to the Remuco server.
 * 
 * @author Oben Sonne
 * 
 * @see Connection
 * @see Serial
 */
public final class Message {

	public static final int ID_IGNORE = 0;
	public static final int ID_PINFO = 2;
	public static final int ID_STATE = 3;
	public static final int ID_PLOB = 4;
	public static final int ID_BYE = 7;
	public static final int ID_CINFO = 8;
	public static final int ID_CTRL = 10;
	public static final int ID_REQ_PLOB = 11;
	public static final int ID_REQ_PLOBLIST = 12;

	/** Creates a new message. The message id is set to {@link #ID_IGNORE}. */
	public Message() {
	}

	/** Creates a new message with the given id and data. */
	public Message(int id, byte bytes[]) {
		this.id = id;
		this.bytes = bytes;
	}

	/**
	 * ID / type of the message. See constants <code>ID_...</code>
	 */
	public int id = ID_IGNORE;

	/**
	 * Message data in binary / serialized data format. This is the data as it
	 * will get transmitted to the server.
	 * 
	 * @see Serial
	 */
	public byte[] bytes = null;

	/**
	 * Dump the message's binary data into the log.
	 * 
	 * @see Log
	 * 
	 */
	protected void dumpBin() {

		Log.ln(bytes.length + " bytes message:");
		Log.ln(bytes);
	}

}
