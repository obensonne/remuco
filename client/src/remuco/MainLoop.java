package remuco;

import java.util.Timer;
import java.util.TimerTask;

import remuco.comm.Connection;
import remuco.util.Log;

/**
 * Global main loop which is used for
 * <ul>
 * <li>handling events from a {@link Connection} (decoupled from the
 * {@link Connection}'s receiver thread and from the threads calling methods on
 * a {@link Connection}),
 * <li>scheduling repetitive tasks,
 * <li>scheduling delayed tasks and
 * <li>synchronizing access to objects where critical race conditions may occur.
 * </ul>
 * <p>
 * <em>Synchronization note:</em> As the management of this class, i.e. calling
 * the methods {@link #enable()} and {@link #disable()}, is done only by the
 * MIDlet class, there is no need to synchronize anything. Of course this
 * requires that the schedule methods here may only be used from a non-static
 * context to be sure it has been initialized appropriately.
 */
public class MainLoop {

	/** Timer task to periodically log that the main loop is alive. */
	private static class AliveLogger extends TimerTask {

		private final long startTime = System.currentTimeMillis();

		public void run() {
			final long now = System.currentTimeMillis();
			final long seconds = (now - startTime) / 1000;
			Log.ln("main loop alive (" + seconds + ")");
		}

	}

	private static Timer timer;

	/** See {@link Timer#schedule(TimerTask, long)} with 10ms delay. */
	public static void schedule(TimerTask task) {

		timer.schedule(task, 10);
	}

	/** See {@link Timer#schedule(TimerTask, long)}. */
	public static void schedule(TimerTask task, long delay) {

		timer.schedule(task, delay);

	}

	/** See {@link Timer#schedule(TimerTask, long, long)}. */
	public static void schedule(TimerTask task, long delay, long period) {

		timer.schedule(task, delay, period);

	}

	/** Disable the main loop. Does nothing if the loop is already disabled. */
	protected static void disable() {

		if (timer != null) {
			timer.cancel();
			timer = null;
		}

	}

	/** Enable the main loop. Does nothing if the loop is already enabled. */
	protected static void enable() {

		if (timer == null) {
			timer = new Timer();
			timer.schedule(new AliveLogger(), 60000, 60000);
		}
	}

}
