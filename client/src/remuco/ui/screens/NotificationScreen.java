/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2009 Oben Sonne <obensonne@googlemail.com>
 *
 *   This file is part of Remuco.
 *
 *   Remuco is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Remuco is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Remuco.  If not, see <http://www.gnu.org/licenses/>.
 *   
 */
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
