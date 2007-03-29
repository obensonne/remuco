package remuco.proto;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

import remuco.data.ClientInfo;
import remuco.data.PlayerControl;
import remuco.data.PlayerState;
import remuco.data.Song;
import remuco.util.ByteArray;
import remuco.util.Log;
import remuco.util.Tools;

public class RemucoIO implements Remuco {

	private static final String REM_CI_ENC_DEFAULT = "ASCII";

	private static final Runtime rt = Runtime.getRuntime();

	/**
	 * This is the bottom limit of free memory we 'require' after receiving a
	 * new player state. So if receiving an incoming player state would result
	 * in free memory below this limit, we call the garbe collector.
	 * 
	 * @see #recvPlayerState(DataInputStream, PlayerState)
	 */
	private static final long FREE_MEM_LIMIT = Math.min(rt.freeMemory() / 10,
			25000);


	/**
	 * 
	 * @param dis
	 * @param ps
	 * @throws TransferDataException
	 *             if the received player state data is malformed
	 * @throws IOException
	 *             on IO error
	 */
	public static void recvPlayerState(DataInputStream dis, PlayerState ps)
			throws TransferDataException, IOException {

		byte[] hdr = new byte[REM_TD_HDR_LEN];
		byte[] data;
		byte version;
		byte dataType;
		int dataLen;

		dis.readFully(hdr);

		version = hdr[0];
		dataType = hdr[1];
		dataLen = ByteArray.readInt(hdr, 2, ByteArray.BO_NET);
		Log.ln("[IO]: PS size " + dataLen);

		if (version != REM_PROTO_VERSION) {
			freeStream(dis);
			throw new TransferDataException("versions differ",
					TransferDataException.MAJOR);
		}
		if (dataType != REM_DATA_TYPE_PLAYER_STATE) {
			freeStream(dis);
			throw new TransferDataException("unexpected data type");
		}
		if (dataLen < REM_PS_TD_LEN) {
			freeStream(dis);
			throw new TransferDataException("not enough data");
		}

		// Check if there is enough free memory to recevie the ps data
		if (!checkMem(dataLen)) {
			freeStream(dis);
			throw new TransferDataException("not enough mem for ps data",
					TransferDataException.NOMEM);
		}

		data = new byte[dataLen];
		dis.readFully(data);
		setPlayerState(data, ps);

	}

	/**
	 * 
	 * @param dos
	 * @param ci
	 * @throws IOException
	 */
	public static void sendClientInfo(DataOutputStream dos, ClientInfo ci)
			throws IOException {

		byte[] encBa;
		byte[] encBaPlusPadding = new byte[REM_CI_ENCSTR_LEN];

		dos.writeByte(REM_PROTO_VERSION);
		dos.writeByte(REM_DATA_TYPE_CLIENT_INFO);
		dos.writeInt(REM_CI_TD_LEN);

		dos.writeShort(ci.getMaxPlaylistLen());

		if (ci.getEncoding().length() >= REM_CI_ENCSTR_LEN - 1) {
			ci.setEncoding(REM_CI_ENC_DEFAULT);
		}
		encBa = ci.getEncoding().getBytes();
		ByteArray.copy(encBa, 0, encBaPlusPadding, 0, encBa.length);
		ByteArray.set(encBaPlusPadding, encBa.length, (byte) 0,
				encBaPlusPadding.length - encBa.length);
		dos.write(encBaPlusPadding);

		dos.flush();

		// a bit logging:

		Log.ln("[IO] Free mem: " + rt.freeMemory() + ", free mem limit: "
				+ FREE_MEM_LIMIT);

	}

	/**
	 * 
	 * @param dos
	 * @param pc
	 * @throws IOException
	 */
	public static void sendPlayerControl(DataOutputStream dos, PlayerControl pc)
			throws IOException {

		dos.writeByte(REM_PROTO_VERSION);
		dos.writeByte(REM_DATA_TYPE_PLAYER_CTRL);
		dos.writeInt(REM_PC_TD_LEN);

		dos.writeShort(pc.getCmd());
		dos.writeShort(pc.getParam());

		dos.flush();

	}

