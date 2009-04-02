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

import remuco.player.Item;
import remuco.player.PlayerInfo;
import remuco.player.Progress;
import remuco.ui.Theme;

public final class ItemScreeny extends Screeny {

	/**
	 * Object to use for {@link #updateData(Object)} when this screeny shall
	 * toggle whether to display the image of the current item (given by a
	 * previous call to {@link #updateData(Object)}) as fullscreen.
	 */
	public static final Object ToogleImageFullScreen = new Object();

	private boolean fullScreenImage = false;

	private ImageScreeny screenyImage, screenyImageClear;

	private TitleScreeny screenyPlobDesc;

	private ProgressScreeny screenyProgress;

	private RateScreeny screenyRate;

	public ItemScreeny(PlayerInfo player) {

		super(player);

		screenyPlobDesc = new TitleScreeny(player);
		screenyProgress = new ProgressScreeny(player);
		screenyRate = new RateScreeny(player);
		screenyImage = new ImageScreeny(player);
		screenyImageClear = new ImageScreeny(player);

	}

	protected void dataUpdated() {

		if (data == ToogleImageFullScreen) {

			fullScreenImage = fullScreenImage ? false : true;

		} else if (data instanceof Progress) {
			
			screenyProgress.updateData(data);
			
		} else { // instance of Item

			screenyImage.updateData(data != null ? ((Item) data).getImg()
					: null);
			screenyPlobDesc.updateData(data);
			screenyRate.updateData(data);

			fullScreenImage = false;

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

		int x, y, h;

		x = wClip / 2 + xClip;
		y = yClip + hClip;
		h = hClip / 3; // max 1/3 for rating
		screenyRate.initRepresentation(x, y, BOTTOM_CENTER, wClip, h);

		x = xClip;
		y = screenyRate.getPreviousY();
		h = 2 * hClip / 3; // all available space, screeny sets height as needed
		screenyProgress.initRepresentation(x, y, BOTTOM_LEFT, wClip, h);

		x = xClip;
		y = screenyProgress.getPreviousY();
		h = y - yClip;
		screenyPlobDesc.initRepresentation(x, y, BOTTOM_LEFT, wClip, h);

		x = xClip;
		y = yClip;
		h = hClip;
		screenyImage.initRepresentation(x, y, TOP_LEFT, wClip, h);
		screenyImageClear.initRepresentation(x, y, TOP_LEFT, wClip, h);

	}

	protected void updateRepresentation() {

		if (fullScreenImage) {

			screenyImage.draw(g);

		} else {

			screenyImageClear.draw(g); // removes any item images artefacts
			screenyPlobDesc.draw(g);
			screenyProgress.draw(g);
			screenyRate.draw(g);

		}

	}

}
