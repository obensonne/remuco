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
package remuco.client.jme.ui.screenies;

import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.lcdui.Image;

import remuco.client.common.data.PlayerInfo;
import remuco.client.common.player.Feature;
import remuco.client.common.util.Log;
import remuco.client.jme.Config;
import remuco.client.jme.ui.IActionListener;
import remuco.client.jme.ui.KeyBindings;
import remuco.client.jme.ui.Theme;

/**
 * A container screeny to group touchscreen specific buttons.
 */
public class ButtonBarScreeny extends Screeny {

	private final Vector buttons;

	public ButtonBarScreeny(PlayerInfo player) {
		super(player);
		buttons = new Vector();
		buttons.addElement(new ButtonScreeny(player, Theme.RTE_BUTTON_PREV,
				KeyBindings.ACTION_PREV));
		if (player.supports(Feature.CTRL_FULLSCREEN)) {
			buttons.addElement(new ButtonScreeny(player,
					Theme.RTE_BUTTON_FULLSCREEN, KeyBindings.ACTION_FULLSCREEN));
		}
		if (player.supports(Feature.CTRL_RATE)) {
			buttons.addElement(new ButtonScreeny(player,
					Theme.RTE_BUTTON_RATE, KeyBindings.ACTION_RATEUP));
		}
		if (player.supports(Feature.CTRL_TAG)) {
			buttons.addElement(new ButtonScreeny(player, Theme.RTE_BUTTON_TAGS,
					KeyBindings.ACTION_EDITTAGS));
		}
		buttons.addElement(new ButtonScreeny(player, Theme.RTE_BUTTON_NEXT,
				KeyBindings.ACTION_NEXT));
	}

	public void pointerPressed(int px, int py, IActionListener actionListener) {
		if (!isInScreeny(px, py)) {
			return;
		}
		final int rx = px - getPreviousX();
		final int ry = py - getPreviousY();
		final Enumeration e = buttons.elements();
		while (e.hasMoreElements()) {
			((Screeny) e.nextElement()).pointerPressed(rx, ry, actionListener);
		}
	}

	public void pointerReleased(int px, int py, IActionListener actionListener) {
		final int rx = px - getPreviousX();
		final int ry = py - getPreviousY();
		final Enumeration e = buttons.elements();
		while (e.hasMoreElements()) {
			((Screeny) e.nextElement()).pointerReleased(rx, ry, actionListener);
		}
	}

	protected void initRepresentation() throws ScreenyException {
		
		if (!Config.TOUCHSCREEN) {
			setImage(INVISIBLE);
			return;
		}

		final Image imgLeft = theme.getImg(Theme.RTE_BUTTONBAR_LEFT);
		final Image imgSpacer = theme.getImg(Theme.RTE_BUTTONBAR_SPACER);
		final Image imgRight = theme.getImg(Theme.RTE_BUTTONBAR_RIGHT);

		final int h = imgLeft.getHeight();

		setImage(Image.createImage(width, h));

		// initially fill all with spacer
		for (int x = 0; x < width; x++) {
			g.drawImage(imgSpacer, x, 0, TOP_LEFT);
		}

		// draw left border image
		g.drawImage(imgLeft, 0, 0, TOP_LEFT);

		// intermediately init buttons (need to know width)
		final Enumeration e = buttons.elements();
		while (e.hasMoreElements()) {
			Screeny button = (Screeny) e.nextElement();
			button.initRepresentation(0, 0, TOP_LEFT, width, h);
		}

		final int xEnd = width - imgRight.getWidth();
		int x = imgLeft.getWidth();

		// gaps between buttons
		final int gaps[] = theme.calculateGaps(xEnd - x, buttons);

		// draw buttons
		Screeny previous = (Screeny) buttons.firstElement();
		previous.initRepresentation(x, 0, TOP_LEFT, xEnd - x, height);
		for (int i = 1; i < buttons.size(); i++) {
			x = previous.getNextX() + gaps[i - 1];
			Screeny current = (Screeny) buttons.elementAt(i);
			current.initRepresentation(x, 0, TOP_LEFT, xEnd - x, height);
			previous = current;
		}

		// draw right border image
		x = previous.getNextX();
		g.drawImage(imgRight, x, 0, TOP_LEFT);
		x += imgLeft.getWidth();

		if (x != width) {
			Log.bug("Bug in BS: " + x + "!=" + width);
		}
	}

	protected void updateRepresentation() {

		final Enumeration e = buttons.elements();
		while (e.hasMoreElements()) {
			((Screeny) e.nextElement()).draw(g);
		}
	}

}
