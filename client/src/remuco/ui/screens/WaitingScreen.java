package remuco.ui.screens;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Item;

public final class WaitingScreen extends Form {

	public static final Command CMD_CANCEL = new Command("Cancel",
			Command.CANCEL, 0);

	private Gauge g;

	/**
	 * Creates a new waiting screen which has one commend ({@link #CMD_CANCEL}).
	 * The cancel can get removed by calling {@link #setCancable(boolean)}
	 * with the argument <code>false</code>.
	 */
	public WaitingScreen() {
		super("Remuco");

		this.addCommand(CMD_CANCEL);

		g = new Gauge("", false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING);
		g.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_VCENTER);
		g.setLabel(" ");

		append(g);
	}

	public void setCancable(boolean c) {

		removeCommand(CMD_CANCEL);

		if (c)
			addCommand(CMD_CANCEL);
	}

	public void setMessage(String m) {
		g.setLabel(m);
	}

}
