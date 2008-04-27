package remuco.comm;

import remuco.util.Log;

/**
 * A message received from or to send to the Remuco server. The {@link Communicator}
 * layer is responsible to convert between binary ({@link #bytes}) and normal ({@link #serializable})
 * format.
 * 
 * @author Christian Buennig
 * 
 * @see Communicator
 * @see Serial
 */
public final class Message {

	public static final int ID_IGNORE = 0 ;
	public static final int ID_SYN_PLIST = 1;
	public static final int ID_IFS_PINFO = 2;
	public static final int ID_SYN_STATE = 3;
	public static final int ID_SYN_PLOB = 4;			/** CAP = currently active plob */
	public static final int ID_SYN_PLAYLIST = 5;
	public static final int ID_SYN_QUEUE = 6;
	public static final int ID_IFS_SRVDOWN = 7;
	public static final int ID_IFC_CINFO = 8;
	public static final int ID_SEL_PLAYER = 9;
	public static final int ID_CTL = 10;
	public static final int ID_REQ_PLOB = 11;
	public static final int ID_REQ_PLOBLIST = 12;
	
	/**
	 * ID / type of the message. See constants <code>ID_...</code>
	 */
	public int id = 0;

	/**
	 * Message data in binary / serialized data format. This is the data as it
	 * will get transmitted to the server.
	 * 
	 * @see Serial
	 */
	public byte[] bytes;

	/**
	 * Dump the message's binary data into the log.
	 * 
	 * @see Log
	 * 
	 */
	protected void dumpBin() {

		int i;

		Log.ln(bytes.length + " bytes message:");

		for (i = 0; i < bytes.length; i++) {
			if ((bytes[i] & 0xFF) > 0x0F)
				Log.l(Integer.toHexString((bytes[i] & 0xFF)) + " ");
			else
				Log.l("0" + Integer.toHexString((bytes[i] & 0xFF)) + " ");
			if ((i + 1) % 16 == 0)
				Log.ln();
		}

		Log.ln();

	}

}
