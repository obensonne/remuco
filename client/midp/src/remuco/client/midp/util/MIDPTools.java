/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2010 by the Remuco team, see AUTHORS.
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
package remuco.client.midp.util;

import java.util.Hashtable;

import javax.microedition.lcdui.Image;

import remuco.client.common.data.ClientInfo;
import remuco.client.common.util.Log;
import remuco.client.common.util.Tools;
import remuco.client.midp.Config;
import remuco.client.midp.ui.screens.ItemlistScreen;
import remuco.client.midp.ui.screens.PlayerScreen;

/**
 * Utility methods specific to the MIDP client.
 * 
 * @see Tools
 */
public class MIDPTools {

	/**
	 * Create an image from its byte array representation.
	 * 
	 * @param ay
	 *            the byte array
	 * @return an image or <code>null</code> if <em>ay</em> is <code>null</code>
	 *         or empty or if <em>ay</em> is malformed
	 */
	public static Image baToImage(byte ay[]) {

		if (ay != null && ay.length > 0) {
			try {
				return Image.createImage(ay, 0, ay.length);
			} catch (Exception e) {
				Log.ln("[IT] creating image failed", e);
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * Build a client info object.
	 * 
	 * @param config
	 *            config instance to access required values
	 * @param extra
	 *            whether to include extra information in the client info
	 * @return a client info object
	 */
	public static ClientInfo buildClientInfo(Config config, boolean extra) {

		final Hashtable info;

		if (extra) {
			info = new Hashtable();

			info.put("name", Config.DEVICE_NAME);
			info.put("touch", Config.TOUCHSCREEN ? "yes" : "no");
			info.put("utf8", Config.UTF8 ? "yes" : "no");

		} else {
			info = null;
		}

		final ClientInfo ci = new ClientInfo(
				Integer.parseInt(config.getOption(PlayerScreen.OD_IMG_SIZE)),
				config.getOption(PlayerScreen.OD_IMG_TYPE),
				Integer.parseInt(config.getOption(ItemlistScreen.OD_PAGE_SIZE)),
				info);

		return ci;
	}

	/**
	 * Build a one element service list containing the connection URL for a
	 * service where all required URL parameters are known.
	 * 
	 * @param proto
	 *            URL parameter
	 * @param addr
	 *            URL parameter
	 * @param port
	 *            URL parameter
	 * @param options
	 *            URL parameter (may be <code>null</code> or empty string)
	 * 
	 * @return a hash table mapping the generic service name <em>Player</em> to
	 *         an URL build from the given parameters
	 */
	public static Hashtable buildManualServiceList(String proto, String addr,
			String port, String options) {

		final StringBuffer url = new StringBuffer(proto);

		url.append("://");
		url.append(addr);
		url.append(':');
		url.append(port);

		if (options != null && options.length() > 0) {
			if (options.charAt(0) != ';') {
				url.append(';');
			}
			url.append(options);
		}

		final Hashtable services = new Hashtable(1);

		services.put("Player", url.toString());

		return services;
	}

}
