package remuco.comm;

/**
 * An exception which indicates that received binary data is malformed (e.g. not
 * enough data or some bytes have values which they should not have).
 * 
 * @author Christian Buennig
 * 
 */
public final class BinaryDataExecption extends Exception {

	/**
	 * 
	 * @param msg
	 *            description of the specifc problem with the binary data
	 */
	public BinaryDataExecption(String msg) {
		super(msg);
	}

}
