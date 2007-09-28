package remuco.player;

import remuco.comm.IStructuredData;

public final class StringParam implements IStructuredData {

	public static final int[] sdFormatVector = new int[] { DT_STR, 1 };

	private final Object[] bdv = new Object[] { new String[1] };

	public String getParam() {

		return ((String[]) bdv[0])[0];

	}

	public Object[] sdGet() {
		return bdv;
	}

	public void sdSet(Object[] bdv) {
		((String[]) this.bdv[0])[0] = ((String[]) bdv[0])[0];
	}

	public void setParam(String param) {

		((String[]) bdv[0])[0] = param;

	}

	public static String getParam(Object[] sdv) {
		return ((String[]) sdv[0])[0];		
	}
	
}
