package remuco.comm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import remuco.Config;

/**
 * Extends {@link ByteArrayInputStream} by some methods for convinient reading
 * of Remuco specific basic data types.
 * <p>
 * The string related parts only works if the VM's default encoding is ASCII
 * compatible since encoding strings are in ASCII and get read with default
 * character encoding (see {@link #readS()} and {@link #readAS()}).
 * 
 * @see Serial
 * @see BaOut
 * 
 * @author Christian Buennig
 * 
 */
public final class BaIn extends ByteArrayInputStream {

	protected BaIn(byte[] ba) {
		super(ba);
	}

	/**
	 * Read a string vector which is prefixed by an encoding string and a length
	 * value.
	 * 
	 * @see BaOut#write(String[], boolean)
	 * @see Serial
	 * 
	 * @return the string vector (never <code>null</code>)
	 * @throws BinaryDataExecption
	 */
	public String[] readAS() throws BinaryDataExecption {

		int num;
		String[] sv;

		checkType(SerialAtom.TYPE_AS);

		num = readInt(); // num strings

		sv = new String[num];

		for (int i = 0; i < num; i++) {
			sv[i] = readString();
		}

		return sv;
	}

	/**
	 * Reads a byte array which is prefixed by a length value.
	 * 
	 * @see BaOut#writeAY(byte[])
	 * 
	 * @return the byte array (never <code>null</code>)
	 * @throws BinaryDataExecption
	 */
	public byte[] readAY() throws BinaryDataExecption {

		int len;
		byte[] ba;

		checkType(SerialAtom.TYPE_AY);

		// check if size can be read

		if (pos + 4 > count)
			throw new BinaryDataExecption("not enough data");

		len = readInt();

		// check if there is enough data

		if (pos + len > count)
			throw new BinaryDataExecption("not enough data");

		// read the byte array

		ba = new byte[len];

		try {
			read(ba);
		} catch (IOException e) {
			// should not happen on a ByteArrayInputStream
		}

		return ba;

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

		int i;

		checkType(SerialAtom.TYPE_B);

		if (pos + 1 > count)
			throw new BinaryDataExecption("not enough data");

		i = read();

		if (i == 0)
			return false;
		else
			return true;

	}

	/**
	 * Read the next for bytes as an int (in net byte order).
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

	/** Like {@link #readI()} but without reading a type code before. */
	public int readInt() throws BinaryDataExecption {

		if (pos + 4 > count)
			throw new BinaryDataExecption("not enough data");

		return (((read() & 0xff) << 24) | ((read() & 0xff) << 16)
				| ((read() & 0xff) << 8) | (read() & 0xff));

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
	 * Read the next for bytes as a byte.
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

		int real;

		if (pos + 1 > count)
			throw new BinaryDataExecption("not enough data");

		real = read();

		if (real != exptected)
			throw new BinaryDataExecption("type mismatch (exp: " + exptected
					+ ", real: " + real + ")");
	}

	/**
	 * Reads a string without reading a type code and encoding string first.
	 * 
	 * @return the string (never <code>null</code>)
	 * 
	 * @throws BinaryDataExecption
	 */
	private String readString() throws BinaryDataExecption {

		String s = null;
		int len;

		len = readInt(); // len string

		if (pos + len > count)
			throw new BinaryDataExecption("not enough data");

		try {
			s = new String(buf, pos, len, Config.encoding); // string
		} catch (UnsupportedEncodingException e) {
			throw new BinaryDataExecption("enc error: " + e.getMessage());
		}

		pos += len;

		return s;
	}

}
