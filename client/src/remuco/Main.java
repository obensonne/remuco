package remuco;

import java.io.UnsupportedEncodingException;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
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
import remuco.ui.DummyScreen;
import remuco.ui.IScreen;
import remuco.util.FormLogPrinter;
import remuco.util.Log;
import remuco.util.Tools;

public class Main extends MIDlet implements CommandListener {

	private static final Command CMD_BACK = new Command("Ok", Command.BACK, 10);

	private static final Command CMD_SHOW_LOG = new Command("Logs",
			Command.SCREEN, 99);

	private static MIDlet midlet;

	/**
	 * This method is to offer access to application properties outside this
	 * class.
	 * 
	 * @param name
	 *            name of the application proerty
	 * @param def
	 *            default value
	 * @return the property's value as an int or the default if the property is
	 *         not set or if the property value is not a number
	 */
	public static int getAPropInt(String name, int def) {

		String s = getAPropString(name, null);

		if (s == null)
			return def;

		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			Log.ln("Property " + name + " is no int!");
			return def;
		}

	}

	/**
	 * This method is to offer access to application properties outside this
	 * class.
	 * 
	 * @param name
	 *            name of the application proerty
	 * @param def
	 *            default value
	 * @return the property's value or the default if not set
	 */
	public static String getAPropString(String name, String def) {

		String s;

		if (midlet == null) {
			return def;
		}

		s = midlet.getAppProperty(name);

		if (s == null) {
			Log.ln("Property " + name + " is not set!");
			return def;
		}

		return s;
	}

	protected RemotePlayer player;

	private GenericStreamConnection connection;

	private Display display;

	private boolean initialized = false;

	private Form logForm;

	private IScreen screenMain;

	protected Main() {
		super();
		midlet = this;
	}

	public void commandAction(Command c, Displayable d) {
		if (c == IScreen.CMD_DISPOSE) {
			cleanUp();
			notifyDestroyed();
		} else if (c == CMD_BACK) { // back from log form
			screenMain.setActive(true);
		} else if (c == Alert.DISMISS_COMMAND) { // back from error alert
			// show the log which exits the app if the user says 'exit'
			logForm.removeCommand(CMD_BACK);
			logForm.addCommand(IScreen.CMD_DISPOSE);
			logForm.setCommandListener(this);
			display.setCurrent(logForm);
		} else if (c == CMD_SHOW_LOG) {
			screenMain.setActive(false);
			display.setCurrent(logForm);
		}
	}

	protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
		cleanUp();
	}

	protected void pauseApp() {
		screenMain.setActive(false);
	}

	protected void startApp() throws MIDletStateChangeException {

		String s = init();
		if (s.length() == 0) {
			screenMain.setActive(true);
		} else {
			showAlert(s); // this results in exit
		}
		
		
	}

	private void tempEC() {
		// temp begin
		String b = "axá";
		Log.l("ETc: " + b + "(");
		for (int i = 0; i < b.length(); i++) {
			Log.l(Integer.toHexString(b.charAt(i)) + ".");
			
		}
		Log.ln(")");
		byte[] ba;
		
		ba = b.getBytes();
		Log.l("ETb: " + b + "(");
		for (int i = 0; i < ba.length; i++) {
			Log.l(Integer.toHexString((int)(ba[i])) + ".");
			
		}
		Log.ln(")");
		
		try {
			b = new String(new byte[] {0x61, 78, (byte)0xc3, (byte)0xa1, 0x0a}, "UTF8");
		} catch (UnsupportedEncodingException e) {
			Log.ln("failed: " + e.getMessage());
		}
		
		Log.ln("ECu: " + b);
		// temp end

	}
	
	private void cleanUp() {
		if (player != null) {
			player.control(PlayerControl.PC_LOGOFF);
			Tools.sleep(200); // prevent connection shutdown before logoff has
			// been send
			connection.close();
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
	private IConnector getConnector() {
		String s;
		IConnector connector;

		// detect which connector to use
		s = getAppProperty(IConnector.APP_PROP_CONNTYPE);
		if (s == null) {
			Log.ln(this, "No Connector specified in application descriptor !");
			return null;
		} else if (s.equals(IConnector.CONNTYPE_BLUETOOTH)) {
			connector = new BluetoothConnector();
		} else {
			Log.ln(this, "Connection type " + s + " unknown !");
			return null;
		}

		if (!connector.init(display)) {
			return null;
		} else {
			return connector;
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
		Class c;
		IScreen sc;
		s = getAppProperty(IScreen.APP_PROP_UI);

		try {
			c = Class.forName("remuco.ui." + s + ".MainScreen");
			sc = (IScreen) c.newInstance();
			return sc;
		} catch (ClassNotFoundException e) {
			Log.ln("MainScreen class not found: " + e.getMessage());
		} catch (InstantiationException e) {
			Log.ln("MainScreen class instantiation failed: " + e.getMessage());
		} catch (IllegalAccessException e) {
			Log.ln("MainScreen class instantiation access error: "
					+ e.getMessage());
		}
		return new DummyScreen();

	}

	private String init() {

		String s = "";
		IConnector connector;

		if (initialized) {
			return s;
		}

		// logging
		logForm = new Form("Logging");
		logForm.addCommand(CMD_BACK);
		logForm.setCommandListener(this);
		Log.setOut(new FormLogPrinter(logForm));

		tempEC();
		
		display = Display.getDisplay(this);

		String sa[] = Tools.getSupportedEncodings();
		Log.l("Supported encodings: ");
		for (int i = 0; i < sa.length; i++) {
			 Log.l(sa[i] + ", ");
		}
		Log.ln();
		
		// create connector
		connector = getConnector();
		if (connector == null) {
			return "Connecting failed. Please review the log messages to "
					+ "see what's wrong.";
		}

		// create a connection
		connection = connector.getConnection();
		synchronized (connection) {
			connector.createConnection();
			try {
				connection.wait();
			} catch (InterruptedException e) {
				Log.ln(this, "I have been interrupted");
				return "Connecting failed. Please review the log "
						+ "messages to see what's wrong.";
			}
		}
		if (connector.getReturnCode() == IConnector.RETURN_USER_CANCEL) {
			commandAction(IScreen.CMD_DISPOSE, null);
			return "x";
		}
		if (connector.getReturnCode() != IConnector.RETURN_OK) {
			return "Connecting failed!\n" + connector.getUserMsg();
		}
		if (!connection.isOpen()) {
			return "Connecting failed. Please review the log messages to "
					+ "see what's wrong.";
		}

		Log.ln("Cconnection to host established.");

		player = new RemotePlayer(connection);

		screenMain = getMainScreen();
		screenMain.setUp(this, display, player);
		screenMain.getDisplayable().addCommand(CMD_SHOW_LOG); // logging

		initialized = true;
		return s;
	}

	private void showAlert(String msg) {
		Alert alert = new Alert("Error");
		alert.setType(AlertType.ERROR);
		alert.setString(msg);
		alert.setTimeout(Alert.FOREVER);
		alert.setCommandListener(this);
		display.setCurrent(alert);
	}

}
