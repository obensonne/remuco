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
package remuco;

import remuco.comm.ISerializable;
import remuco.comm.Serial;
import remuco.comm.SerialAtom;
import remuco.util.Log;

public final class ClientInfo implements ISerializable {

	private static final int[] ATOMS_FMT = new int[] { SerialAtom.TYPE_I,
			SerialAtom.TYPE_I, SerialAtom.TYPE_S };

	private static ClientInfo instance = null;

	public static ClientInfo getInstance() {
		if (instance == null) {
			instance = new ClientInfo();
		}
		return instance;
	}

	private final SerialAtom[] atoms;

	private ClientInfo() {

		atoms = SerialAtom.build(ATOMS_FMT);

		atoms[0].i = Config.SCREEN_WIDTH;
		atoms[1].i = Config.SCREEN_HEIGHT;
		atoms[2].s = Serial.ENCODING;

	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	public void notifyAtomsUpdated() {
		Log.bug("Feb 22, 2009.6:25:29 PM");
	}

}
