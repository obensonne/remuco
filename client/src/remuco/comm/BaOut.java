package remuco.comm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import remuco.util.Log;

/**
 * Extends {@link ByteArrayOutputStream} by some methods for convenient writing
 * of Remuco specific basic data types.
 * 
 * Some problems may occur when strings get written which have character not
 * contained in the default character encoding. It is not possible to detect
 * such a situation, so in that case the special characters get lost when
 * written into this output stream. This is probably ok, since the client only
 * sends pids and plids which should be ASCII.
 * 
 * @see Serial
 * @see BaIn
 * 
 * @author Oben Sonne
 * 
 */
public final class BaOut extends ByteArrayOutputStream {

	protected BaOut() {
		this(0);
	}

	protected BaOut(int size) {
		super(size);
	}

	public byte[] getBuf() {
		return buf;
	}

	/**
	 * Overrides {@link ByteArrayOutputStream#write(byte[])}. Does exactly the
	 * same but catches the senseless {@link IOException}.
	 */
	public void write(byte[] ba) {
		try {
			super.write(ba);
		} catch (IOException e) {
			// should not happen on a ByteArrayOutputStream
		}
	}

	/**
	 * Write a string vector prefixed by a type code, encoding and length.
	 * 
	 * @param sv
	 *            the string vector
	 * 
	 * @see BaIn#readAS()
	 */
	public void writeAS(String[] sv) {

		write(SerialAtom.TYPE_AS);

		if (sv == null) {
			writeInt(0); // num strings
			return;
		}

		writeInt(sv.length); // num strings

		for (int i = 0; i < sv.length; i++) { // strings
			write(sv[i]);
		}
	}

	/**
	 * Writes a byte array with a prefixed length value.
	 * 
	 * @param ba
	 *            the byte array
	 * 
	 * @see BaIn#readAY()
	 */
	public void writeAY(byte[] ba) {

		write(SerialAtom.TYPE_AY);

		if (ba == null) {
			writeInt(0);
		} else {
			writeInt(ba.length);
			write(ba);
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

	/** Same as {@link #writeI(int)} but without a prefixed type code. */
	public void writeInt(int i) {

		write((i >> 24) & 0xff);
		write((i >> 16) & 0xff);
		write((i >> 8) & 0xff);
		write(i & 0xff);
	}

	/**
	 * Writes an int as {@link #writeInt(int)} but here at a specific position
	 * into the underlying byte array.
	 * 
	 * @param i
	 *            the int
	 * @param at
	 *            the position to write to
	 */
	public void writeIntAt(int i, int at) {

		if (at + 4 > count) {
			Log.bug("Feb 22, 2009.6:27:49 PM");
			return;
		}

		int tmp = count;

		count = at;

		writeInt(i);

		count = tmp;

	}

	/**
	 * Writes a long (in net byte order) prefixed by a type code.
	 * 
	 * @param l
	 *            the long to write
	 * 
	 * @see BaIn#readL()
	 */
	public void writeL(long l) {

		write(SerialAtom.TYPE_L);
		
		write((int) (l >> 56) & 0xff);
		write((int) (l >> 48) & 0xff);
		write((int) (l >> 40) & 0xff);
		write((int) (l >> 32) & 0xff);
		write((int) (l >> 24) & 0xff);
		write((int) (l >> 16) & 0xff);
		write((int) (l >> 8) & 0xff);
		write((int) l & 0xff);
	}

	/**
	 * Writes a string prefixed by a type code and an encoding string.
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
			writeInt(0);
		} else {
			try {
				ba = s.getBytes(Serial.ENCODING);
			} catch (UnsupportedEncodingException e) {
				// fall back to default encoding
				// we are lucky if it is compatible to UTF-8
				ba = s.getBytes();
			}
			writeInt(ba.length);
			write(ba);
		}
	}

}
