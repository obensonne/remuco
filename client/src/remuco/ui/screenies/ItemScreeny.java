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
package remuco.ui.screenies;

import javax.microedition.lcdui.Image;

import remuco.player.PlayerInfo;
import remuco.ui.IActionListener;
import remuco.ui.KeyBindings;
import remuco.ui.Theme;

/**
 * A container screeny to display information about an item.
 */
public final class ItemScreeny extends Screeny {

	private final TitleScreeny screenyDesc;

	private final RateScreeny screenyRate;

	public ItemScreeny(PlayerInfo player) {

		super(player);

		screenyDesc = new TitleScreeny(player);
		screenyRate = new RateScreeny(player);
	}

	public void pointerPressed(int px, int py, IActionListener actionListener) {
		if (!isInScreeny(px, py)) {
			return;
		}
		actionListener.handleActionPressed(KeyBindings.ACTION_IMAGE);
	}

	protected void dataUpdated() {

		screenyDesc.updateData(data);
		screenyRate.updateData(data);
	}

	protected void initRepresentation() throws ScreenyException {

		setImage(Image.createImage(width, height)); // occupy available space

		// fill clip with item background color

		g.setColor(theme.getColor(Theme.RTC_BG));
		g.fillRect(0, 0, width, height);

		// sub screenies

		int x, y, w, h;

		x = width / 2;
		y = height - Theme.LINE_GAP;
		h = height / 3; // max 1/3 for rating
		w = width;
		screenyRate.initRepresentation(x, y, BOTTOM_CENTER, w, h);

		x = 0;
		y = 0;
		h = screenyRate.getPreviousY();
		w = width;
		screenyDesc.initRepresentation(x, y, TOP_LEFT, w, h);

	}

	protected void updateRepresentation() {

		screenyDesc.draw(g);
		screenyRate.draw(g);
	}

}
