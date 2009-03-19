package remuco.ui.screens;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.Item;

import remuco.ui.Theme;

public final class WaitingScreen extends Form {

	public static final Command CMD_CANCEL = new Command("Cancel",
			Command.STOP, 0);

	private final Gauge g;

	private final ImageItem ii;

	private Object property = null;

	/**
	 * Creates a new waiting screen which has one command ({@link #CMD_CANCEL}).
	 */
	public WaitingScreen() {
		super("Remuco");

		this.addCommand(CMD_CANCEL);

		g = new Gauge("", false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING);
		g.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_VCENTER);
		g.setLabel(" ");

		final Theme theme = Theme.getInstance();

		ii = new ImageItem(null, theme.aicRefresh, Item.LAYOUT_CENTER
				| Item.LAYOUT_VCENTER | Item.LAYOUT_NEWLINE_AFTER, null);

		append(ii);
		append(g);

	}

	/**
	 * Attach a property to this waiting screen.
	 * <p>
	 * The idea is that this property is used as an indicator for what this
	 * waiting screen is waiting for.
	 * <p>
	 * Further, as this method and {@link #detachProperty()} are synchronized on
	 * this waiting screen, the property can be used for synchronous handling of
	 * a waiting state.
	 */
	public synchronized void attachProperty(Object property) {
		this.property = property;
	}

	/**
	 * Detach the property attached to this waiting screen.
	 * 
	 * @return the detached property or <code>null</code> if there is no
	 *         attached property
	 * 
	 * @see #attachProperty(Object)
	 */
	public synchronized Object detachProperty() {
		return property;
	}

	/** Set an image centered above the gauge. */
	public void setImage(Image img) {
		ii.setImage(img);
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
