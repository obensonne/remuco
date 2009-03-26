/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2009 Oben Sonne <obensonne@googlemail.com>
 *
 *   This file is part of Remuco.
 *
 *   Remuco is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Remuco is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Remuco.  If not, see <http://www.gnu.org/licenses/>.
 *   
 */
package remuco.ui;

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import remuco.Config;
import remuco.Remuco;
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
 * <p>
 * <em>Note:</em> All the <em>notify..</em> methods are expected to be called
 * only by the global timer thread.
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
	 * This waiting screen's property is used for synchronizing connection state
	 * handling between the UI event thread and the global timer thread.
	 * */
	private final WaitingScreen screenConnecting;

	/** Screen to select a device to connect to */
	private final DeviceSelectorScreen screenDeviceSelector;

	/** Main player interaction screen */
	private PlayerScreen screenPlayer = null;

	/** Screen to select a service (media player) */
	private final ServiceSelectorScreen screenServiceSelector;

	private final IServiceFinder serviceFinderBluetooth, serviceFinderWifi;

	public UI(CommandListener parent, Display display) {

		this.parent = parent;
		this.display = display;

		timer = Remuco.getGlobalTimer();

		if (BluetoothFactory.BLUETOOTH) {
			serviceFinderBluetooth = BluetoothFactory.createBluetoothServiceFinder();
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
		screenConnecting.setImage(Theme.getInstance().aicConnecting);
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

			property = screenConnecting.detachProperty();

			if (property == null) {
				return; // already connected
			}

			if (property instanceof IServiceFinder) {
				// currently searching for services
				((IServiceFinder) property).cancelServiceSearch();
			} else if (property instanceof Connection) {
				// currently waiting for player description
				((Connection) property).down();
			} else {
				Log.bug("Mar 17, 2009.9:40:43 PM");
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

		if (screenConnecting.detachProperty() == null) {
			// connection set up already canceled by user
			return;
		}

		if (screenPlayer != null) {
			Log.bug("Jan 27, 2009.10:54:54 PM");
			screenPlayer.dispose();
			screenPlayer = null;
		}

		screenPlayer = new PlayerScreen(display, conn, pinfo);
		screenPlayer.addCommand(CMD.BACK);
		screenPlayer.addCommand(CMD.LOG);
		screenPlayer.addCommand(CMD.EXIT);
		screenPlayer.setCommandListener(this);

		Log.ln("[UI] show player screen");

		display.setCurrent(screenPlayer);
	}

	public void notifyDisconnected(UserException reason) {

		disposePlayerScreen();
		alert(reason, screenDeviceSelector);
	}

	public void notifyMessage(Connection conn, Message m) {

		try {
			if (screenPlayer != null) {
				screenPlayer.handleMessageForPlayer(m);
			}
		} catch (BinaryDataExecption e) {
			Log.ln("[UI] rxed malformed data", e);
			disposePlayerScreen();
			alert("Connection Error", "Received malformed data.", e,
				screenDeviceSelector);
		} catch (OutOfMemoryError e) {
			m.data = null;
			m = null;
			Log.ln("[UI] rxed data too big, not enough memory");
			disposePlayerScreen();
			alert("Memory Error", "Recevied data too big.", null,
				screenDeviceSelector);
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

		if (screenConnecting.detachProperty() == null) {
			// connection set up already canceled by user
			return;
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
	public void show() {

		screenDeviceSelector.show();
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

	private final Timer timer;

	/**
	 * Dispose the player screen and nullify its reference synchronized.
	 */
	private void disposePlayerScreen() {

		// ensure disposing is done by the global timer thread:
		timer.schedule(new TimerTask() {
			public void run() {
				if (screenPlayer != null) {
					screenPlayer.dispose();
					screenPlayer = null;
				}
			}
		}, 200);
	}

}
