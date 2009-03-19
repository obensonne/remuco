package remuco.comm;

/**
 * An exception which indicates that received binary data is malformed (e.g. not
 * enough data or some data have values which they should not have).
 * 
 * @author Oben Sonne
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
