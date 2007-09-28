package remuco.comm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import remuco.util.Log;
import remuco.util.Tools;

/**
 * Extends {@link ByteArrayOutputStream} by some methods for convinient writing
 * of Remuco specific basic data types.
 * 
 * @see Serializer
 * 
 * @author Christian Buennig
 * 
 */
public final class BaOut extends ByteArrayOutputStream {

	protected BaOut() {
		super();
	}

	protected BaOut(int size) {
		super(size);
	}

	public byte[] getBuf() {
		return buf;
	}

	/**
	 * Writes a byte array with a prefixed null pointer flag and size value.
	 * 
	 * @param ba
	 *            the byte array
	 * 
	 * @see BaIn#readBa()
	 */
	public void writeBa(byte[] ba) {

		if (ba != null) {
			write(1);
			writeInt(ba.length);
			write(ba);
		} else {
			write(0);
		}

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
	 * Writes a string with a prefixed null pointer flag.
	 * 
	 * @param s
	 *            the string
	 * 
	 * @see BaIn#readString()
	 */
	public void write(String s) {

		if (s != null) {
			write(1);
			write(s.getBytes());
			write(0);
		} else {
			write(0);
		}

	}

	/**
	 * Writes a string with a prefixed null pointer flag. Conversion to bytes
	 * uses the given character encoding.
	 * 
	 * @param s
	 *            the string
	 * 
	 * @see BaIn#readString(String)
	 */
	public void write(String s, String enc) throws UnsupportedEncodingException {

		if (s != null) {
			write(1);
			write(s.getBytes(enc));
			write(0);
		} else {
			write(0);
		}

	}

	/**
	 * Write a string vector with prefixed [null pointer flag,] size and
	 * encoding.
	 * 
	 * @param sv
	 *            the string vector
	 * @param withNullPointerFlag
	 *            If <code>true</code>, the string vector will be prefixed
	 *            with a null pointer flag (must be <code>false</code> if the
	 *            string vector represents a concatenation of data elements of
	 *            type {@link IStructuredData#DT_STR} and must be
	 *            <code>true</code> if the string vector is one of several
	 *            vectors, i.e. a concatenation of data elements of type
	 *            {@link IStructuredData#DT_SV}).
	 * 
	 * @see BaIn#readStringV(int)
	 */
	public void write(String[] sv, boolean withNullPointerFlag) {

		int i, m = 0;

		if (withNullPointerFlag) {
			if (sv == null) {
				write(0);
				return;
			}

			write(1);

			writeInt(0); // write true size later
			m = count;
		}

		Log.asssert(sv);

		write(Tools.defaultEncoding);

		for (i = 0; i < sv.length; i++) {

			write(sv[i]);

		}

		if (withNullPointerFlag) {
			Log.ln("[BO] write sv size (" + (count - m) + ") at " + (m - 4));
			writeIntAt(count - m, m - 4);
		}

	}

	/**
	 * Writes an int in net byte order. In contrast to {@link #write(int)} which
	 * casts the given int to a byte and writes only one byte.
	 * 
	 * @param i
	 *            the int to write
	 * 
	 * @see BaIn#readInt()
	 */
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

		Log.asssert(at + 4 <= count);

		int m = count;

		count = at;

		writeInt(i);

		count = m;

	}

}
