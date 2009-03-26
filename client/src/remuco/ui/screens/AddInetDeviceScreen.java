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
