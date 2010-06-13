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
package remuco.client.midp.ui.screens;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import remuco.client.midp.ui.IKeyListener;
import remuco.client.midp.ui.KeyBindings;
import remuco.client.midp.ui.Theme;

/** Screen to configure the binding for one specific key. */
public final class KeyBinderScreen extends Canvas {

	/**
	 * Just to block the soft keys which are always used for some menus and
	 * therefore not suited to be used as hot keys.
	 */
	private static final Command CMD_BLOCK1 = new Command("BLOCKED",
			Command.OK, 0);

	/**
	 * Just to block the soft keys which are always used for some menus and
	 * therefore not suited to be used as hot keys.
	 */
	private static final Command CMD_BLOCK2 = new Command("BLOCKED",
			Command.BACK, 0);

	/**
	 * Just to block the soft keys which are always used for some menus and
	 * therefore not suited to be used as hot keys.
	 */
	private static final Command CMD_BLOCK3 = new Command("BLOCKED",
			Command.BACK, 0);

	private static final Font FONT = Theme.FONT_SMALL;

	private int actionCode;

	private final IKeyListener kl;

	private final StringBuffer textHint, textCurrent;

	public KeyBinderScreen(IKeyListener kl) {

		this.kl = kl;

		textHint = new StringBuffer(70);
		textCurrent = new StringBuffer(70);

		addCommand(CMD_BLOCK1);
		addCommand(CMD_BLOCK2);
		addCommand(CMD_BLOCK3);

	}

	public void configure(int actionCode) {

		this.actionCode = actionCode;

	}

	protected void keyPressed(int keyCode) {
		kl.keyPressed(keyCode);
	}

	protected void paint(Graphics g) {

		// set up textHint

		textHint.delete(0, textHint.length());
		textHint.append("Please press the key for action '");
		textHint.append(KeyBindings.actionNames[actionCode]).append("'");

		textCurrent.delete(0, textCurrent.length());
		textCurrent.append("Currently ");

		final int key = KeyBindings.getInstance().getKeyForAction(actionCode);

		if (key == 0)
			textCurrent.append("no key is set.");
		else
			textCurrent.append("key ").append(getKeyName(key)).append(
					" is used.");

		// draw

		g.setColor(0);
		g.fillRect(0, 0, getWidth(), getHeight());

		g.setColor(0xFFCC00);

		String[] splitted;
		int y;
		
		splitted = Theme.splitString(textHint.toString(), getWidth(), FONT);
		y = drawStrings(g, splitted, getWidth(), getHeight() / 2
				- FONT.getHeight());
		splitted = Theme.splitString(textCurrent.toString(), getWidth(), FONT);
		y = drawStrings(g, splitted, getWidth(), y);

	}

	private int drawStrings(Graphics g, String[] sa, int width, int y) {

		final int fontHeight = FONT.getHeight();

		g.setFont(FONT);

		for (int i = 0; i < sa.length; i++) {
			g.drawString(sa[i], width / 2, y, Graphics.TOP | Graphics.HCENTER);
			y += fontHeight;
		}

		return y;
	}

}
