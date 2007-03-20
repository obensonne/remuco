package remuco.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import remuco.connection.GenericStreamConnection;
import remuco.data.ClientInfo;
import remuco.data.IPlayerStateObserver;
import remuco.data.ObservablePlayerState;
import remuco.data.PlayerControl;
import remuco.proto.TransferDataException;
import remuco.util.Log;

/*
 * This class is a proxy for a remuco connected to a server via a stream
 * connection ({@link remuco.IStreamConnection}).<br>
 * Incoming communication (receiving commands) works in an extra thread which
 * automatically starts when constructing an object of this class. The extra
 * thread also does an initial hello dialog with the connected remuco. Outgoing
 * communication (sending updates) can be done by calling
 * {@link #update(PlayerUpdate)}.
 */

/**
 * This is a player skeleton. It is the counter part of the player stub.
 * 
 * <p> - it recognizes player state changes from a local player and forwards
 * them through a strem
 * <p> - it receives player controls from a stream and delegates them to a local
 * player
 * <p>
 * 
 * @author Christian Buennig
 * @see remuco.player.PlayerStub
 * 
 */
public class PlayerSkeleton extends Thread implements IPlayerStateObserver {

    private String clientSideEncoding = ClientInfo.DEF_ENC;

    private class HelloWatchDog extends Thread {

        public void run() {
            try {
                Thread.sleep(1000 * HELLO_DIALOG_TIMEOUT);
                Log.ln(this, "timeout while waiting for hello reply command.");
                close();
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    /**
     * Timeout for hello dialog.
     */
    private static final int HELLO_DIALOG_TIMEOUT = 10;

    /**
     * Number of subsequent IO errors before closing this stub
     */
    private static final int MAX_IO_ERRORS = 10;

    private GenericStreamConnection con;

    private DataInputStream dis;

    private DataOutputStream dos;

    private int ioErrors = 0;

    private ObservablePlayerState ops;

    private VirutalPlayer player;

    /**
     * Connects a remuco with a player.
     * 
     * @param con
     *            the connection to use for communication
     * @param player
     *            the player to work with
     */
    public PlayerSkeleton(GenericStreamConnection con, VirutalPlayer player) {
        this.con = con;
        dos = con.getOut();
        dis = con.getIn();
        this.player = player;
        ops = player.getObservablePlayerState();
        ops.addObserver(this);

        this.start();
    }

    private long psLastPlID;

    public void notifyPlayerStateChange() {
        try {
            synchronized (ops) {
                if (psLastPlID != ops.playlistGetID()) {
                    psLastPlID = ops.playlistGetID();
                    ops.includePlaylistInNextByteArray();
                }
                TransferData.write(dos, ops, clientSideEncoding);
            }
            ioErrors = 0;
        } catch (IOException e) {
            Log.ln(this, "IO error (write): " + e.getMessage());
            if (ioErrors++ >= MAX_IO_ERRORS || !con.isOpen()) {
                Log.ln(this, "connection error");
                close();
            }
        }
    }

    public void run() {

        HelloWatchDog helloWatchDog = new HelloWatchDog();
        PlayerControl pc = new PlayerControl();
        ClientInfo cmi = new ClientInfo();

        // check client (hello dialog)

        try {
            Log.ln(this, "sending hello message ..");
            TransferData.write(dos, NullData.INSTANCE, clientSideEncoding);
            Log.ln(this, "waiting for hello reply (client meta info) ..");
            helloWatchDog.start();
            TransferData.read(dis, cmi, clientSideEncoding);
            helloWatchDog.interrupt();
            clientSideEncoding = cmi.getEncoding();
            Log.ln(this, "ClientInfo: " + cmi.toString());
            Log.l(this, "sending player state ..");
            synchronized (ops) {
                psLastPlID = ops.playlistGetID();
                ops.includePlaylistInNextByteArray();
                TransferData.write(dos, ops, clientSideEncoding);
            }
            Log.ln("ok");
        } catch (TransferDataException e) {
            Log.ln(this, "hello dialog failed: " + e.getMessage());
            close();
            return;
        } catch (IOException e) {
            Log.ln(this, "hello dialog failed: " + e.getMessage());
            close();
            return;
        }

        // start work
        while (ioErrors <= MAX_IO_ERRORS && con.isOpen()) {
            try {
                TransferData.read(dis, pc, clientSideEncoding);
            } catch (TransferDataException e) {
                Log.ln(this, "error reading transfer data: " + e.getMessage());
                ioErrors++;
                continue;
            } catch (IOException e) {
                Log.ln(this, "IO error reading transfer data: "
                        + e.getMessage());
                ioErrors++;
                continue;
            }

            Log.ln(this, "got PC[" + pc + "] from remote controller");

            if (pc.getCmd() == PlayerControl.CODE_LOGOFF) {
                Log.ln(this, "client said bye bye. stopping.");
                break;
            }
            player.control(pc);
            ioErrors = 0;
        }
        Log.ln(this, "connection error");
        close();
    }

    private void close() {
        Log.ln(this, "closing.");
        ops.removeObserver(this);
        con.close();
    }

}
