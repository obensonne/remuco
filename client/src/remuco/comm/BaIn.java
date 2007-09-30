package remuco.comm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

import remuco.util.Log;

/**
 * Extends {@link ByteArrayInputStream} by some methods for convinient reading
 * of Remuco specific basic data types.
 * 
 * @see Serializer
 * 
 * @author Christian Buennig
 * 
 */
public final class BaIn extends ByteArrayInputStream {

	protected BaIn(byte[] ba) {
		super(ba);
	}

	/**
	 * Reads a byte array which is prefixed by a null pointer flag and a size
	 * value.
	 * 
	 * @see BaOut#writeBa(byte[])
	 * 
	 * @return
	 * @throws BinaryDataExecption
	 */
	public byte[] readBa() throws BinaryDataExecption {

		int size, end;
		byte[] ba;

		// check null pointer flag

		if (read() == 0)
			return null;

		// check if size can be read

		if (count - pos < 4)
			throw new BinaryDataExecption("missing data (to read ba size)");

		size = readInt();

		// check if there is enough data

		end = pos + size;

		if (end > count)
			throw new BinaryDataExecption("missing data (expected ba size "
					+ size + " > remaining bytes " + (count - pos) + " + )");

		// read the byte array

		ba = new byte[size];

		try {
			read(ba);
		} catch (IOException e) {
			// should not happen on a ByteArrayInputStream
		}

		return ba;

	}

	/**
	 * Read the next for bytes as an int in net byte order.
	 * 
	 * @see BaOut#writeInt(int)
	 * 
	 * @return the int
	 * @throws BinaryDataExecption
	 *             if the int could not be read because there is not enough data
	 */
	public int readInt() throws BinaryDataExecption {

		if (pos + 4 > count)
			throw new BinaryDataExecption("not enough data");

		return (((read() & 0xff) << 24) | ((read() & 0xff) << 16)
				| ((read() & 0xff) << 8) | (read() & 0xff));

	}

	/**
	 * Read an int vector.
	 * 
	 * @return the int vector or <code>null</code> if the null pointer flag is
	 *         0
	 * @throws BinaryDataExecption
	 *             if the int vector could not be read because there is not
	 *             enough data
	 */
	public int[] readIntV() throws BinaryDataExecption {

		int i, j, len;
		int[] iv = null;

		if (pos >= count)
			throw new BinaryDataExecption(
					"cannot read int vector (already reached end of data)");

		i = read(); // read null pointer flag

		if (i != 0) {

			len = readInt() / 4; // number of ints

			iv = new int[len];

			for (j = 0; j < len; j++) {

				iv[j] = readInt();

			}
		}

		return iv;

	}

	/**
	 * Read a null pointer prefixed string. Same as {@link #readString(String)}
	 * with parameter <code>null</code>.
	 * 
	 * @see BaOut#write(String)
	 * 
	 * @return the string.
	 * @throws BinaryDataExecption
	 */
	public String readString() throws BinaryDataExecption {

		try {
			return readString(null);
		} catch (UnsupportedEncodingException e) {
			Log.asssertNotReached();
			return null;
		}

	}

	/**
	 * Read a string which is prefixed by a null pointer flag.
	 * 
	 * @see BaOut#write(String, String)
	 * 
	 * @param enc
	 *            the encoding to use when reading
	 * @return the string
	 * @throws UnsupportedEncodingException
	 * @throws BinaryDataExecption
	 */
	public String readString(String enc) throws UnsupportedEncodingException,
			BinaryDataExecption {

		String s = null;
		int i, j;

		if (pos >= count)
			throw new BinaryDataExecption(
					"cannot read string (already reached end of data)");

		i = read();

		if (i != 0) {

			for (j = pos; buf[pos] != 0; pos++) {
				if (pos >= count)
					throw new BinaryDataExecption(
							"cannot read string (no null terminator)");
			}

			if (enc == null)
				s = new String(buf, j, pos - j);
			else
				s = new String(buf, j, pos - j, enc);

			pos++; // skip the terminating null

		}

		return s;

	}

	/**
	 * Read a string vector.
	 * <p>
	 * If the string vector to read is a single one (a concatenation of data
	 * elements of type {@link IStructuredData#DT_STR}) then the size should be
	 * known and param <code>size</code> must be set to the vector's size in
	 * bytes.
	 * <p>
	 * If the string vector to read is one of several vectors (a concatenation
	 * of data elements of type {@link IStructuredData#DT_SV}) size must be
	 * zero.
	 * 
	 * @see BaOut#write(String[], boolean)
	 * @see Serializer
	 * 
	 * @param size
	 *            see description above
	 * 
	 * @return the string vector
	 * @throws BinaryDataExecption
	 */
	public String[] readStringV(int size) throws BinaryDataExecption {

		int end;
		Vector v = new Vector();
		String[] sv;
		String enc, s;

		if (size == 0) { // read a string vector from a string vector array

			// check null pointer flag

			if (read() == 0)
				return null;

			// check if size can be read

			if (count - pos < 4)
				throw new BinaryDataExecption("missing data (to read sv size)");

			size = readInt();

		}

		// check if there is enough data

		end = pos + size;

		if (end > count)
			throw new BinaryDataExecption("missing data (expected sv size "
					+ size + " > remaining bytes " + (count - pos) + " + )");

		// read the encoding string

		enc = readString();

		// read the strings

		while (pos < end) {

			try {
				s = readString(enc);
			} catch (UnsupportedEncodingException e) {
				throw new BinaryDataExecption("enc error: " + e.getMessage());
			}
			// Log.ln("[BI] read string " + s);
			v.addElement(s);

		}

		if (pos != end)
			throw new BinaryDataExecption("strings misaligned");

		sv = new String[v.size()];
		v.copyInto(sv);

		return sv;
	}

}
