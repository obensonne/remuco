package remuco.comm;

/**
 * To be implemented by all classes which shall be serializable. These classes'
 * attribtued can be set and retreived as <i>structered data</i> via the
 * methods {@link #sdGet()} and {@link #sdSet(Object[])}.
 * 
 * @see Serial
 * @see Message
 * 
 * @author Christian Buennig
 * 
 */
public interface IStructuredData {

	public static final int DT_NONE = 0;

	/**
	 * An <code>int</code>.
	 */
	public static final int DT_INT = 1;

	/**
	 * A {@link String}.
	 */
	public static final int DT_STR = 2;

	/**
	 * A <code>byte</code> array.
	 */
	public static final int DT_BA = 3;

	/**
	 * An <code>int</code> array.
	 */
	public static final int DT_IV = 4;

	/**
	 * A {@link String} array.
	 */
	public static final int DT_SV = 5;

	public static final int DT_COUNT = 6;

	/**
	 * Must be set! Describes how the data returned from {@link #sdGet()} and to
	 * set with {@link #sdSet(Object[])} is structured / formatted.
	 */
	public static int[] sdFormatVector = null;

	/**
	 * Get the object's attribute values as structured data..
	 * 
	 * @return the objects attribute values formatted as structured data.
	 */
	public Object[] sdGet();

	/**
	 * Set the object's attributes with the values in the structured data.
	 * 
	 * @param bdv
	 *            the structured data to use for setting the attributes
	 */
	public void sdSet(Object[] bdv);
	/*
	 * @throws BinaryDataExecption if the data in the binary data vector is
	 * malformed (e.g. some data is null) - this exception is caused by data
	 * errors that do not break the format of binary data but it's semantics
	 */
}
