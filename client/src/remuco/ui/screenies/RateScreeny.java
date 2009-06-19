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

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import remuco.player.Item;
import remuco.player.PlayerInfo;
import remuco.ui.Theme;

/**
 * A screeny to display the rating of an item.
 */
public final class RateScreeny extends Screeny {

	private int rating;

	private int ratingMax;

	private Image ratingOn, ratingOff;

	private int ratingWidth, ratingHeight;

	public RateScreeny(PlayerInfo player) {
		super(player);
	}

	protected void dataUpdated() {

		Item item = (Item) data;

		if (item == null)
			rating = player.getMaxRating();
		else
			rating = item.getRating();

	}

	protected void initRepresentation() throws ScreenyException {

		ratingMax = player.getMaxRating();

		if (ratingMax == 0) {
			setImage(INVISIBLE);
			return;
		}

		ratingOn = theme.getImg(Theme.RTE_ICON_RATING_ON);
		ratingOff = theme.getImg(Theme.RTE_ICON_RATING_OFF);

		// scale rate images if needed (may happen if the remote player has a
		// wide rating range)
		ratingOn = Theme.shrinkImageIfNeeded(ratingOn, width / ratingMax,
			height);
		ratingOff = Theme.shrinkImageIfNeeded(ratingOff, width / ratingMax,
			height);

		ratingWidth = ratingOn.getWidth();
		ratingHeight = ratingOn.getHeight();

		setImage(Image.createImage(ratingMax * ratingWidth, ratingHeight));

		g.setColor(theme.getColor(Theme.RTC_BG));
		g.fillRect(0, 0, width, height);

	}

	protected void updateRepresentation() {

		int x;

		x = 0;
		for (int j = 1; j <= rating; j++) {
			g.drawImage(ratingOn, x, 0, Graphics.LEFT | Graphics.TOP);
			x += ratingWidth;
		}
		for (int j = rating + 1; j <= ratingMax; j++) {
			g.drawImage(ratingOff, x, 0, Graphics.LEFT | Graphics.TOP);
			x += ratingWidth;
		}

	}

}
