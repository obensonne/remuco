package remuco.ui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;

import remuco.connection.RemotePlayer;

public class DummyScreen implements IScreen {

	private Form f;
	
	private Display d;
	
	public Displayable getDisplayable() {
		return f;
	}

	public void setActive(boolean active) {
		d.setCurrent(f);
	}

	public void setUp(CommandListener pcl, Display d, RemotePlayer player) {
		
		this.d = d;
		
		f = new Form("Remuco");
		f.append("Error: Loading Userinterface failed!");
		
		f.addCommand(IScreen.CMD_DISPOSE);
		f.setCommandListener(pcl);

		
	}

	public void commandAction(Command c, Displayable d) {
	}

	public void notifyPlayerStateChange() {
	}

}