	private static boolean checkMem(int dataLen) {

		long freeMem;

		if (dataLen < 1000) {
			// small data packet, don't care about mem
			return true;
		}

		freeMem = rt.freeMemory();

		if (dataLen > freeMem - FREE_MEM_LIMIT) {
			Log.ln("[MC]: free mem too small (" + freeMem + ") -> run gc");
			System.gc();
			freeMem = rt.freeMemory();
			if (dataLen > freeMem - FREE_MEM_LIMIT) {
				Log.ln("[MC]: free mem still too small (" + freeMem + ")");
				return false;
			} else {
				Log.ln("[MC]: gc was successful (" + freeMem + ")");
				return true;
			}
		} else {
			return true;
		}

	}

	private static Song createSong(byte[] data, int offset, int len) {

		int pos, lss;
		Song s = new Song();
		String tagName = null, tagValue = null;

		for (pos = offset, lss = pos; pos < offset + len; pos++) {
			if (data[pos] == 0) {
				if (tagName == null) {
					tagName = new String(ByteArray.sub(data, lss, pos - lss));
					lss = pos + 1;
				} else {
					tagValue = new String(ByteArray.sub(data, lss, pos - lss));
					lss = pos + 1;
					s.setTag(tagName, tagValue);
					tagName = null;
					tagValue = null;
				}
			}
		}
		if (tagName != null || tagValue != null) {
			Log.ln("[IO]: warning - malformed song data");
		}

		return s;
	}

	/**
	 * Skips all incomng data from stream <code>is</code>
	 * 
	 * @param is
	 *            the stream to skip incoming data from
	 * @throws IOException
	 */
	private static void freeStream(InputStream is) throws IOException {

		int bytesAvailable = 0;

		while ((bytesAvailable = is.available()) > 0) {
			is.skip(bytesAvailable);
			Log.ln("[IO]: Skipped " + bytesAvailable + " bytes");
			Tools.sleep(50); // wait a bit to ensure stream gets really freed
		}

	}

	/**
	 * Update a player state with the values from a byte array representation of
	 * another player state.
	 * 
	 * @param data
	 *            byte array player state
	 * @param ps
	 *            player state to update
	 * @throws TransferDataException
	 *             if the byte array data is malformed
	 */
	private static void setPlayerState(byte[] data, PlayerState ps)
			throws TransferDataException {

		int plLen, plSize, sLen, pos, i, n;
		short plPos;
		boolean plIncl;

		// fixed fields
		Log.ln("[IO]: read PS .. fix");
		ps.setState((byte) data[0]);
		ps.setVolume(data[1]);
		ps.playlistSetRepeat((data[2] & Remuco.REM_PS_FLAG_PL_REPEAT) != 0);
		ps.playlistSetShuffle((data[2] & Remuco.REM_PS_FLAG_PL_SHUFFLE) != 0);
		plIncl = (data[3] == 1);
		plPos = ByteArray.readShort(data, 4, ByteArray.BO_NET);
		plLen = ByteArray.readShort(data, 6, ByteArray.BO_NET);
		plSize = ByteArray.readInt(data, 8, ByteArray.BO_NET);

		if (data.length != (REM_PS_TD_LEN + (plIncl ? plSize : 0))) {
			throw new TransferDataException(
					"wrong size information in player state data");
		}

		// playlist
		pos = REM_PS_TD_LEN; // == 12 (first playlist data byte - if any)
		if (plIncl) {
			Log.l("create pl (" + plLen + " songs) .. ");
			ps.playlistClear();
			n = data.length;
			for (i = 0, pos = REM_PS_TD_LEN; i < plLen && pos < n; i++) {

				if (pos + 4 > n) {
					throw new TransferDataException(
							"playlist data malformed (incomplete song)");
				}
				sLen = ByteArray.readInt(data, pos, ByteArray.BO_NET);
				if (pos + 4 + sLen > n) {
					throw new TransferDataException(
							"playlist data malformed (incomplete song)");
				}
				ps.playlistAddSong(createSong(data, pos + 4, sLen));

				pos += (4 + sLen);
			}
			if (pos < n) {
				throw new TransferDataException("playlist data malformed ("
						+ (n - pos) + " unused bytes)");
			}
			if (i < plLen) {
				throw new TransferDataException(
						"playlist data malformed (missing " + (plLen - i)
								+ " songs)");
			}
		} else {
			Log.l("no pl incl .. ");
		}
		ps.playlistSetPosition(plPos);
		Log.ln("ok");
	}
}
