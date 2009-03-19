package remuco.ui.screens;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.StringItem;

import remuco.ui.CMD;
import remuco.ui.Theme;
import remuco.util.Log;

public class NotificationScreen extends Form implements CommandListener {

	private final Display display;

	private final StringItem msgItem;

	private Displayable next = null;

	/**
	 * Create new silent alert.
	 * <p>
	 * <em>Note:</em> Use {@link #show(Displayable)} instead of
	 * {@link Display#setCurrent(Displayable)} to show this notification
	 * (otherwise it will be on display forever).
	 * 
	 * 
	 * @param title
	 */
	public NotificationScreen(Display display, String title, Image icon) {

		super(title);
		this.display = display;

		append(Theme.pseudoStretch(icon, getWidth()));

		msgItem = new StringItem(null, "");
		msgItem.setLayout(Item.LAYOUT_CENTER | Item.LAYOUT_SHRINK);
		append(msgItem);

		addCommand(CMD.OK);
		setCommandListener(this);
	}

	public void commandAction(Command c, Displayable d) {

		if (c == CMD.OK) {
			if (next != null) {
				display.setCurrent(next);
			} else {
				Log.bug("Mar 18, 2009.21:19:58 PM");
			}
		} else {
			Log.bug("Mar 18, 2009.21:19:45 PM");
		}
	}

	/** Set the notification message. */
	public void setMessage(String msg) {
		msgItem.setText(msg);
	}

	/** Show this notification, subsequent show <em>next</em>. */
	public void show(Displayable next) {

		this.next = next;
		display.setCurrent(this);

	}

}
