package remuco.comm;

import remuco.controller.ClientInfo;
import remuco.player.Info;
import remuco.player.Plob;
import remuco.player.PlobList;
import remuco.player.Library;
import remuco.player.SimpleControl;
import remuco.player.State;
import remuco.player.StringParam;
import remuco.util.Log;

/**
 * A message received from or to send to the Remuco server. The {@link Comm}
 * layer is responsible to convert between binary ({@link #bd}) and normal ({@link #sd})
 * format.
 * 
 * @author Christian Buennig
 * 
 * @see Comm
 * @see Serializer
 */
public final class Message {

	/**
	 * An array that maps message IDs to the data structure format vectors (see
	 * {@link IStructuredData#sdFormatVector}) that are suitable for the
	 * message's corresponding data. This mapping is for incoming messages.
	 * <p>
	 * These data structure format vectors (DSFV) are used by {@link Serializer}
	 * to convert message data into and from binary data.
	 */
	public static final int[][] DSFVAin = new int[][] { null, // 0, ID_IGNORE
			Info.sdFormatVector, // 1, ID_IFS_PINFO
			State.sdFormatVector, // 2, ID_IFS_STATE
			Plob.sdFormatVector, // 3, ID_IFS_CURPLOB
			PlobList.sdFormatVector, // 4, ID_IFS_PLAYLIST
			PlobList.sdFormatVector, // 5, ID_IFS_QUEUE
			null, // 6, ID_IFS_SRVDOWN
			null, // 7, only out, ID_IFC_CINFO
			null, // 8, only out, ID_CTL_SCTRL
			null, // 9, only out, ID_CTL_UPD_PLOB
			null, // 10, only out, ID_CTL_UPD_PLOBLIST
			null, // 11, only out, ID_CTL_PLAY_PLOBLIST
			Plob.sdFormatVector, // 12, ID_REQ_PLOB
			PlobList.sdFormatVector, // 13, ID_REQ_PLOBLIST
			PlobList.sdFormatVector, // 14, ID_REQ_SEARCH
			Library.sdFormatVector // 15, ID_REQ_LIBRARY
	};

	/**
	 * An array that maps message IDs to the data structure format vectors (see
	 * {@link IStructuredData#sdFormatVector}) that are suitable for the
	 * message's corresponding data. This mapping is for outgoing messages.
	 * <p>
	 * These data structure format vectors (DSFV) are used by {@link Serializer}
	 * to convert message data into and from binary data.
	 */
	public static final int[][] DSFVAout = new int[][] { null, // 0, ID_IGNORE
			null, // 1, only in, ID_IFS_PINFO
			null, // 2, only in, ID_IFS_STATE
			null, // 3, only in, ID_IFS_CURPLOB
			null, // 4, only in, ID_IFS_PLAYLIST
			null, // 5, only in, ID_IFS_QUEUE
			null, // 6, only in, ID_IFS_SRVDOWN
			ClientInfo.sdFormatVector, // 7, ID_IFC_CINFO
			SimpleControl.sdFormatVector, // 8, ID_CTL_SCTRL
			Plob.sdFormatVector, // 9, ID_CTL_UPD_PLOB
			PlobList.sdFormatVector, // 10, ID_CTL_UPD_PLOBLIST
			StringParam.sdFormatVector, // 11, ID_CTL_PLAY_PLOBLIST
			StringParam.sdFormatVector, // 12, ID_REQ_PLOB
			StringParam.sdFormatVector, // 13, ID_REQ_PLOBLIST
			Plob.sdFormatVector, // 14, ID_REQ_SEARCH
			null // 15, ID_REQ_LIBRARY
	};

	public static final int ID_IGNORE = 0;

	/**
	 * Info about the player
	 * <p>
	 * Param: {@link Info}
	 */
	public static final int ID_IFS_PINFO = 1;

	/**
	 * Current player state.
	 * <p>
	 * Param: {@link State}
	 */
	public static final int ID_IFS_STATE = 2;

	/**
	 * Currently played plob.
	 * <p>
	 * Param: {@link Plob}
	 */
	public static final int ID_IFS_CURPLOB = 3;

	/**
	 * Current playlist.
	 * <p>
	 * Param: {@link PlobList}
	 */
	public static final int ID_IFS_PLAYLIST = 4;

