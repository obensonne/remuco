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

import remuco.client.common.serial.ISerializable;
import remuco.client.common.serial.SerialAtom;
import remuco.util.Tools;

public class Progress implements ISerializable {

	private static final int[] ATOMS_FMT = new int[] { SerialAtom.TYPE_I,
			SerialAtom.TYPE_I };

	private final SerialAtom[] atoms;

	public Progress() {
		atoms = SerialAtom.build(ATOMS_FMT);
	}

	public int getProgress() {
		return atoms[0].i;
	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	public int getLength() {
		return atoms[1].i;
	}

	public void notifyAtomsUpdated() {
	}

	public String getLengthFormatted() {
		return Tools.formatTime(atoms[1].i);
	}

	public String getProgressFormatted() {
		return Tools.formatTime(atoms[0].i);
	}

}
