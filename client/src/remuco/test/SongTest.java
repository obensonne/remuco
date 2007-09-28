package remuco.test;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import remuco.connection.RemotePlayer;
import remuco.data.ObservablePlayerState;
import remuco.server.VirutalPlayer;
import remuco.ui.canvas.MainScreen;

public class SongTest extends MIDlet implements CommandListener {

	protected void destroyApp(boolean unconditional)
			throws MIDletStateChangeException {
		// TODO Auto-generated method stub

	}

	protected void pauseApp() {
		// TODO Auto-generated method stub

	}

	private Display display;

	protected void startApp() throws MIDletStateChangeException {
		display = Display.getDisplay(this);

		MainScreen ms = new MainScreen();
		ms.setUp(this, display, null);

		display.setCurrent(ms.getDisplayable());
	}

	public void commandAction(Command arg0, Displayable arg1) {
		// TODO Auto-generated method stub
		
	}

}