	/**
	 * Current queue.
	 * <p>
	 * Param: {@link PlobList}
	 */
	public static final int ID_IFS_QUEUE = 5;

	/**
	 * Server is down
	 * <p>
	 * Param: none
	 */
	public static final int ID_IFS_SRVDOWN = 6;

	/**
	 * Information about the client.
	 * <p>
	 * Param: {@link ClientInfo}
	 */
	public static final int ID_IFC_CINFO = 7;

	/**
	 * Simple control
	 * <p>
	 * Param: {@link SimpleControl}
	 */
	public static final int ID_CTL_SCTRL = 8;

	/**
	 * Update a plob (its meta tags)
	 * <p>
	 * Param: {@link Plob}
	 */
	public static final int ID_CTL_UPD_PLOB = 9;

	/**
	 * Update a ploblist
	 * <p>
	 * Param: {@link PlobList}
	 * <p>
	 * <b>FUTURE FEATURE !</b> (not yet implemented)
	 */
	public static final int ID_CTL_UPD_PLOBLIST = 10;

	/**
	 * Load a ploblist as the new playlist. The string param is the plid of the
	 * ploblist to load as playlist.
	 * <p>
	 * Param: {@link StringParam}
	 */
	public static final int ID_CTL_PLAY_PLOBLIST = 11;

	/**
	 * Request a plob.
	 * <p>
	 * Param: {@link StringParam} (containing one string - the plob's PID) <br>
	 * Return: {@link Plob}
	 */
	public static final int ID_REQ_PLOB = 12;

	/**
	 * Request a ploblist.
	 * <p>
	 * Param: {@link StringParam} (containing one string - the ploblist's PLID)
	 * <br>
	 * Return: {@link PlobList}
	 */
	public static final int ID_REQ_PLOBLIST = 13;

	/**
	 * Search plobs.
	 * <p>
	 * Param: {@link Plob} (search all plobs where the tags contain at least the
	 * tag values from this plob - so this is a kind of reference or plob mask)
	 * <br>
	 * Return: {@link PlobList}
	 * <p>
	 * <b>FUTURE FEATURE !</b> (not yet implemented)
	 */
	public static final int ID_REQ_SEARCH = 14;

	/**
	 * Request a list of all available ploblist (not including playlist and
	 * queue).
	 * <p>
	 * Param: none
	 * <p>
	 * Return: {@link Library}
	 */
	public static final int ID_REQ_LIBRARY = 15;

	/**
	 * This is a local message, i.e. originating somewhere in the client's
	 * communication layers. It signals that an error occured in the
	 * communication layer.
	 */
	public static final int ID_LOCAL_ERROR = 100;

	/**
	 * This is a local message, i.e. originating somewhere in the client's
	 * communication layers. It signals that the communication layer
	 * successfully connected to the server.
	 */
	public static final int ID_LOCAL_CONNECTED = 101;

	/**
	 * This is a local message, i.e. originating somewhere in the client's
	 * communication layers. It signals that the communication layer has lost
	 * the connection to the server.
	 */
	public static final int ID_LOCAL_DISCONNECTED = 102;

	/**
	 * ID / type of the message. See constants <code>ID_...</code>
	 */
	public int id = 0;

	/**
	 * Message data in structured data format. Every class that implements
	 * {@link IStructuredData} provides 2 methods which returns the objects
	 * attributes as structered data or which sets its attributes based on
	 * structered data.
	 * 
	 * @see Serializer
	 * 
	 */
	public Object[] sd = null;

	/**
	 * Message data in binary / serialized data format. This is the data as it
	 * will get transmitted to the server.
	 * 
	 * @see Serializer
	 */
	protected byte[] bd;

	/**
	 * Dump the message's binary data into the log.
	 * 
	 * @see Log
	 * 
	 */
	protected void dumpBin() {

		int i;

		Log.ln(bd.length + " bytes message:");

		for (i = 0; i < bd.length; i++) {
			if ((bd[i] & 0xFF) > 0x0F)
				Log.l(Integer.toHexString((bd[i] & 0xFF)) + " ");
			else
				Log.l("0" + Integer.toHexString((bd[i] & 0xFF)) + " ");
			if ((i + 1) % 16 == 0)
				Log.ln();
		}

		Log.ln();

	}

}
