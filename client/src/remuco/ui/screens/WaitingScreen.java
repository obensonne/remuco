package remuco.ui.screens;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Item;

import remuco.ui.Theme;

public final class WaitingScreen extends Form {

	public static final Command CMD_CANCEL = new Command("Cancel",
			Command.STOP, 0);

	private final Gauge g;

	private Object property = null;

	/**
	 * Creates a new waiting screen which has one commend ({@link #CMD_CANCEL}).
	 * The cancel possibility can be disabled by calling
	 * {@link #setCancable(boolean)} with the argument <code>false</code>.
	 */
	public WaitingScreen() {
		super("Remuco");

		this.addCommand(CMD_CANCEL);

		g = new Gauge("", false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING);
		g.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_VCENTER);
		g.setLabel(" ");

		append(g);

	}

	/** Attach a property to this screen. */
	public void attachProperty(Object property) {
		this.property = property;
	}

	/**
	 * Detach the property attached to this screen.
	 * 
	 * @return the detached property or <code>null</code> if there is no
	 *         attached property
	 */
	public Object detachProperty() {
		return property;
	}

	public void setCancable(boolean c) {

		removeCommand(CMD_CANCEL);

		if (c)
			addCommand(CMD_CANCEL);
	}

	public void setImage(Image img) {
		deleteAll();
		append(Theme.pseudoStretch(img, getWidth()));
		append(g);
	}

	/**
	 * Set the waiting message (gauge label).
	 * 
	 * @param m
	 *            the message
	 */
	public void setMessage(String m) {
		g.setLabel(m);
	}

}
