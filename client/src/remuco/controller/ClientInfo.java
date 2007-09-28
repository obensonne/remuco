package remuco.controller;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

import remuco.comm.IStructuredData;
import remuco.util.Tools;

public final class ClientInfo implements IStructuredData {

	public static final int[] sdFormatVector = new int[] { DT_INT, 3, DT_SV, 1 };

	private Object[] serialBdv = new Object[2];

	protected ClientInfo() {

		String[] encs;

		int memLimit;

		// just to get the screen size
		Canvas c = new Canvas() {
			protected void paint(Graphics g) {
			}
		};

		encs = Tools.getSupportedEncodings();

		memLimit = (int) Runtime.getRuntime().freeMemory();

		serialBdv[0] = new int[] { memLimit, c.getWidth(), c.getHeight() };

		serialBdv[1] = new String[][] { encs };

	}

	public boolean equals(Object obj) {

		if (obj == this)
			return true;

		if (obj == null)
			return false;

		if (!(obj instanceof ClientInfo))
			return false;

		ClientInfo ci = (ClientInfo) obj;
		int i, j;

		int[] iv1, iv2;
		String[][] svv1, svv2;

		if (serialBdv.length != ci.serialBdv.length)
			return false;

		iv1 = (int[]) serialBdv[0];
		iv2 = (int[]) ci.serialBdv[0];

		if (iv1 != iv2 && (iv1 == null || iv2 == null))
			return false;

		if (iv1 != null) {
			if (iv1.length != iv2.length)
				return false;
			for (i = 0; i < iv1.length; i++) {
				if (iv1[i] != iv2[i])
					return false;
			}
		}

		svv1 = (String[][]) serialBdv[1];
		svv2 = (String[][]) ci.serialBdv[1];

		for (i = 0; i < svv1.length; i++) {
			if (svv1[i] == svv2[i])
				continue;
			if (svv1[i] == null || svv2[i] == null)
				return false;

			if (svv1[i].length != svv2[i].length)
				return false;
			for (j = 0; j < svv1[i].length; j++) {
				if (svv1[i][j] == svv2[i][j])
					continue;
				if (svv1[i][j] == null || svv2[i][j] == null)
					return false;
				if (!svv1[i][j].equals(svv2[i][j]))
					return false;
			}
		}

		return true;

	}

	public Object[] sdGet() {

		return serialBdv;

	}

	public void sdSet(Object[] bdv) {

		serialBdv = bdv;

	}

	public String toString() {

		StringBuffer sb = new StringBuffer();

		int i;
		int[] iv = (int[]) serialBdv[0];

		sb.append("ML: ").append(iv[0]).append(" IW: ").append(iv[1]);
		sb.append(" IH: ").append(iv[2]);

		String[][] svv = (String[][]) serialBdv[1];
		String[] sv = svv[0];

		sb.append("ENCS: ");
		for (i = 0; i < sv.length; i++) {
			sb.append(sv[i]).append(", ");
		}

		return sb.toString();
	}

}
