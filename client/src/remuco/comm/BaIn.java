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
package remuco.comm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Extends {@link ByteArrayInputStream} by some methods for convinient reading
 * of Remuco specific basic data types.
 * <p>
 * The string related parts may fail if {@link Serial#ENCODING} is not
 * supported.
 * 
 * @see Serial
 * @see BaOut
 * 
 * @author Oben Sonne
 * 
 */
public final class BaIn extends ByteArrayInputStream {

	protected BaIn(byte[] ba) {
		super(ba);
	}

	/**
	 * Reads a boolean array which is prefixed by a type code and a length
	 * value.
	 * 
	 * @see BaOut#writeAB(boolean[])
	 * 
	 * @return the boolean array (never <code>null</code>)
	 * @throws BinaryDataExecption
	 */
	public boolean[] readAB() throws BinaryDataExecption {

		checkType(SerialAtom.TYPE_AB);

		final byte ay[] = readBytes();

		final boolean ab[] = new boolean[ay.length];

		for (int i = 0; i < ab.length; i++) {
			ab[i] = ay[i] == 0 ? false : true;
		}

		return ab;
	}

	/**
	 * Read an integer array which is prefixed by a type code and a length
	 * value.
	 * 
	 * @see BaOut#writeAI(int[])
	 * @see Serial
	 * 
	 * @return the integer array (never <code>null</code>)
	 * @throws BinaryDataExecption
	 */
	public int[] readAI() throws BinaryDataExecption {

		checkType(SerialAtom.TYPE_AI);

		final int len = readInt(); // num ints

		final int ai[] = new int[len];

		for (int i = 0; i < len; i++) {
			ai[i] = readInt();
		}

		return ai;
	}

	/**
	 * Read a short array which is prefixed by a type code and a length value.
	 * 
	 * @see BaOut#writeAN(short[])
	 * @see Serial
	 * 
	 * @return the short array (never <code>null</code>)
	 * @throws BinaryDataExecption
	 */
	public short[] readAN() throws BinaryDataExecption {

		checkType(SerialAtom.TYPE_AN);

		final int len = readInt(); // num shorts

		final short an[] = new short[len];

		for (int i = 0; i < len; i++) {
			an[i] = readShort();
		}

		return an;
	}

	/**
	 * Read a string array which is prefixed by a type code and a length value.
	 * 
	 * @see BaOut#writeAS(String[])
	 * @see Serial
	 * 
	 * @return the string array (never <code>null</code>)
	 * @throws BinaryDataExecption
	 */
	public String[] readAS() throws BinaryDataExecption {

		checkType(SerialAtom.TYPE_AS);

		final int len = readInt(); // num strings

		final String as[] = new String[len];

		for (int i = 0; i < len; i++) {
			as[i] = readString();
		}

		return as;
	}

	/**
	 * Reads a byte array which is prefixed by a type code and a length value.
	 * 
	 * @see BaOut#writeAY(byte[])
	 * 
	 * @return the byte array (never <code>null</code>)
	 * @throws BinaryDataExecption
	 */
	public byte[] readAY() throws BinaryDataExecption {

		checkType(SerialAtom.TYPE_AY);

		return readBytes();
	}

	/**
	 * Read the next byte as a boolean.
	 * 
	 * @see BaOut#write(boolean)
	 * 
	 * @return the boolean
	 * @throws BinaryDataExecption
	 *             if the boolean could not be read because there is not enough
	 *             data
	 */
	public boolean readB() throws BinaryDataExecption {

		checkType(SerialAtom.TYPE_B);

		if (pos + 1 > count)
			throw new BinaryDataExecption("not enough data");

		final int b = read();

		if (b == 0)
			return false;
		else
			return true;

	}

	/**
	 * Read the next 4 data as an int (in net byte order).
	 * 
	 * @see BaOut#writeI(int)
	 * 
	 * @return the int
	 * @throws BinaryDataExecption
	 *             if the int could not be read because there is not enough data
	 */
	public int readI() throws BinaryDataExecption {

		checkType(SerialAtom.TYPE_I);

		return readInt();
	}

	/**
	 * Read the next 2 data as a short (in net byte order).
	 * 
	 * @see BaOut#writeN(int)
	 * 
	 * @return the short
	 * @throws BinaryDataExecption
	 *             if the short could not be read because there is not enough
	 *             data
	 */
	public short readN() throws BinaryDataExecption {

		checkType(SerialAtom.TYPE_N);

		return readShort();
	}

	/**
	 * Read a string which is prefixed by an encoding string.
	 * 
	 * @see BaOut#write(String, boolean)
	 * 
	 * @return the string (never <code>null</code>)
	 * @throws BinaryDataExecption
	 */
	public String readS() throws BinaryDataExecption {

		checkType(SerialAtom.TYPE_S);

		return readString();
	}

	/**
	 * Read the next 8 data as a long (in net byte order).
	 * 
	 * @see BaOut#writeX(long)
	 * 
	 * @return the long
	 * @throws BinaryDataExecption
	 *             if the long could not be read because there is not enough
	 *             data
	 */
	public int readX() throws BinaryDataExecption {

		checkType(SerialAtom.TYPE_X);

		if (pos + 8 > count)
			throw new BinaryDataExecption("not enough data");

		return (((read() & 0xff) << 56) | ((read() & 0xff) << 48)
				| ((read() & 0xff) << 40) | ((read() & 0xff) << 32)
				| ((read() & 0xff) << 24) | ((read() & 0xff) << 16)
				| ((read() & 0xff) << 8) | (read() & 0xff));

	}

	/**
	 * Read the next for data as a byte.
	 * 
	 * @see BaOut#writeY(int)
	 * 
	 * @return the byte
	 * @throws BinaryDataExecption
	 *             if the byte could not be read because there is not enough
	 *             data
	 */
	public byte readY() throws BinaryDataExecption {

		checkType(SerialAtom.TYPE_Y);

		if (pos + 1 > count)
			throw new BinaryDataExecption("not enough data");

		return (byte) read();

	}

	/**
	 * Reads the next byte, interprets it as a type code and compares it to an
	 * expected type code.
	 * 
	 * @param exptected
	 * @throws BinaryDataExecption
	 *             if expected and real type code differ
	 */
	private void checkType(int exptected) throws BinaryDataExecption {

		if (pos + 1 > count)
			throw new BinaryDataExecption("not enough data");

		final int real = read();

		if (real != exptected)
			throw new BinaryDataExecption("type mismatch (exp: " + exptected
					+ ", real: " + real + ")");
	}

	/** Same as {@link #readAY()} but without reading a type code. */
	private byte[] readBytes() throws BinaryDataExecption {

		// check if size can be read

		if (pos + 4 > count)
			throw new BinaryDataExecption("not enough data");

		final int len = readInt();

		// check if there is enough data

		if (pos + len > count)
			throw new BinaryDataExecption("not enough data");

		// read the byte array

		final byte ay[] = new byte[len];

		try {
			read(ay);
		} catch (IOException e) {
			// should not happen on a ByteArrayInputStream
		}

		return ay;
	}

	/** Like {@link #readI()} but without reading a type code before. */
	private int readInt() throws BinaryDataExecption {

		if (pos + 4 > count)
			throw new BinaryDataExecption("not enough data");

		return (((read() & 0xff) << 24) | ((read() & 0xff) << 16)
				| ((read() & 0xff) << 8) | (read() & 0xff));

	}

	/** Like {@link #readN()} but without reading a type code before. */
	private short readShort() throws BinaryDataExecption {

		if (pos + 2 > count)
			throw new BinaryDataExecption("not enough data");

		return (short) (((read() & 0xff) << 8) | (read() & 0xff));

	}

	/**
	 * Reads a string without reading a type code first.
	 * 
	 * @return the string (never <code>null</code>)
	 * 
	 * @throws BinaryDataExecption
	 */
	private String readString() throws BinaryDataExecption {

		final int len = readShort(); // len string

		if (pos + len > count)
			throw new BinaryDataExecption("not enough data");

		String s = null;

		try {
			s = new String(buf, pos, len, Serial.ENCODING); // string
		} catch (UnsupportedEncodingException uee) {
			try {
				// fall back to default encoding
				// we are lucky if it is compatible to UTF-8
				s = new String(buf, pos, len);
			} catch (Exception e) {
				s = "CharsetEncodingError";
			}
		}

		pos += len;

		return s;
	}

}
