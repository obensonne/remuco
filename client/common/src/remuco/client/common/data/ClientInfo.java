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
package remuco.client.common.data;

import java.util.Enumeration;
import java.util.Hashtable;

import remuco.client.common.io.Connection;
import remuco.client.common.serial.ISerializable;
import remuco.client.common.serial.SerialAtom;
import remuco.client.common.util.Log;

public final class ClientInfo implements ISerializable {

	private static final int[] ATOMS_FMT = new int[] { SerialAtom.TYPE_I,
			SerialAtom.TYPE_S, SerialAtom.TYPE_I, SerialAtom.TYPE_AS,
			SerialAtom.TYPE_AS };

	/** Possible image type of images send from server to client. */
	public static final String IMG_TYPE_JPEG = "JPEG";
	
	/** Possible image type of images send from server to client. */
	public static final String IMG_TYPE_PNG = "PNG";

	private final SerialAtom[] atoms;

	/** Remuco version */
	public static final String VERSION = "0.9.4"; // VERSION_CHECK

	/**
	 * Create a new client info.
	 * <p>
	 * A client info is needed for initial setup of a {@link Connection}.
	 * Later, when the user decides to change one of the values given here as
	 * parameters, a new client info should be send to the server using
	 * {@link Connection#send(ClientInfo)}.
	 * 
	 * @param imgSize
	 *            preferred size of images (cover art) for this client device
	 * @param imgType
	 *            preferred type of images (cover art) for this client device
	 *            (one of {@link #IMG_TYPE_JPEG} or {@link #IMG_TYPE_PNG})
	 * @param ilPageSize
	 *            preferred maximum length of item list pages for this client
	 *            device (when browsing a player's media library, track lists
	 *            may be very long and exceed capabilities of client devices -
	 *            this value is used to split media library track lists into
	 *            multiple pages)
	 * @param extra
	 *            optional extra information as a string hash table (used by the
	 *            tool <em>remuco-report</em>) - this is only useful for an
	 *            initial client info and may be <code>null</code> when sending
	 *            an updated client info to the server
	 */
	public ClientInfo(int imgSize, String imgType, int ilPageSize,
			Hashtable extra) {

		atoms = SerialAtom.build(ATOMS_FMT);

		atoms[0].i = imgSize;
		atoms[1].s = imgType;
		atoms[2].i = ilPageSize;

		if (extra != null) {

			extra.put("version", VERSION);
			
			atoms[3].as = new String[extra.size()];
			atoms[4].as = new String[extra.size()];

			final Enumeration enu = extra.keys();
			int i = 0;
			while (enu.hasMoreElements()) {
				final String key = (String) enu.nextElement();
				final String val = (String) extra.get(key);
				atoms[3].as[i] = key;
				atoms[4].as[i] = val;
				i++;
			}
		}
	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	public void notifyAtomsUpdated() {
		Log.bug("Feb 22, 2009.6:25:29 PM");
	}


}
