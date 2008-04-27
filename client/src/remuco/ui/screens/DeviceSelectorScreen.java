package remuco.ui.screens;

import java.util.Vector;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextField;

import remuco.Config;
import remuco.UserException;
import remuco.comm.Scanner;
import remuco.comm.IScanResultListener;
import remuco.ui.UI;

public final class DeviceSelectorScreen extends List implements
		CommandListener, IScanResultListener {

	private static final Command CMD_ADD_DEVICE = new Command("Add Device",
			Command.SCREEN, 5);

	private static final Command CMD_DELETE_KNOWN_DEVICE = new Command(
			"Forget Device", Command.SCREEN, 30);

	private static final Command CMD_SCAN = new Command("Scan", Command.SCREEN,
			10);

	private static final Command CMD_SHOW_KNOWN_DEVICES = new Command(
			"Known devices", Command.SCREEN, 30);

	private static final Command CMD_CONNECT = new Command("Connect",
			Command.SCREEN, 1);

	private final Alert alertScanProblem, alertForgetDevice;

	/**
	 * Contains the devices currently selectable by the user. Element
	 * <code>2 * i</code> is the address device <code>i</code> and
	 * <code>2 * i + 1</code> is the device name.
	 */
	private String[] devices;

	private final Scanner df;

	private final Display display;

	private final CommandListener parent;

	private final WaitingScreen ws;

	private final Form formHost;

	private final Alert alertWelcome;

	private final Alert alertBadInput;

	private final TextField textfieldHost;

	public DeviceSelectorScreen(CommandListener parent, Display display) {
		super("Connector", IMPLICIT);

		this.parent = parent;
		this.display = display;

		df = new Scanner();

		alertScanProblem = new Alert("");
		alertScanProblem.setType(AlertType.ERROR);
		alertScanProblem.setTimeout(Alert.FOREVER);
		alertScanProblem.setCommandListener(this);

		alertForgetDevice = new Alert("Forget ...");
		alertForgetDevice.setString("Please confirm ..");
		alertForgetDevice.setType(AlertType.WARNING);
		alertForgetDevice.setTimeout(Alert.FOREVER);
		alertForgetDevice.addCommand(UI.CMD_NO);
		alertForgetDevice.addCommand(UI.CMD_YES);
		alertForgetDevice.setCommandListener(this);

		formHost = new Form("Add Device");
		textfieldHost = new TextField("Host", "192.168.0.26", 256,
				TextField.NON_PREDICTIVE);
		formHost.append(textfieldHost);
		formHost.append("Bluetooth:\n");
		formHost.append("Enter a Bluetooth hardware address in the format "
				+ "001122AABBFF.\n");
		formHost.append("WLAN:\n");
		formHost.append("Enter an IP address or a host name, optionally "
				+ "append a colon and a port number (e.g. '192.168.4.5:3452'"
				+ " or 'my.host.name:36542').");
		formHost.addCommand(UI.CMD_BACK);
		formHost.addCommand(UI.CMD_OK);
		formHost.setCommandListener(this);

		alertBadInput = new Alert("Error", "Need some input!", null,
				AlertType.ERROR);

		alertWelcome = new Alert("Remuco");
		alertWelcome.setType(AlertType.INFO);
		alertWelcome.setString("Currently there are no known server devices. "
				+ "On the next screen, do a scan for (Bluetooth) "
				+ "devices or add a (Bluetooth/WLAN) device manually.");
		alertWelcome.setTimeout(Alert.FOREVER);

		ws = new WaitingScreen();
		ws.setCommandListener(this);

		devices = new String[0];

		addCommand(CMD_SCAN);
		addCommand(CMD_ADD_DEVICE);

		setCommandListener(this);
	}

	public void commandAction(Command c, Displayable d) {

		int index;
		String s;

		if (c.equals(CMD_CONNECT)) { // DEVICE SELECTED //

			index = getSelectedIndex();

			// remember the device
			Config.knownDevicesAdd(devices[2 * index], devices[2 * index + 1]);

			// notify parent that user has selected a device
			parent.commandAction(SELECT_COMMAND, d);

		} else if (c == CMD_SCAN) { // SCAN //

			ws.setMessage("Scanning");

			try {
				df.startScan(this);
				display.setCurrent(ws);
			} catch (UserException e) {
				alertScanProblem.setTitle("Scan Error");
				alertScanProblem
						.setString(e.getError() + ": " + e.getDetails());
				alertScanProblem.setType(AlertType.ERROR);
				display.setCurrent(alertScanProblem);
			}

		} else if (c == WaitingScreen.CMD_CANCEL) { // cancel scan

			df.cancelScan();

			display.setCurrent(this);

		} else if (c == CMD_ADD_DEVICE) { // ADD IP HOST //

			display.setCurrent(formHost);

		} else if (c == UI.CMD_OK && d == formHost) { // o.k. //

			s = textfieldHost.getString();
			if (s.length() == 0) {
				display.setCurrent(alertBadInput, this);
				return;
			}

			Config.knownDevicesAdd(s, null);

			showKnownDevices();

			display.setCurrent(this);

		} else if (c == UI.CMD_BACK && d == formHost) { // cancel //

			display.setCurrent(this);

		} else if (c == CMD_SHOW_KNOWN_DEVICES) { // BACK TO KNOWN DEVS //

			showKnownDevices();

			display.setCurrent(this);

		} else if (c == CMD_DELETE_KNOWN_DEVICE) { // FORGET DEVICE //

			alertForgetDevice.setTitle("Forget "
					+ getString(getSelectedIndex()) + "?");

			display.setCurrent(alertForgetDevice);

		} else if (c == UI.CMD_NO) { // no, do not forget

			display.setCurrent(this);

		} else if (c == UI.CMD_YES) { // yes, forget

			index = getSelectedIndex();

			Config.knownDevicesDelete(devices[2 * index]);

			devices = new String[Config.knownDevicesGet().size()];

			if (devices.length > 0) {
				Config.knownDevicesGet().copyInto(devices);
			} else {
				removeCommand(CMD_DELETE_KNOWN_DEVICE);
				removeCommand(CMD_CONNECT);
			}

			delete(index);

			display.setCurrent(this);

		} else if (c == Alert.DISMISS_COMMAND) { // ALERT DISMISS //

			display.setCurrent(this);

		} else {

			parent.commandAction(c, d); // includes CMD_SHOW_LOG
		}
	}

	/**
	 * Get the device selected by the user. Makes sense to call after this
	 * screen has raised the command {@link List#SELECT_COMMAND}.
	 * 
	 * @return the device address of the selected device or <code>null</code>
	 *         if no device has been selected
	 */
	public String getSelectedDevice() {

		int i = getSelectedIndex();

		if (i < 0)
			return null;
		else
			return devices[2 * i];

	}

	public void scanFinished(String[] devices) {

		this.devices = devices;

		// the following commands only make sense if the device selection
		// list contains known devices, not - like now - found devices
		removeCommand(CMD_DELETE_KNOWN_DEVICE);
		addCommand(CMD_SHOW_KNOWN_DEVICES);

		updateList();

		if (devices.length == 0) {
			alertScanProblem.setTitle("No Devices");
			alertScanProblem.setString("Please check if your computer is in "
					+ "range, its Bluetooth is enabled and visible.");
			display.setCurrent(alertScanProblem);
		} else {
			display.setCurrent(this);
		}
	}

	/**
	 * When to show this screen the first time (i.e. when the app starts),
	 * instead of showing it by calling {@link Display#setCurrent(Displayable)}
	 * with this screen as argument, use this method to make it visible. Then
	 * the screen automatically scans for new devices if there are no known
	 * devices.
	 */
	public void showYourself() {

		showKnownDevices();

		if (devices.length == 0)
			display.setCurrent(alertWelcome, this);
		else
			display.setCurrent(this);

	}

	/**
	 * Updates the list of this screen to schow all known devices.
	 * 
	 */
	private void showKnownDevices() {

		Vector knownDevs;

		removeCommand(CMD_SHOW_KNOWN_DEVICES);

		knownDevs = Config.knownDevicesGet();

		devices = new String[knownDevs.size()];
		knownDevs.copyInto(devices);

		updateList();

		if (devices.length > 0) {
			addCommand(CMD_DELETE_KNOWN_DEVICE);
		} else {
			removeCommand(CMD_DELETE_KNOWN_DEVICE);
		}

	}

	/**
	 * Update visualization to display the devices listed in {@link #devices}.
	 */
	private void updateList() {

		String devName;

		deleteAll();

		for (int i = 0; i < devices.length; i += 2) {

			devName = devices[i + 1] != null ? devices[i + 1] : devices[i];

			append(devName, null);
		}

		if (devices.length > 0) {
			setSelectCommand(CMD_CONNECT);
			setSelectedIndex(0, true);
		} else {
			removeCommand(CMD_CONNECT);
		}

	}
}
