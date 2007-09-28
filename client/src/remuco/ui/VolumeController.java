package remuco.ui;

import remuco.player.Player;
import remuco.util.Tools;

public final class VolumeController extends Thread {

	private int adjusting = 0;

	private boolean interrupted = false;

	private final Object mutex;

	private final Player player;

	protected VolumeController(Player player) {

		mutex = new Object();

		this.player = player;

	}

	public void interrupt() {
		super.interrupt();
		interrupted = true;
	}

	public void run() {

		int val_current, val_new;
		
		while (!interrupted) {

			synchronized (mutex) {
				try {
					mutex.wait();
				} catch (InterruptedException e) {
					continue;
				}
			}

			val_current = player.state.getVolume();
			val_new = val_current;

			while (adjusting != 0) {

				val_new += adjusting;

				if (val_new >= 100 || val_new <= 0) {
					val_new = adjusting > 0 ? 100 : 0;
					adjusting = 0;
				}

				player.ctrlVolume(val_new);

				Tools.sleep(200);

			}
		}
	}

	public synchronized void startVolumeAdjustment(boolean up) {

		if (adjusting != 0)
			return;
		
		adjusting = up ? 5 : -5;

		synchronized (mutex) {
			mutex.notify();
		}

	}

	public synchronized void stopVolumeAdjustment() {

		adjusting = 0;

	}

}
