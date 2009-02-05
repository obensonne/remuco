package remuco.ui;

import remuco.player.Player;
import remuco.util.Log;

public final class Adjuster implements Runnable {

	public static final int PROGRESS = 1;

	public static final int VOLUME = 0;

	private static final int ADJUST_INTERVAL = 248;

	/**
	 * Specifies what the controller currently is supposed to do:
	 * <ul>
	 * <li>= 0 : adjusting is disabled
	 * <li>&lt; 0 : currently adjusting up/right
	 * <li>&gt; 0 : currently adjusting down/left
	 * </ul>
	 * Gets controlled via {@link #startAdjust(boolean)} and
	 * {@link #stopAdjust()}.
	 */
	private int adjusting = 0;

	private boolean interrupted = false;

	private final Player player;

	private final Thread thread;

	private final int type;

	private int delay;

	/**
	 * Create a new slider controller. Automatically starts a new thread.
	 * 
	 * @param player
	 * @param type
	 *            one of {@link #VOLUME} or {@link #PROGRESS}
	 */
	public Adjuster(Player player, int type) {

		this.player = player;
		this.type = type;

		thread = new Thread(this);
		thread.start();
	}

	/**
	 * Irrevocably interrupt the controller thread. Use {@link #stopAdjust()} to
	 * stop a current adjustment.
	 */
	public void interrupt() {

		interrupted = true;
		adjusting = 0;

		if (thread.isAlive())
			thread.interrupt();
	}

	public void run() {

		delay = 0;

		while (!interrupted) {

			//Log.debug("adj waiting..");

			synchronized (thread) {
				try {
					thread.wait();
				} catch (InterruptedException e) {
					continue;
				}
			}

			//Log.debug("adj woke up..");

			if (delay > 0) {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e1) {
					// nothing to do
				}
			}

			delay = 0;

			/*
			 * When stopAdjust() gets called now, it signals we're already doing
			 * repeating/adjusting actions (returns false), but in fact we did
			 * not adjust yet and we won't adjust anymore since 'adjusting' is
			 * now set to 0. This means the startAdjust() call had no effect,
			 * neither a one-time nor a repeating action. Stuff it ..
			 */

			//Log.debug("adj delay end");

			while (adjusting != 0) {

				switch (type) {
				case VOLUME:
					if (adjusting > 0) {
						player.ctrlVolumeUp();
					} else {
						player.ctrlVolumeDown();
					}
					break;
				case PROGRESS:
					if (adjusting > 0) {
						player.ctrlSeekFwd();
					} else {
						player.ctrlSeekBwd();
					}
					break;
				default:
					Log.bug("Jan 30, 2009.12:45:38 AM");
					adjusting = 0;
					break;
				}

				try {
					Thread.sleep(ADJUST_INTERVAL);
				} catch (InterruptedException e) {
					continue;
				}
			}
		}
	}

	/**
	 * Start a new adjustment. Use {@link #stopAdjust()} to stop it.
	 * 
	 * @param up
	 *            if <code>true</code>, adjust up/right, otherwise adjust
	 *            down/left
	 * @param delay
	 *            millis to wait before starting adjustment
	 */
	public void startAdjust(boolean up, int delay) {

		if (adjusting != 0)
			return;

		adjusting = up ? 1 : -1;

		synchronized (thread) {
			this.delay = delay;
			thread.notify();
		}

	}

	/**
	 * Stop adjustment.
	 * 
	 * @return <code>true</code> if start was still in delay, <code>false</code>
	 *         otherwise
	 */
	public boolean stopAdjust() {

		adjusting = 0;

		synchronized (thread) {
			if (delay > 0) // start still in delay
				return true;
			else
				return false;
		}

	}

}
