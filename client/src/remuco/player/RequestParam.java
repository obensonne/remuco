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
package remuco.player;

import remuco.comm.BinaryDataExecption;
import remuco.comm.ISerializable;
import remuco.comm.SerialAtom;
import remuco.util.Log;

/**
 * Parameters of a request to send to the server.
 * 
 * @author Oben Sonne
 * 
 */
public class RequestParam implements ISerializable {

	private static final int[] ATOMS_FMT = new int[] { SerialAtom.TYPE_S,
			SerialAtom.TYPE_AS };

	private final SerialAtom[] atoms;

	/** Request for an item. */
	public RequestParam(String id) {
		this();
		atoms[0].s = id;
	}

	/** Request for a file system or media lib level or search. */
	public RequestParam(String path[]) {
		this();
		atoms[1].as = path;
	}

	private RequestParam() {
		atoms = SerialAtom.build(ATOMS_FMT);
	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	public void notifyAtomsUpdated() throws BinaryDataExecption {
		Log.bug("Mar 9, 2009.6:34:50 PM");
	}

}
