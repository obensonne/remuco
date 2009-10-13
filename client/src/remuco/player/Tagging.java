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
package remuco.player;

import java.util.Vector;

import remuco.comm.BinaryDataExecption;
import remuco.comm.ISerializable;
import remuco.comm.SerialAtom;
import remuco.util.Log;
import remuco.util.Tools;

public class Tagging implements ISerializable {

	private static final int[] ATOMS_FMT = new int[] { SerialAtom.TYPE_S,
			SerialAtom.TYPE_AS };

	public static String[] trimAndFlatten(String tagString) {

		final String tags[] = Tools.splitString(tagString, ',', true);

		final Vector clean = new Vector(tags.length);

		for (int j = 0; j < tags.length; j++) {

			final String tag = tags[j];

			if (tag.length() == 0 || clean.contains(tag)) {
				continue;
			}

			clean.addElement(tag);
		}

		final String ret[] = new String[clean.size()];

		clean.copyInto(ret);

		return ret;
	}

	private final SerialAtom[] atoms;

	public Tagging(String id, String tags) {
		atoms = SerialAtom.build(ATOMS_FMT);
		atoms[0].s = id;
		atoms[1].as = trimAndFlatten(tags);
	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	public void notifyAtomsUpdated() throws BinaryDataExecption {
		Log.bug("Mar 9, 2009.6:32:37 PM");
	}

}
