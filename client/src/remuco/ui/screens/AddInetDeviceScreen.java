package remuco.ui.screens;

import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import remuco.comm.InetServiceFinder;

public class AddInetDeviceScreen extends Form {

	private final TextField tfHost, tfPort;

	public AddInetDeviceScreen() {

		super("WiFi Connection");

		tfHost = new TextField("Host or IP address:", "", 256, TextField.NON_PREDICTIVE);

		tfPort = new TextField("Port number (if unsure, do not change):", "" + InetServiceFinder.PORT, 5, TextField.URL);

		//append(new CenteredImageItem(Theme.ICON_WIFI_20));
		append(" ");
		append(tfHost);
		append(" ");
		append(tfPort);
	}
	
	public String getAddress() {
		
		final String host = tfHost.getString();
		final String port = tfPort.getString();
		
		if (host.length() == 0) {
			return null;
		}
		
		if (port.length() == 0) {
			return host + ":" + InetServiceFinder.PORT;
		} else {
			return host + ":" + port;			
		}
		
	}

}
