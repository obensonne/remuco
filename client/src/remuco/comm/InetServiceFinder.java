package remuco.comm;

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import remuco.Remuco;
import remuco.UserException;

public final class InetServiceFinder implements IServiceFinder {

	public static final String PORT = "34271";

	private final Object lock;

	private TimerTask notifier = null;

	private final Timer timer;

	public InetServiceFinder() {

		lock = new Object();

		timer = Remuco.getGlobalTimer();

	}

	public void cancelServiceSearch() {

		synchronized (lock) {
			if (notifier != null) {
				notifier.cancel();
				notifier = null;
			}
		}

	}

	public void findServices(String addr, final IServiceListener listener)
			throws UserException {

		final String url;

		if (addr.indexOf(':') >= 0)
			url = "socket://" + addr;
		else
			url = "socket://" + addr + ":" + PORT;

		final Hashtable services = new Hashtable(1);
		services.put("Player", url);
		
		synchronized (lock) {

			if (notifier != null) {
				return;
			}
			
			notifier = new TimerTask() {
				public void run() {
					listener.notifyServices(services, null);
				}
			};
		}

		// fake a service search:
		timer.schedule(notifier, 1000);

	}

}
