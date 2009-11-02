/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2009 by the Remuco team, see AUTHORS.
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
package remuco.client.jme;

import java.util.Hashtable;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.StringItem;

import remuco.client.common.UserException;
import remuco.client.common.io.ISocket;
import remuco.client.common.player.Player;
import remuco.client.common.util.Log;
import remuco.client.jme.io.BluetoothFactory;
import remuco.client.jme.io.IDevice;
import remuco.client.jme.io.IServiceFinder;
import remuco.client.jme.io.IServiceListener;
import remuco.client.jme.io.InetServiceFinder;
import remuco.client.jme.io.Socket;
import remuco.client.jme.ui.CMD;
import remuco.client.jme.ui.Theme;
import remuco.client.jme.ui.screens.DeviceSelectorScreen;
import remuco.client.jme.ui.screens.DeviceSelectorScreen.IDeviceSelectionListener;
import remuco.client.jme.ui.screens.LogScreen;
import remuco.client.jme.ui.screens.PlayerScreen;
import remuco.client.jme.ui.screens.ServiceSelectorScreen;
import remuco.client.jme.ui.screens.WaitingScreen;
import remuco.comm.Connection;
import remuco.comm.Connection.IConnectionListener;

/**
 * MIDlet of the Remuco client.
 * <p>
 * <h1>Emulator Code</h1>
 * Some code is only used while running inside the WTK emulator. All
 * corresponding code is either tagged with <code>emulator</code> in its JavaDoc
 * or is located inside an if-statement block using the condition
 * {@link #EMULATION}.
 */
