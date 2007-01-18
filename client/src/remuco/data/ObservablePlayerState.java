package remuco.data;

import java.util.Vector;


/**
 * Extends the standard PlayerState by methods to register observer for a player
 * state. Whenever the player state get's changed, the {@link #changed()} method
 * must be called. This will notify all observers about the change. The
 * notification happens in an own thread.
 * 
 * @author Christian Buennig
 * 
 */
public class ObservablePlayerState extends PlayerState implements Runnable {

    private Vector observer;

    public ObservablePlayerState() {
        super();
        observer = new Vector();
        new Thread(this).start();
    }

    public synchronized void addObserver(IPlayerStateObserver pso) {
        observer.addElement(pso);
    }

    /**
     * This will notify all registered {@link IPlayerStateObserver} that this
     * player state has changed. The notification process happens in an extra
     * thread, so this method will return immediatly.
     * 
     */
    public synchronized void changed() {
        this.notify();
    }

    public synchronized void removeObserver(IPlayerStateObserver pso) {
        observer.removeElement(pso);
    }

    public void run() {
        int n;
        IPlayerStateObserver pso;
        while (true) {
            synchronized (this) {
                try {
                    this.wait();
                    n = observer.size();
                    for (int i = 0; i < n; i++) {
                        pso = (IPlayerStateObserver) observer.elementAt(i);
                        pso.notifyPlayerStateChange();
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

}
