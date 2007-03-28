package remuco.ui.canvas;

public interface IKeyListener {

	
	public static final int
		KEY_PLAY_PAUSE = 0,
		KEY_STOP = 1,
		KEY_RESTART = 2,
		KEY_NEXT = 3,
		KEY_PREV = 4,
		KEY_VOLUME_UP = 5,
		KEY_VOLUME_DOWN = 6,
		KEY_SHOW_PLAYLIST = 7,
		KEY_RATE_UP = 8,
		KEY_RATE_DOWN = 9,
		KEY_NOOP = 99;
	
	public void keyPressed(int key);
	
	public void keyReleased(int key);
	
}
