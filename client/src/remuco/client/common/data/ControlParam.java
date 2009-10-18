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
package remuco.client.common.data;

import remuco.client.common.serial.BinaryDataExecption;
import remuco.client.common.serial.ISerializable;
import remuco.client.common.serial.SerialAtom;
import remuco.util.Log;

/** Parameters of a control to send to the server. */
public class ControlParam implements ISerializable {

	private static final int[] ATOMS_FMT = new int[] { SerialAtom.TYPE_I };

	private final SerialAtom[] atoms;

	public ControlParam(int param) {
		atoms = SerialAtom.build(ATOMS_FMT);
		atoms[0].i = param;
	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	public void notifyAtomsUpdated() throws BinaryDataExecption {
		Log.bug("Mar 9, 2009.5:36:51 PM");
	}

}
