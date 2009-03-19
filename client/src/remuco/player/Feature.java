package remuco.player;

public class Feature {

	// --- 'is known' features ---

	public static final int KNOWN_VOLUME = 1 << 0;
	public static final int KNOWN_REPEAT = 1 << 1;
	public static final int KNOWN_SHUFFLE = 1 << 2;
	public static final int KNOWN_PLAYBACK = 1 << 3;
	public static final int KNOWN_PROGRESS = 1 << 4;

	// --- misc control features ---

	public static final int CTRL_PLAYBACK = 1 << 9;
	public static final int CTRL_VOLUME = 1 << 10;
	public static final int CTRL_SEEK = 1 << 11;
	public static final int CTRL_TAG = 1 << 12;
	public static final int CTRL_CLEAR_PL = 1 << 13;
	public static final int CTRL_CLEAR_QU = 1 << 14;
	public static final int CTRL_RATE = 1 << 15;
	public static final int CTRL_REPEAT = 1 << 16;
	public static final int CTRL_SHUFFLE = 1 << 17;
	public static final int CTRL_NEXT = 1 << 18;
	public static final int CTRL_PREV = 1 << 19;
	public static final int CTRL_FULLSCREEN = 1 << 20;
	        
	// --- request features ---

	public static final int REQ_ITEM = 1 << 25;
	public static final int REQ_PL = 1 << 26;
	public static final int REQ_QU = 1 << 27;
	public static final int REQ_MLIB = 1 << 28;

	public static final int SHUTDOWN = 1 << 30;

}
