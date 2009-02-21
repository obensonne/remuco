package remuco.ui.screens;

import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import remuco.comm.InetServiceFinder;

public class AddInetDeviceScreen extends Form {

	private final TextField tfHost, tfPort, tfOptions;

	public AddInetDeviceScreen() {

		super("WiFi Connection");

		tfHost = new TextField("Host or IP address:", "", 256,
				TextField.URL);

		tfPort = new TextField("Port number (if unsure, do not change):", ""
				+ InetServiceFinder.PORT, 5, TextField.NUMERIC);

		tfOptions = new TextField("Options (if unsure, leave empty):", "", 256,
				TextField.NON_PREDICTIVE);

		//append(" ");
		append(tfHost);
		//append(" ");
		append(tfPort);
		//append(" ");
		append(tfOptions);
	}

	public String getAddress() {

		final String host = tfHost.getString();
		final String port = tfPort.getString();
		final String options = tfOptions.getString();
		
		if (host.length() == 0) {
			return null;
		}

		final StringBuffer sb = new StringBuffer(host);

		sb.append(':');
		
		if (port.length() == 0) {
			sb.append(InetServiceFinder.PORT);
		} else {
			sb.append(port);
		}
		
		if (options.length() > 0) {
			if (!options.startsWith(";")) {
				sb.append(';');
			}
			sb.append(options);
		}

		return sb.toString();
	}

}
