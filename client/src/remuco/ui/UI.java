package remuco.ui;

import java.util.Hashtable;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import remuco.Config;
import remuco.UserException;
import remuco.comm.BinaryDataExecption;
import remuco.comm.BluetoothFactory;
import remuco.comm.Connection;
import remuco.comm.IConnectionListener;
import remuco.comm.IDeviceSelectionListener;
import remuco.comm.IMessageListener;
import remuco.comm.IServiceFinder;
import remuco.comm.IServiceListener;
import remuco.comm.InetServiceFinder;
import remuco.comm.Message;
import remuco.player.PlayerInfo;
import remuco.ui.screens.DeviceSelectorScreen;
import remuco.ui.screens.PlayerScreen;
import remuco.ui.screens.ServiceSelectorScreen;
import remuco.ui.screens.WaitingScreen;
import remuco.util.Log;

/**
 * Central screen manager.
 * 
 * @author Oben Sonne
 * 
 */
public final class UI implements CommandListener, IConnectionListener,
		IMessageListener, IServiceListener, IDeviceSelectionListener {

	/** An alert to signal an alerting message :) */
	private final Alert alert;

	private final Display display;

	private final CommandListener parent;

	/**
	 * Screen to show progress while connecting.
	 * <p>
	 * This object is also used as a mutex for synchronous connection state
	 * handling in
	 * <ul>
	 * <li>{@link #notifyConnected(Connection, PlayerInfo)},
	 * <li>{@link #notifyServices(Hashtable, UserException)} and
	 * <li>handling of {@link WaitingScreen#CMD_CANCEL} in
	 * {@link #commandAction(Command, Displayable)}.
	 * </ul>
	 * */
	private final WaitingScreen screenConnecting;
	// private final WaitingAlert screenConnecting;

	/** Screen to select a device to connect to */
	private final DeviceSelectorScreen screenDeviceSelector;

	/** Main player interaction screen */
	private PlayerScreen screenPlayer = null;

	/**
	 * A mutex to synchronize actions related to the player screen. The player
	 * screen itself ({@link #screenPlayer}) cannot be used because it may be
	 * <code>null</code>.
	 */
	private final Object screenPlayerLock = new Object();

	/** Screen to select a service (media player) */
	private final ServiceSelectorScreen screenServiceSelector;

	private final IServiceFinder serviceFinderBluetooth, serviceFinderWifi;

	public UI(CommandListener parent, Display display) {

		this.parent = parent;
		this.display = display;

		if (BluetoothFactory.BLUETOOTH) {
			serviceFinderBluetooth = BluetoothFactory
					.createBluetoothServiceFinder();
		} else {
			serviceFinderBluetooth = null;
		}

		serviceFinderWifi = new InetServiceFinder();

		alert = new Alert("");
		alert.setTimeout(Alert.FOREVER);
		alert.setType(AlertType.ERROR);

		screenServiceSelector = new ServiceSelectorScreen();
		screenServiceSelector.addCommand(CMD.BACK);
		screenServiceSelector.setCommandListener(this);

		screenConnecting = new WaitingScreen();
		// screenConnecting = new WaitingAlert();
		screenConnecting.setTitle("Connecting");
		screenConnecting.setImage(Theme.getInstance().ALERT_ICON_CONNECTING);
		screenConnecting.setCommandListener(this);

		screenDeviceSelector = new DeviceSelectorScreen(this, display, this);
		screenDeviceSelector.addCommand(CMD.LOG);
		screenDeviceSelector.addCommand(CMD.EXIT);

	}

	public void commandAction(Command c, Displayable d) {

		if (c == List.SELECT_COMMAND && d == screenServiceSelector) {

			connect(screenServiceSelector.getSelectedService());

		} else if (c == CMD.BACK && d == screenServiceSelector) {

			display.setCurrent(screenDeviceSelector);

		} else if (c == WaitingScreen.CMD_CANCEL && d == screenConnecting) {

			// user canceled connection setup

			final Object property;

			synchronized (screenConnecting) {
				property = screenConnecting.detachProperty();
			}

			if (property == null) {
				// already connected
				return;
			}

			if (property instanceof IServiceFinder) {
				// currently searching for services
				final IServiceFinder dev = (IServiceFinder) property;
				dev.cancelServiceSearch();
			} else if (property instanceof Connection) {
				// currently waiting for player description
				final Connection conn = (Connection) property;
				conn.down();
			}

			display.setCurrent(screenDeviceSelector);

		} else if (c == CMD.BACK && d == screenPlayer) {

			disposePlayerScreen();

			display.setCurrent(screenDeviceSelector);

		} else if (c == CMD.EXIT) {

			disposePlayerScreen();

			parent.commandAction(c, d);

		} else {

			parent.commandAction(c, d);

		}

	}

	public void notifyConnected(Connection conn, PlayerInfo pinfo) {

		Log.debug("got conn notification");
		
		synchronized (screenConnecting) {
			if (screenConnecting.detachProperty() == null) {
				// already canceled
				Log.debug("canceled");
				return;
			}
		}

		Log.debug("ok");
		
		synchronized (screenPlayerLock) {

			if (screenPlayer != null) {
				Log.bug("Jan 27, 2009.10:54:54 PM");
				screenPlayer.dispose();
				screenPlayer = null;
			}

			Log.debug("new player screen");

			try {
				screenPlayer = new PlayerScreen(display, conn, pinfo);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			screenPlayer.addCommand(CMD.BACK);
			screenPlayer.addCommand(CMD.LOG);
			screenPlayer.addCommand(CMD.EXIT);
			screenPlayer.setCommandListener(this);

			Log.ln("[UI] show player screen");
			
			display.setCurrent(screenPlayer);
		}

	}

	public void notifyDisconnected(UserException reason) {

		disposePlayerScreen();
		alert(reason, screenDeviceSelector);
	}

	public void notifyMessage(Connection conn, Message m) {

		// called by connection thread

		switch (m.id) {

		case Message.ID_IGNORE:

			break;

		case Message.ID_IFS_SRVDOWN:

			Log.ln("[UI] rxed server down");
			disposePlayerScreen();
			alert("Server Down", "The Remuco server said bye.", null,
					screenDeviceSelector);

			break;

		default: // forward all other to the player

			try {
				synchronized (screenPlayerLock) {
					if (screenPlayer != null) {
						screenPlayer.handleMessageForPlayer(m);
					}
				}
			} catch (BinaryDataExecption e) {
				Log.ln("[UI] rxed malformed data", e);
				disposePlayerScreen();
				alert("Connection Error", "Received malformed data.", e,
						screenDeviceSelector);
			} catch (OutOfMemoryError e) {
				Log.ln("[UI] rxed data too big, not enough memory");
				disposePlayerScreen();
				alert("Memory Error", "Recevied data too big.", null,
						screenDeviceSelector);
			}

			break;
		}

	}

	public void notifySelectedDevice(String type, String addr) {

		final IServiceFinder sf;

		if (type.equals(Config.DEVICE_TYPE_BLUETOOTH)) {

			if (serviceFinderBluetooth == null) {
				// this may happen in emulator, when switching on/off bluetooth
				// support between two runs
				Log.bug("Feb 3, 2009.12:54:53 AM");
				return;
			}
			sf = serviceFinderBluetooth;

		} else if (type.equals(Config.DEVICE_TYPE_INET)) {

			sf = serviceFinderWifi;

		} else {

			Log.bug("Jan 26, 2009.7:29:56 PM");
			return;
		}

		try {
			sf.findServices(addr, this);
		} catch (UserException e) {
			alert(e, screenDeviceSelector);
			return;
		}

		screenConnecting.attachProperty(sf);
		screenConnecting.setMessage("Searching for players.");
		display.setCurrent(screenConnecting);

	}

	public void notifyServices(Hashtable services, UserException ex) {

		synchronized (screenConnecting) {

			if (screenConnecting.detachProperty() == null) {
				return;
			}
		}

		if (ex != null) {
			alert(ex, screenDeviceSelector);
			return;
		}

		if (services.size() == 1) {

			final String url = (String) services.elements().nextElement();

			connect(url);

		} else {

			screenServiceSelector.setServices(services);

			display.setCurrent(screenServiceSelector);

		}

	}

	/**
	 * To call once when the app starts to hint the UI that it should start its
	 * work i.e. the user interaction.
	 * 
	 */
	public void showYourself() {

		screenDeviceSelector.showYourself();
	}

	/**
	 * Alert an error user to the user.
	 * 
	 * @param error
	 *            error title
	 * @param details
	 *            error details
	 * @param ex
	 *            optional exception whose message will be shown together with
	 *            <i>details</i>
	 * @param next
	 *            the displayable to show after the alert
	 */
	private void alert(String error, String details, Exception ex,
			Displayable next) {
		alert(new UserException(error, details, ex), next);
	}

	/**
	 * Alert an error user to the user.
	 * 
	 * @param ue
	 *            the user exception describing the error
	 * @param next
	 *            the displayable to show after the alert
	 */
	private void alert(UserException ue, Displayable next) {

		alert.setTitle(ue.getError());
		alert.setString(ue.getDetails());
		display.setCurrent(alert, next);
	}

	/**
	 * Connect to the given service and set up a waiting screen.
	 * 
	 * @param url
	 *            the service url
	 */
	private void connect(String url) {

		final Connection conn;

		try {
			conn = new Connection(url, this, this);
		} catch (UserException e) {
			alert(e, screenDeviceSelector);
			return;
		}

		screenConnecting.attachProperty(conn);
		screenConnecting.setMessage("Connecting to player.");
		display.setCurrent(screenConnecting);

	}

	/** Dispose the player screen and nullify its reference synchronized. */
	private void disposePlayerScreen() {

		synchronized (screenPlayerLock) {
			if (screenPlayer != null) {
				screenPlayer.dispose();
				screenPlayer = null;
			}
		}
	}

}
