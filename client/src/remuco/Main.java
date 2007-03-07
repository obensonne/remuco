package remuco;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import remuco.connection.BluetoothConnector;
import remuco.connection.GenericStreamConnection;
import remuco.connection.IConnector;
import remuco.connection.RemotePlayer;
import remuco.data.PlayerControl;
import remuco.ui.IScreen;
import remuco.util.FormLogPrinter;
import remuco.util.Log;
import remuco.util.Tools;

public class Main extends MIDlet implements CommandListener {

    private static final String APP_PROP_CONNTYPE = "remuco-connection";

    private static final String APP_PROP_UI = "remuco-ui";

    private static final Command CMD_BACK = new Command("Ok", Command.BACK, 10);

    private static final Command CMD_SHOW_LOG = new Command("Logs",
            Command.SCREEN, 99);

    private Display display;

    private boolean initialized = false;

    private Form logForm;

    private IScreen screenMain;

    protected RemotePlayer player;

    public void commandAction(Command c, Displayable d) {
        if (c == IScreen.CMD_DISPOSE) {
            cleanUp();
            notifyDestroyed();
        } else if (c == CMD_BACK) { // back from log form
            screenMain.setActive(true);
        } else if (c == CMD_SHOW_LOG) {
            screenMain.setActive(false);
            display.setCurrent(logForm);
        }
    }

    private void cleanUp() {
        if (player != null) {
            player.control(PlayerControl.PC_LOGOFF);
            Tools.sleep(200); // prevent connection shutdown before logoff has
                              // been send 
        }
        Log.ln(this, "bye bye!");
    }

    /**
     * Reads the application property {@link #APP_PROP_CONNTYPE}, creates an
     * according connector, uses the connector to create a connection and
     * returns the created connection.
     * 
     * @return connection to host system
     */
    private GenericStreamConnection getConnection() {
        String s;
        IConnector connector;
        GenericStreamConnection connection;

        // detect which connector to use
        s = getAppProperty(APP_PROP_CONNTYPE);
        if (s == null) {
            Log.ln(this, "No Connector specified in application descriptor !");
            return null;
        } else if (s.equals(IConnector.CONNTYPE_BLUETOOTH)) {
            connector = new BluetoothConnector();
        } else {
            Log.ln(this, "Connection type " + s + " unknown !");
            return null;
        }

        // create a connection
        if (!connector.init(display)) {
            Log.ln(this, "initializing connector failed");
            return null;
        } else {
            connection = connector.getConnection();
            synchronized (connection) {
                connector.createConnection();
                try {
                    connection.wait();
                    return connection;
                } catch (InterruptedException e) {
                    Log.ln(this, "I have been interrupted");
                    return null;
                }
            }
        }
    }

    /**
     * Reads the application property {@link #APP_PROP_UI} and returns the main
     * screen of the according UI.
     * 
     * @return main screen of the according UI
     */
    private IScreen getMainScreen() {
        String s;
        s = getAppProperty(APP_PROP_UI);
        if (s == null) {
            Log.ln(this, "No UI specified in apllication descriptor !");
            return null;
        } else if (s.equals(IScreen.UI_SIMPLE)) {
            return new remuco.ui.simple.MainScreen();
        } else {
            Log.ln(this, "UI type " + s + " unknown !");
            return null;
        }
    }

    private boolean init() {
        if (initialized) {
            return true;
        }

        // logging
        logForm = new Form("Logging");
        logForm.addCommand(CMD_BACK);
        logForm.setCommandListener(this);
        Log.setOut(new FormLogPrinter(logForm));

        display = Display.getDisplay(this);

        // create connection
        GenericStreamConnection connection = getConnection();
        if (connection == null || !connection.isOpen()) {
            // error handling
            Alert alert = new Alert("Error");
            alert.setString("No connection to Remuco server! Inspect the log " +
            		"for error analysis.");
            alert.setTimeout(Alert.FOREVER);
            display.setCurrent(alert);
            Tools.sleep(2000);
            
            // show the log, so the user can see what failed
            logForm.removeCommand(CMD_BACK);
            logForm.addCommand(IScreen.CMD_DISPOSE);
            logForm.setCommandListener(this);
            display.setCurrent(logForm);
            
            return false;
        }

        Log.ln(this, "connection to host established.");

        player = new RemotePlayer(connection);

        screenMain = getMainScreen();
        if (screenMain == null)
            return false;
        screenMain.setUp(this, display, player);
        screenMain.getDisplayable().addCommand(CMD_SHOW_LOG); // logging

        initialized = true;
        return true;
    }

    protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
        cleanUp();
    }

    protected void pauseApp() {
        screenMain.setActive(false);
    }

    protected void startApp() throws MIDletStateChangeException {
        if (init()) {
            screenMain.setActive(true);
        }
    }
}
