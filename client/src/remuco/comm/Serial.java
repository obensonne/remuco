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
package remuco.comm;

import remuco.util.Log;

/**
 * {@link Serial} (de)serializes {@link ISerializable}s.
 * 
 * @author Oben Sonne
 * 
 */
public final class Serial {

	public static final String ENCODING = "UTF-8";

	/**
	 * Updates a {@link ISerializable} with binary data.
	 * 
	 * @param m
	 *            the message
	 * @throws BinaryDataExecption
	 *             if binary data is somehow malformed (e.g. incompatible to the
	 *             atoms of the {@link ISerializable})
	 */
	public static void in(ISerializable s, byte[] data)
			throws BinaryDataExecption {

		final BaIn bis = new BaIn(data);

		final SerialAtom atoms[] = s.getAtoms();

		final int len = atoms.length;

		for (int i = 0; i < len; i++) {

			switch (atoms[i].type) {
			case SerialAtom.TYPE_Y:
				atoms[i].y = bis.readY();
				break;
			case SerialAtom.TYPE_B:
				atoms[i].b = bis.readB();
				break;
			case SerialAtom.TYPE_N:
				atoms[i].n = bis.readN();
				break;
			case SerialAtom.TYPE_I:
				atoms[i].i = bis.readI();
				break;
			case SerialAtom.TYPE_X:
				atoms[i].x = bis.readX();
				break;
			case SerialAtom.TYPE_S:
				atoms[i].s = bis.readS();
				break;
			case SerialAtom.TYPE_AB:
				atoms[i].ab = bis.readAB();
				break;
			case SerialAtom.TYPE_AY:
				atoms[i].ay = bis.readAY();
				break;
			case SerialAtom.TYPE_AN:
				atoms[i].an = bis.readAN();
				break;
			case SerialAtom.TYPE_AI:
				atoms[i].ai = bis.readAI();
				break;
			case SerialAtom.TYPE_AS:
				atoms[i].as = bis.readAS();
				break;
			default:
				Log.bug("Feb 22, 2009.6:25:57 PM");
				break;
			}
		}

		s.notifyAtomsUpdated();

		if (bis.available() > 0) {
			throw new BinaryDataExecption(bis.available() + " unused bytes");
		}

	}

	/**
	 * Converts the given {@link ISerializable} into a byte array.
	 * 
	 * @param s
	 *            the serializable (may be <code>null</code>)
	 * 
	 * @return a byte array or <code>null</code> if <em>s</em> is
	 *         <code>null</code>
	 */
	public static byte[] out(ISerializable s) {

		if (s == null) {
			return null;
		}

		final BaOut bos = new BaOut(256); // should be enough for most messages

		final SerialAtom atoms[] = s.getAtoms();

		final int len = atoms.length;

		for (int i = 0; i < len; i++) {

			switch (atoms[i].type) {
			case SerialAtom.TYPE_Y:
				bos.writeY(atoms[i].y);
				break;
			case SerialAtom.TYPE_B:
				bos.writeB(atoms[i].b);
				break;
			case SerialAtom.TYPE_N:
				bos.writeN(atoms[i].n);
				break;
			case SerialAtom.TYPE_I:
				bos.writeI(atoms[i].i);
				break;
			case SerialAtom.TYPE_X:
				bos.writeX(atoms[i].x);
				break;
			case SerialAtom.TYPE_S:
				bos.writeS(atoms[i].s);
				break;
			case SerialAtom.TYPE_AB:
				bos.writeAB(atoms[i].ab);
				break;
			case SerialAtom.TYPE_AY:
				bos.writeAY(atoms[i].ay);
				break;
			case SerialAtom.TYPE_AN:
				bos.writeAN(atoms[i].an);
				break;
			case SerialAtom.TYPE_AI:
				bos.writeAI(atoms[i].ai);
				break;
			case SerialAtom.TYPE_AS:
				bos.writeAS(atoms[i].as);
				break;
			default:
				Log.bug("Feb 22, 2009.6:26:09 PM");
				break;
			}
		}

		return bos.toByteArray();

	}

}
