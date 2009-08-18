package remuco;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import remuco.util.Log;

public class Entry extends MIDlet {

	private Remuco remuco;
	
	protected void destroyApp(boolean unconditional)
			throws MIDletStateChangeException {
		
		remuco.destroy();
		remuco = null;
		
		MainLoop.disable();

	}
	
	protected void pauseApp() {
		
		Log.ln("[EN] paused");
		
	}

	protected void startApp() throws MIDletStateChangeException {
		
		MainLoop.enable();
		
		if (remuco == null) {
			remuco = new Remuco(this);
			Log.ln("[EN] started");
		} else {
			Log.ln("[EN] resumed");			
		}

	}
	
}
