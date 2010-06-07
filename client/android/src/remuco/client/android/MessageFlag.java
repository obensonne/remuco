package remuco.client.android;

public abstract class MessageFlag {

	public static final int ITEM_CHANGED = 10;
	public static final int PROGRESS_CHANGED = 11;
	public static final int STATE_CHANGED = 12;
	
	public static final int CONNECTED = 1;
	public static final int DISCONNECTED = 2;
	
	public static final int TICK = 20;
	
	// control messages
	public static final int CTRL_PLAY_PAUSE 	= 30;
	public static final int CTRL_PREV			= 31;
	public static final int CTRL_NEXT			= 32;
	public static final int CTRL_VOLUME_UP		= 33;
	public static final int CTRL_VOLUME_DOWN	= 34;
	public static final int CTRL_RATE 			= 35;
	
	
}
