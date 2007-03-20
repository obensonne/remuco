package remuco.test;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.midlet.MIDlet;

import remuco.ui.canvas.MainScreen;

public class Cat extends MIDlet implements CommandListener {

	Display display;

	Command exit;

	MainScreen sc;
	
	public Cat() {
		display = Display.getDisplay(this);
		sc = new MainScreen();
	}

	public void commandAction(Command c, Displayable d) {
		notifyDestroyed();
	}

	public void destroyApp(boolean unconditional) {
	}

	public void pauseApp() {
		System.out.println("App paused.");
	}

	public void startApp() {
		
		
		display = Display.getDisplay(this);

		exit = new Command("Exit", Command.STOP, 1);

//		Form f = new Form("Form");

//		SongItem si = new SongItem("hallo");

//		si.setSong("sdaf fdas afer");

//		f.append("Hallo");
//		f.append(si);
		// f.append(vl);

		sc.setUp(this, display, null);
		
		display.setCurrent(sc.getDisplayable());
		
		sc.setActive(true);
		// display.setCurrent(canvas);
	}
}