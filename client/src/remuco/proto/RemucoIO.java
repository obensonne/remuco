package remuco.proto;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import remuco.data.ClientInfo;
import remuco.data.PlayerControl;
import remuco.data.PlayerState;
import remuco.data.Song;
import remuco.util.ByteArray;
import remuco.util.Log;

public class RemucoIO implements Remuco {

    private static final String REM_CI_ENC_DEFAULT = "ASCII";

    private static final String LOGPRE = "[RIO]: ";

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

        if (version != REM_PROTO_VERSION) {
            throw new TransferDataException("versions differ");
        }
        if (dataType != REM_DATA_TYPE_PLAYER_STATE) {
            throw new TransferDataException("unexpected data type");
        }
        if (dataLen < REM_PS_TD_LEN) {
            throw new TransferDataException("not enough data");
        }

        data = new byte[dataLen];
        dis.readFully(data);
        setPlayerState(data, ps);

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
        Log.ln(LOGPRE + "read PS .. fix");
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
            Log.ln(LOGPRE + "warning - malformed song data");
        }

        return s;
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

    }
}