public class Remuco implements CommandListener, IConnectionListener,
		IServiceListener, IDeviceSelectionListener {

	private class ReconnectDialog extends Form implements CommandListener {

		private final String url;

		public ReconnectDialog(String url, String msg) {
			super("Disconnected");
			this.url = url;
			final ImageItem img = new ImageItem(null,
					Theme.getInstance().aicConnecting, Item.LAYOUT_CENTER
							| Item.LAYOUT_NEWLINE_AFTER, "");
			append(img);
			final StringItem text = new StringItem(null, msg);
			text.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_AFTER);
			append(text);
			append("\n");
			final StringItem question = new StringItem("Reconnect?", null);
			question.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_AFTER);
			append(question);
			addCommand(CMD.YES);
			addCommand(CMD.NO);
			setCommandListener(this);

		}

		public void commandAction(Command c, Displayable d) {

			if (c == CMD.YES) {
				connect(url);
			} else if (c == CMD.NO) {
				display.setCurrent(screenDeviceSelector);
			} else {
				Log.bug("Apr 9, 2009.9:53:36 PM");
			}
		}

	}

	/**
	 * @emulator If <code>true</code>, the client runs inside the WTK emulator.
	 */
	public static final boolean EMULATION;

	public static final String VERSION = "@VERSION@";

	static {
		EMULATION = "@EMULATION@".equalsIgnoreCase("true") ? true : false;
	}

	/** An alert to signal an alerting message :) */
	private final Alert alert;

	private final Alert alertLoadConfig;

	private final Alert alertSaveConfig;

	private final Config config;

	private final Display display;

	private Displayable displayableAfterLog;

	private final Entry midlet;

	/**
	 * Screen to show progress while connecting.
	 * <p>
	 * This waiting screen's property is used for synchronizing connection state
	 * handling between the UI event thread and the global timer thread.
	 * */
	private final WaitingScreen screenConnecting;

	/** Screen to select a device to connect to */
	private final DeviceSelectorScreen screenDeviceSelector;

	private final LogScreen screenLog;

	/** Main player interaction screen */
	private PlayerScreen screenPlayer = null;

	/** Screen to select a service (media player) */
	private final ServiceSelectorScreen screenServiceSelector;

	public Remuco(Entry midlet) {

		this.midlet = midlet;
		display = Display.getDisplay(midlet);

		// set up logging

		screenLog = new LogScreen(display);
		screenLog.addCommand(CMD.BACK);
		screenLog.setCommandListener(this);
		if (EMULATION) {
			Log.ln("RUNING IN EMULATION MODE ..");
			screenLog.append("Emulation -> logging goes to standard out!");
		} else {
			Log.setOut(screenLog);
		}

		// init configuration

		Config.init(midlet);

		config = Config.getInstance();

		// set up some displayables

		alertLoadConfig = new Alert("Error");
		alertLoadConfig.setString("Errors while loading configuration. "
				+ "Please inspect the log for details! Configuration erros "
				+ "are normal, if you installed a new version of the client.");
		alertLoadConfig.setType(AlertType.ERROR);
		alertLoadConfig.setTimeout(Alert.FOREVER);
		alertLoadConfig.setCommandListener(this);

		alertSaveConfig = new Alert("Error");
		alertSaveConfig.setString("Errors while saving configuration."
				+ " Please inspect the log for details!");
		alertSaveConfig.setType(AlertType.ERROR);
		alertSaveConfig.setTimeout(Alert.FOREVER);
		alertSaveConfig.setCommandListener(this);

		// set up the start screen

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

		final Displayable next;
		if (config.devices.isEmpty()) {
			next = screenDeviceSelector.getAddScreen();
		} else {
			next = screenDeviceSelector;
		}
		if (config.loadedSuccessfully) {
			display.setCurrent(next);
		} else {
			display.setCurrent(alertLoadConfig, next);
		}
	}

	public void commandAction(Command c, Displayable d) {

		if (c == CMD.LOG) {

			displayableAfterLog = d;

			display.setCurrent(screenLog);

		} else if (c == CMD.BACK && d == screenLog) {

			// display the displayable shown before the log

			if (displayableAfterLog != null) {
				display.setCurrent(displayableAfterLog);
				displayableAfterLog = null;
			} else {
				Log.bug("Aug 18, 2009.16:38:28 AM");
			}

		} else if (c == List.SELECT_COMMAND && d == screenServiceSelector) {

			connect(screenServiceSelector.getSelectedService());

		} else if (c == CMD.BACK && d == screenServiceSelector) {

			display.setCurrent(screenDeviceSelector);

		} else if (c == WaitingScreen.CMD_CANCEL && d == screenConnecting) {

			// user canceled connection setup

			final Object property = screenConnecting.detachProperty();

			if (property == null) {
				return; // already connected
			}

			if (property instanceof IServiceFinder) {
				// currently searching for services
				((IServiceFinder) property).cancelServiceSearch();
			} else if (property instanceof Connection) {
				// currently waiting for player description
				((Connection) property).close();
			} else {
				Log.bug("Mar 17, 2009.9:40:43 PM");
			}

			display.setCurrent(screenDeviceSelector);

		} else if (c == CMD.EXIT) {

			disconnect();

			if (config.save()) {
				midlet.notifyExit();
			} else {
				display.setCurrent(alertSaveConfig);
			}

		} else if (c == CMD.BACK && d == screenPlayer) {

			disconnect();

			final Alert confirm = new Alert("Disconnected",
					"Disconnected from remote player.",
					Theme.getInstance().aicConnecting, AlertType.CONFIRMATION);
			confirm.setTimeout(1500);
			display.setCurrent(confirm, screenDeviceSelector);

		} else if (c == Alert.DISMISS_COMMAND && d == alertLoadConfig) {

			// continue startup

			display.setCurrent(screenDeviceSelector);

		} else if (c == Alert.DISMISS_COMMAND && d == alertSaveConfig) {

			// continue shut down

			midlet.notifyExit();

		} else {

			Log.ln("[ROOT] unexpected command: " + c.getLabel());

		}

	}

	public void notifyConnected(Player player) {

		if (screenConnecting.detachProperty() == null) {
			// connection set up already canceled by user
			return;
		}

		screenPlayer = new PlayerScreen(display, player);
		screenPlayer.addCommand(CMD.BACK);
		screenPlayer.addCommand(CMD.LOG);
		screenPlayer.addCommand(CMD.EXIT);
		screenPlayer.setCommandListener(this);

		Log.ln("[UI] show player screen");

		display.setCurrent(screenPlayer);
	}

	public void notifyDisconnected(ISocket sock, UserException reason) {

		disconnect();
		
		if (sock != null) {
			final String url = ((Socket) sock).url;
			display.setCurrent(new ReconnectDialog(url, reason.getDetails()));
		} else {
			alert(reason, screenDeviceSelector);
		}
	}

	public void notifySelectedDevice(IDevice iDevice) {

		final IServiceFinder sf;

		if (iDevice.getType() == IDevice.TYPE_BLUETOOTH) {
			sf = BluetoothFactory.createBluetoothServiceFinder();
		} else if (iDevice.getType() == IDevice.TYPE_WIFI) {
			sf = new InetServiceFinder();
		} else {
			sf = null;
			Log.bug("Jan 26, 2009.7:29:56 PM");
		}

		if (sf == null) {
			return;
		}

		try {
			sf.findServices(iDevice, this);
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
	 * Called when the application managed requests to shutdown.
	 * <p>
	 * In this method important and delay-free clean up stuff has to be
	 * implemented.
	 */
	protected void destroy() {

		disconnect();
		config.save();

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

		final Socket sock;

		try {
			sock = new Socket(url);
		} catch (UserException e) {
			alert(e, screenDeviceSelector);
			return;
		}
		
		final int ping = Integer.parseInt(config.getOption(Config.OD_PING));
		final Connection conn = new Connection(sock, this, ping);

		screenConnecting.attachProperty(conn);
		screenConnecting.setMessage("Connecting to player.");
		display.setCurrent(screenConnecting);
	}

	/**
	 * Disconnects from the currently connected player (if there is one) and do
	 * related clean up.
	 */
	private void disconnect() {

		config.removeSessionOptionListener();

		if (screenPlayer != null) {
			screenPlayer.getPlayer().getConnection().close();
			screenPlayer = null;
		}
	}
}
