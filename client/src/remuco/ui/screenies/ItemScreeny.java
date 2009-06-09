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
import remuco.player.Progress;
import remuco.ui.IActionListener;
import remuco.ui.KeyBindings;
import remuco.ui.Theme;

public final class ItemScreeny extends Screeny {

	private final TitleScreeny screenyDesc;

	private final ButtonScreeny screenyNext, screenyPrev;

	private final ProgressScreeny screenyProgress;

	private final RateScreeny screenyRate;

	public ItemScreeny(PlayerInfo player) {

		super(player);

		screenyDesc = new TitleScreeny(player);
		screenyProgress = new ProgressScreeny(player);
		screenyRate = new RateScreeny(player);
		screenyNext = new ButtonScreeny(player, Theme.RTE_NEXT,
				KeyBindings.ACTION_NEXT);
		screenyPrev = new ButtonScreeny(player, Theme.RTE_PREV,
				KeyBindings.ACTION_PREV);

	}

	public void pointerPressed(int px, int py, IActionListener actionListener) {
		final int rx = px - getPreviousX();
		final int ry = py - getPreviousY();
		screenyDesc.pointerPressed(rx, ry, actionListener);
		screenyRate.pointerPressed(rx, ry, actionListener);
		screenyNext.pointerPressed(rx, ry, actionListener);
		screenyPrev.pointerPressed(rx, ry, actionListener);
	}

	public void pointerReleased(int px, int py, IActionListener actionListener) {
		final int rx = px - getPreviousX();
		final int ry = py - getPreviousY();
		screenyDesc.pointerReleased(rx, ry, actionListener);
		screenyRate.pointerReleased(rx, ry, actionListener);
		screenyNext.pointerReleased(rx, ry, actionListener);
		screenyPrev.pointerReleased(rx, ry, actionListener);
	}

	protected void dataUpdated() {

		if (data instanceof Progress) {
			screenyProgress.updateData(data);
		} else { // instance of Item
			screenyDesc.updateData(data);
			screenyRate.updateData(data);
		}
	}

	protected void initRepresentation() throws ScreenyException {

		setImage(Image.createImage(width, height)); // occupy available space

		final int clip[] = drawBorders(theme.getImg(Theme.RTE_ITEM_BORDER_NW),
			theme.getImg(Theme.RTE_ITEM_BORDER_N),
			theme.getImg(Theme.RTE_ITEM_BORDER_NE),
			theme.getImg(Theme.RTE_ITEM_BORDER_W),
			theme.getImg(Theme.RTE_ITEM_BORDER_E),
			theme.getImg(Theme.RTE_ITEM_BORDER_SW),
			theme.getImg(Theme.RTE_ITEM_BORDER_S),
			theme.getImg(Theme.RTE_ITEM_BORDER_SE),
			theme.getColor(Theme.RTC_BG_ITEM));

		// fill clip with item background color

		final int xClip = clip[0];
		final int yClip = clip[1];
		final int wClip = clip[2];
		final int hClip = clip[3];

		g.setColor(theme.getColor(Theme.RTC_BG_ITEM));
		g.fillRect(xClip, yClip, wClip, hClip);

		// sub screenies

		int x, y, w, h;

		x = xClip;
		y = yClip + hClip;
		h = hClip / 3; // max 1/3 for next/prev buttons
		screenyPrev.initRepresentation(x, y, BOTTOM_LEFT, wClip, h);

		x = xClip + wClip;
		y = yClip + hClip;
		h = hClip / 3; // max 1/3 for next/prev buttons
		screenyNext.initRepresentation(x, y, BOTTOM_RIGHT, wClip, h);

		x = xClip + wClip / 2;
		y = yClip + hClip;
		h = hClip / 3; // max 1/3 for rating
		w = screenyNext.getPreviousX() - screenyPrev.getNextX();
		screenyRate.initRepresentation(x, y, BOTTOM_CENTER, w, h);

		x = screenyPrev.getNextX();
		y = screenyRate.getPreviousY();
		w = screenyNext.getPreviousX() - screenyPrev.getNextX();
		h = 2 * hClip / 3; // all available space, screeny sets height as needed
		screenyProgress.initRepresentation(x, y, BOTTOM_LEFT, w, h);

		x = xClip;
		y = Math.min(screenyProgress.getPreviousY(), screenyPrev.getPreviousY());
		h = y - yClip;
		screenyDesc.initRepresentation(x, y, BOTTOM_LEFT, wClip, h);

	}

	protected void updateRepresentation() {

		screenyDesc.draw(g);
		screenyProgress.draw(g);
		screenyRate.draw(g);
		screenyPrev.draw(g);
		screenyNext.draw(g);
	}

}
