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

public class SerialAtom {

	/** Data type: byte **/
	public static final int TYPE_Y = 1;

	/** Data type: integer **/
	public static final int TYPE_I = 2;

	/** Data type: boolean **/
	public static final int TYPE_B = 3;

	/** Data type: string **/
	public static final int TYPE_S = 4;

	/** Data type: array of data **/
	public static final int TYPE_AY = 5;

	/** Data type: array of integers **/
	public static final int TYPE_AI = 6;

	/** Data type: array of strings **/
	public static final int TYPE_AS = 7;

	/** Data type: long **/
	public static final int TYPE_X = 8;

	/** Data type: short **/
	public static final int TYPE_N = 9;

	/** Data type: array of shorts **/
	public static final int TYPE_AN = 10;

	/** Data type: array of boolean **/
	public static final int TYPE_AB = 11;

	public static SerialAtom[] build(int fmt[]) {

		final SerialAtom atoms[] = new SerialAtom[fmt.length];

		for (int i = 0; i < atoms.length; i++) {
			atoms[i] = new SerialAtom(fmt[i]);
		}

		return atoms;
	}

	public final int type;

	public int i;

	public short n;

	public long x;

	public String s;

	public String[] as;

	public int[] ai;

	public short[] an;

	public byte y;

	public boolean b;

	public byte[] ay;

	public boolean[] ab;

	private SerialAtom(int type) {
		this.type = type;
	}

}
