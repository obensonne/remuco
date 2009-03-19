package remuco.ui;

import java.util.TimerTask;

import remuco.player.Player;
import remuco.util.Log;

/**
 * Timer task to periodically do a player control (intended for controls
 * triggered by holding down a key).
 */
public class RepeatedControl extends TimerTask {

	public static final int SEEK = 1;

	public static final int VOLUME = 0;

	private final int direction;

	private boolean neverRun = true;

	private final Player player;

	private final int type;

	/**
	 * Create a new repeated control task.
	 * 
	 * @param type
	 *            either {@link #SEEK} or {@link #VOLUME}
	 * @param player
	 *            the player to use to control
	 * @param direction
	 *            either <code>-1</code> or <code>1</code> for seeking
	 *            backward/forward respectively adjusting volume down/up
	 */
	public RepeatedControl(int type, Player player, int direction) {
		this.type = type;
		this.player = player;
		this.direction = direction;
	}

	/**
	 * Same as {@link TimerTask#cancel()}, only differs in return value.
	 * 
	 * @return <code>true</code> if the task has never run, <code>false</code>
	 *         if it has run one or more times
	 */
	public boolean cancel() {

		return super.cancel() & neverRun; // == neverRun for repeated tasks
	}

	public void run() {

		// actually we have to synchronize here to be perfect, but we do not
		// need to be perfect
		neverRun = false;

		switch (type) {
		case VOLUME:
			player.ctrlVolume(direction);
			break;
		case SEEK:
			player.ctrlSeek(direction);
			break;
		default:
			Log.bug("Mar 17, 2009.9:11:31 PM");
			break;
		}

	}

}
