package remuco.data;

public interface IPlayerStateObserver {

    /**
     * Implementation hint:<br>
     * The calling PlayerState has the monitor on itself, so all operations on
     * the PlayerState within this method are already synchronized.
     * 
     */
    public void notifyPlayerStateChange();

}
