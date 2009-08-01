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
import remuco.comm.SerialAtom;
import remuco.util.Log;

public final class ClientInfo implements ISerializable {

	public static final OptionDescriptor OD_IMG_SIZE = new OptionDescriptor(
			"img-size", "Image size", Math.min(200, Config.IMG_MAX_SIZE), 0,
			Config.IMG_MAX_SIZE);

	public static final OptionDescriptor OD_IMG_TYPE = new OptionDescriptor(
			"img-type", "Image type", "JPEG", "JPEG,PNG");

	public static final OptionDescriptor OD_PAGE_SIZE = new OptionDescriptor(
			"page-size", "Page size of lists", 50, 10, 10000);

	private static final int[] ATOMS_FMT = new int[] { SerialAtom.TYPE_I,
		SerialAtom.TYPE_S, SerialAtom.TYPE_I };

	private final SerialAtom[] atoms;

	public ClientInfo() {

		atoms = SerialAtom.build(ATOMS_FMT);

		final Config config = Config.getInstance();

		atoms[0].i = Integer.parseInt(config.getOption(OD_IMG_SIZE));
		atoms[1].s = config.getOption(OD_IMG_TYPE);
		atoms[2].i = Integer.parseInt(config.getOption(OD_PAGE_SIZE));

	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	public void notifyAtomsUpdated() {
		Log.bug("Feb 22, 2009.6:25:29 PM");
	}

}
