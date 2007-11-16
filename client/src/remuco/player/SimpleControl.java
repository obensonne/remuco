package remuco.player;

import remuco.comm.IStructuredData;

public final class SimpleControl implements IStructuredData {

	public static final int CMD_PLAYPAUSE = 0;

	public static final int CMD_STOP = 1;

	public static final int CMD_RESTART = 2;

	public static final int CMD_NEXT = 3;

	public static final int CMD_PREV = 4;

	public static final int CMD_JUMP = 5;

	public static final int CMD_VOLUME = 6;

	public static final int CMD_RATE = 7;

	/** <b>FUTUTRE FEATURE</b> */
	public static final int CMD_VOTE = 8;

	public static final int CMD_SEEK = 9;

	public static final int CMD_COUNT = 10;

	public static final int[] sdFormatVector = new int[] { DT_INT, 2 };

	/**
	 * The command and param of the simple control packed into a 2 element int
	 * array.
	 */
	private final int[] serialIv = new int[2];

	/**
	 * Contains the attributes to be serialized in the format described by
	 * {@link #sdFormatVector}.
	 */
	private final Object[] serialBdv = new Object[] { serialIv };

	/**
	 * Creates a simple control with the given command and param.
	 * 
	 * @param cmd
	 *            use constants <code>CMD_..</code>, e.g. {@link #CMD_NEXT}.
	 * @param param
	 */
	public SimpleControl() {

		serialIv[0] = 0;
		serialIv[1] = 0;

	}

	public synchronized boolean equals(Object o) {
		if (o == null)
			return false;
		if (this == o)
			return true;
		if (!(o instanceof SimpleControl))
			return false;
		SimpleControl sc = (SimpleControl) o;
		synchronized (sc) {
			return getCmd() == sc.getCmd() && getParam() == sc.getParam();
		}
	}

	public synchronized int getCmd() {
		return serialIv[0];
	}

	public synchronized int getParam() {
		return serialIv[1];
	}

	public Object[] sdGet() {
		return serialBdv;
	}

	public void sdSet(Object[] bdv) {

		int[] iv = (int[]) bdv[0];

		serialIv[0] = iv[0];
		serialIv[1] = iv[1];

	}

	/**
	 * Set command and param.
	 * 
	 * @param cmd
	 * @param param
	 */
	public synchronized void set(int cmd, int param) {

		serialIv[0] = cmd;
		serialIv[1] = param;
	}

	public synchronized String toString() {

		StringBuffer sb = new StringBuffer();
		sb.append("CODE: ").append(getCmd());
		sb.append(" - Param: ").append(getParam());

		return sb.toString();
	}

}
