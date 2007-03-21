package remuco.ui;

import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Item;

public class WaitingScreen extends Form {

	private Gauge g;

	public WaitingScreen() {
		super("Remuco");

		g = new Gauge("", false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING);
		g.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_VCENTER);
		g.setLabel(" ");

		append(g);
	}

	public void setMessage(String m) {
		g.setLabel(m);
	}

}
