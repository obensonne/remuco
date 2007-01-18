package remuco.connection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import remuco.data.ClientInfo;
import remuco.data.ObservablePlayerState;
import remuco.data.PlayerControl;
import remuco.data.PlayerState;
import remuco.proto.Remuco;
import remuco.proto.RemucoIO;
import remuco.proto.TransferDataException;
import remuco.util.Log;

/**
 * This is a player stub. It behaves like a player, but actually the player
 * state is recevied from and the player control commands are sent via a stream
 * to/from the remuco server which delegates data between the music player and
 * clients.
 * 
 * <p> - it accepts player controls and forwards them through a stream
 * <p> - it recevies player state changes from a stream and replays the 'player
 * state change' locally
 * <p>
 * 
 * @author Christian Buennig
 */
public class RemotePlayer implements Runnable {

    /**
     * Number of subsequent IO errors before closing the connection
     */
    private static final int MAX_IO_ERRORS = 10;

    private GenericStreamConnection con;

    private DataInputStream dis;

    private DataOutputStream dos;

    private int ioErrors = 0;

    private ObservablePlayerState ops;

    public RemotePlayer(GenericStreamConnection con) {
        this.con = con;
        dis = con.getIn();
        dos = con.getOut();

        ops = new ObservablePlayerState();

        Thread t = new Thread(this);
        t.start();

    }

    public void control(PlayerControl pc) {
        if (ops.getState() == Remuco.REM_PS_STATE_SRVOFF) {
            return;
        }
        try {
            RemucoIO.sendPlayerControl(dos, pc);
            ioErrors = 0;
        } catch (IOException e) {
            Log.ln(this, "sending PC[" + pc + "] failed: " + e.getMessage());
            if (ioErrors++ >= MAX_IO_ERRORS || !con.isOpen()) {
                Log.ln(this, "connection error");
                ops.setState(PlayerState.ST_ERROR);
                con.close();
            }
        }
    }

    public ObservablePlayerState getObservablePlayerState() {
        return ops;
    }

    public void run() {

        // meta infos about us (the client)
        ClientInfo ci = new ClientInfo();
        String s = System.getProperty("microedition.encoding");
        ci.setEncoding(s == null ? ClientInfo.DEF_ENC : s);

        // hello dialog
        try {
            Log.l(this, "sending client info .. ");
            RemucoIO.sendClientInfo(dos, ci);
            Log.l(this, "ok\n");
        } catch (IOException e) {
            Log.ln(this, "sending client info failed: " + e.getMessage());
            con.close();
            return;
        }

        // wait for player states
        while (ioErrors <= MAX_IO_ERRORS && con.isOpen()) {
            try {
                Log.ln(this, "waiting for new player state .. ");
                RemucoIO.recvPlayerState(dis, ops);
                Log.ln(this, "received new player state (" + ops + ")");
                ops.changed();
                ioErrors = 0;
                if (ops.getState() == Remuco.REM_PS_STATE_SRVOFF) {
                    Log.ln("server has shutdown");
                    con.close();
                    ops.changed();
                    return;
                }
            } catch (TransferDataException e) {
                Log.ln(this, "receiving player state failed:" + e.getMessage());
                ioErrors++;
            } catch (IOException e) {
                Log.ln(this, "receiving player state failed:" + e.getMessage());
                ioErrors++;
            }
        }

        Log.ln(this, "connection error");
        ops.setState(PlayerState.ST_ERROR);
        con.close();
    }

}
