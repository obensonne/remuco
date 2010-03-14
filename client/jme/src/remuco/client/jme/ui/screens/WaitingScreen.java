/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2010 by the Remuco team, see AUTHORS.
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
package remuco.client.jme.ui.screens;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.Item;

import remuco.client.jme.ui.Theme;

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
		final Object p = property;
		property = null;
		return p;
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
