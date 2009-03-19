package remuco.comm;

public class SerialAtom {

	/** Data type: byte **/
	public static final int TYPE_Y = 1;

	/** Data type: integer **/
	public static final int TYPE_I = 2;

	/** Data type: boolean **/
	public static final int TYPE_B = 3;

	/** Data type: string **/
	public static final int TYPE_S = 4;

	/** Data type: array of data **/
	public static final int TYPE_AY = 5;

	/** Data type: array of integers **/
	public static final int TYPE_AI = 6;

	/** Data type: array of strings **/
	public static final int TYPE_AS = 7;

	/** Data type: long **/
	public static final int TYPE_X = 8;

	/** Data type: short **/
	public static final int TYPE_N = 9;

	/** Data type: array of shorts **/
	public static final int TYPE_AN = 10;

	/** Data type: array of boolean **/
	public static final int TYPE_AB = 11;

	public static SerialAtom[] build(int fmt[]) {

		final SerialAtom atoms[] = new SerialAtom[fmt.length];

		for (int i = 0; i < atoms.length; i++) {
			atoms[i] = new SerialAtom(fmt[i]);
		}

		return atoms;
	}

	public final int type;

	public int i;

	public short n;

	public long x;

	public String s;

	public String[] as;

	public int[] ai;

	public short[] an;

	public byte y;

	public boolean b;

	public byte[] ay;

	public boolean[] ab;

	private SerialAtom(int type) {
		this.type = type;
	}

}
