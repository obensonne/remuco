package remuco.comm;

import remuco.util.Log;

public class SerialAtom {

	public static final int TYPE_NONE = 0;
	public static final int TYPE_Y = 1;
	public static final int TYPE_I = 2;
	public static final int TYPE_B = 3;
	public static final int TYPE_S = 4;
	public static final int TYPE_AY = 5;
	public static final int TYPE_AI = 6;
	public static final int TYPE_AS = 7;
	public static final int TYPE_L = 8;

	public final int type;

	public int i;
	
	public long l;

	public String s;

	public String[] as;
	
	public int[] ai;

	public byte y;

	public boolean b;

	public byte[] ay;

	public SerialAtom(int type) {
		this.type = type;
	}
	
	public final SerialAtom copy() {
		
		SerialAtom sa = new SerialAtom(type);
		
		switch (type) {
		case TYPE_Y:
			sa.y = y;
			break;
		case TYPE_B:
			sa.b = b;			
			break;
		case TYPE_I:
			sa.i = i;			
			break;
		case TYPE_L:
			sa.l = l;			
			break;
		case TYPE_S:
			sa.s = new String(s);
			break;
		case TYPE_AY:
			sa.ay = new byte[ay.length];
			for (int j = 0; j < ay.length; j++) {
				sa.ay[j] = ay[j];
			}
			break;
		case TYPE_AI:
			sa.ai = new int[ai.length];
			for (int j = 0; j < ai.length; j++) {
				sa.ai[j] = ai[j];
			}
			break;
		case TYPE_AS:
			sa.as = new String[as.length];
			for (int j = 0; j < as.length; j++) {
				sa.as[j] = new String(as[j]);
			}			
			break;
		default:
			
			Log.bug("Feb 5, 2009.11:04:32 PM");
			
			break;
		}
		
		return sa;
	}
	
}
