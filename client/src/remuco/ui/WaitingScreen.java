package remuco.ui;

import javax.microedition.lcdui.Form;

public class WaitingScreen extends Form {

	public WaitingScreen() {
		super("Remuco");

		append(" ");
	}

	public void setMessage(String m) {
		delete(0);
		append(m);
	}

}
