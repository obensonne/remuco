package remuco.comm;

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import remuco.UserException;

public final class InetServiceFinder implements IServiceFinder {

	private class Notifier extends TimerTask {

		private final IServiceListener listener;

		private Notifier(IServiceListener listener) {
			this.listener = listener;
		}

		public void run() {
			synchronized (services) {
				listener.notifyServices(services, null);
			}
		}
	}

	public static final String PORT = "34271";

	private static final Timer HELPER_THREAD = new Timer();

	private Notifier notifier = null;

	private final Hashtable services;

	public InetServiceFinder() {

		services = new Hashtable(1);

	}

	public void cancelServiceSearch() {

		synchronized (services) {
			if (notifier != null) {
				notifier.cancel();
				notifier = null;
			}
		}

	}

	public void findServices(String addr, IServiceListener listener)
			throws UserException {

		final String url;

		if (addr.indexOf(':') >= 0)
			url = "socket://" + addr;
		else
			url = "socket://" + addr + ":" + PORT;

		synchronized (services) {
			if (notifier != null)
				return;
			notifier = new Notifier(listener);
		}

		services.put("Player", url);

		// fake a service search:
		HELPER_THREAD.schedule(notifier, 1000);
	}

}
