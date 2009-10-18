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
package remuco.client.common.serial;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Extends {@link ByteArrayOutputStream} by some methods for convenient writing
 * of Remuco specific basic data types.
 * <p>
 * The string related parts may fail if {@link Serial#ENCODING} is not
 * supported.
 * 
 * @see Serial
 * @see BaIn
 */
public final class BaOut extends ByteArrayOutputStream {

	protected BaOut(int size) {
		super(size);
	}

	/**
	 * Overrides {@link ByteArrayOutputStream#write(byte[])}. Does exactly the
	 * same but catches the senseless {@link IOException}.
	 */
	public void write(byte[] ab) {
		try {
			super.write(ab);
		} catch (IOException e) {
			// should not happen on a ByteArrayOutputStream
		}
	}

	/**
	 * Writes a byte array with a prefixed length value.
	 * 
	 * @param ab
	 *            the byte array
	 * 
	 * @see BaIn#readAY()
	 */
	public void writeAB(boolean[] ab) {

		write(SerialAtom.TYPE_AB);

		if (ab == null) {
			writeInt(0);
		} else {
			writeInt(ab.length);
			for (int i = 0; i < ab.length; i++) {
				write(ab[i] ? 1 : 0);
			}
		}
	}

	/**
	 * Writes an integer array with a prefixed length value.
	 * 
	 * @param ai
	 *            the integer array
	 * 
	 * @see BaIn#readAI()
	 */
	public void writeAI(int[] ai) {

		write(SerialAtom.TYPE_AI);

		if (ai == null) {
			writeInt(0); // num ints
			return;
		}

		writeInt(ai.length); // num ints

		for (int i = 0; i < ai.length; i++) { // ints
			writeInt(ai[i]);
		}
	}

	/**
	 * Writes a short array with a prefixed length value.
	 * 
	 * @param an
	 *            the short array
	 * 
	 * @see BaIn#readAN()
	 */
	public void writeAN(short[] an) {

		write(SerialAtom.TYPE_AN);

		if (an == null) {
			writeInt(0); // num shorts
			return;
		}

		writeInt(an.length); // num shorts

		for (int i = 0; i < an.length; i++) { // ints
			writeShort(an[i]);
		}
	}

	/**
	 * Write a string array prefixed by a type code.
	 * 
	 * @param as
	 *            the string vector
	 * 
	 * @see BaIn#readAS()
	 */
	public void writeAS(String[] as) {

		write(SerialAtom.TYPE_AS);

		if (as == null) {
			writeInt(0); // num strings
			return;
		}

		writeInt(as.length); // num strings

		for (int i = 0; i < as.length; i++) { // strings
			write(as[i]);
		}
	}

	/**
	 * Writes a byte array with a prefixed length value.
	 * 
	 * @param ay
	 *            the byte array
	 * 
	 * @see BaIn#readAY()
	 */
	public void writeAY(byte[] ay) {

		write(SerialAtom.TYPE_AY);

		if (ay == null) {
			writeInt(0);
		} else {
			writeInt(ay.length);
			write(ay);
		}
	}

	/**
	 * Writes a boolean prefixed by a type code.
	 * 
	 * @param b
	 *            the boolean to write
	 * 
	 * @see BaIn#readB()
	 */
	public void writeB(boolean b) {

		write(SerialAtom.TYPE_B);
		write(b ? 1 : 0);

	}

	/**
	 * Writes an int (in net byte order) prefixed by a type code.
	 * 
	 * @param i
	 *            the int to write
	 * 
	 * @see BaIn#readI()
	 */
	public void writeI(int i) {

		write(SerialAtom.TYPE_I);
		writeInt(i);
	}

	/**
	 * Writes a short (in net byte order) prefixed by a type code.
	 * 
	 * @param i
	 *            the short to write
	 * 
	 * @see BaIn#readN()
	 */
	public void writeN(short i) {

		write(SerialAtom.TYPE_N);
		writeShort(i);
	}

	/**
	 * Writes a string prefixed by a type code.
	 * 
	 * @param s
	 *            the string
	 * 
	 * @see BaIn#readS()
	 */
	public void writeS(String s) {

		write(SerialAtom.TYPE_S);
		write(s);

	}

	/**
	 * Writes a long (in net byte order) prefixed by a type code.
	 * 
	 * @param x
	 *            the long to write
	 * 
	 * @see BaIn#readX()
	 */
	public void writeX(long x) {

		write(SerialAtom.TYPE_X);

		write((int) (x >> 56) & 0xff);
		write((int) (x >> 48) & 0xff);
		write((int) (x >> 40) & 0xff);
		write((int) (x >> 32) & 0xff);
		write((int) (x >> 24) & 0xff);
		write((int) (x >> 16) & 0xff);
		write((int) (x >> 8) & 0xff);
		write((int) x & 0xff);
	}

	/**
	 * Writes a byte prefixed by a type code.
	 * 
	 * @param y
	 *            the byte to write
	 * 
	 * @see BaIn#readY()
	 */
	public void writeY(byte y) {

		write(SerialAtom.TYPE_Y);
		write(y);

	}

	/**
	 * Writes a string, <em>not</em> prefixed by a type code.
	 * 
	 * @param s
	 *            the string
	 * 
	 * @see BaIn#readS()
	 */
	private void write(String s) {

		byte[] ba;

		if (s == null) {
			writeShort((short) 0);
		} else {
			try {
				ba = s.getBytes(Serial.ENCODING);
			} catch (UnsupportedEncodingException e) {
				// fall back to default encoding
				// we are lucky if it is compatible to UTF-8
				ba = s.getBytes();
			}
			writeShort((short) ba.length);
			write(ba);
		}
	}

	/** Same as {@link #writeI(int)} but without a prefixed type code. */
	private void writeInt(int i) {

		write((i >> 24) & 0xff);
		write((i >> 16) & 0xff);
		write((i >> 8) & 0xff);
		write(i & 0xff);
	}

	/** Same as {@link #writeN(int)} but without a prefixed type code. */
	private void writeShort(short n) {

		write((n >> 8) & 0xff);
		write(n & 0xff);
	}

	// /**
	// * Writes an int as {@link #writeInt(int)} but here at a specific position
	// * into the underlying byte array.
	// *
	// * @param i
	// * the int
	// * @param at
	// * the position to write to
	// */
	// private void writeIntAt(int i, int at) {
	//
	// if (at + 4 > count) {
	// Log.bug("Feb 22, 2009.6:27:49 PM");
	// return;
	// }
	//
	// int tmp = count;
	//
	// count = at;
	//
	// writeInt(i);
	//
	// count = tmp;
	//
	// }

}
