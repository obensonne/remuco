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

import java.util.Vector;

import remuco.client.common.player.Feature;
import remuco.client.common.serial.BinaryDataExecption;
import remuco.client.common.serial.ISerializable;
import remuco.client.common.serial.SerialAtom;

/** A descriptive interface for the player. */
public class PlayerInfo implements ISerializable {

	private static final int[] ATOMS_FMT = new int[] { SerialAtom.TYPE_S,
			SerialAtom.TYPE_I, SerialAtom.TYPE_Y, SerialAtom.TYPE_AI,
			SerialAtom.TYPE_AS, SerialAtom.TYPE_AB, SerialAtom.TYPE_AS };

	private final SerialAtom[] atoms;

	private final Vector fileActions;

	private int flags;

	private int maxRating;

	private String name = "Remuco";

	private String searchMask[] = {};

	public PlayerInfo() {

		atoms = SerialAtom.build(ATOMS_FMT);

		fileActions = new Vector();

	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	public Vector getFileActions() {
		return fileActions;
	}

	public int getMaxRating() {
		return maxRating;
	}

	public String getName() {
		return name;
	}

	public String[] getSearchMask() {
		return searchMask;
	}

	public void notifyAtomsUpdated() throws BinaryDataExecption {

		name = atoms[0].s;
		flags = atoms[1].i;
		maxRating = atoms[2].y;

		fileActions.removeAllElements();
		int off = 3;
		for (int i = 0; i < atoms[off].ai.length; i++) {
			fileActions.addElement(new ItemAction(atoms[off].ai[i],
					atoms[off + 1].as[i], atoms[off + 2].ab[i]));
		}

		searchMask = atoms[6].as;
	}

	public boolean supports(int feature) {
		return (flags & feature) != 0;
	}

	public boolean supportsMediaBrowser() {

		boolean b = false;
		b |= (flags & Feature.REQ_PL) != 0;
		b |= (flags & Feature.REQ_QU) != 0;
		b |= (flags & Feature.REQ_MLIB) != 0;
		b |= fileActions.size() > 0;
		return b;
	}
}