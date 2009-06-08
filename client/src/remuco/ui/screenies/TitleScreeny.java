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
import remuco.ui.IActionListener;
import remuco.ui.KeyBindings;
import remuco.ui.Theme;

/**
 * A screeny for meta information like title, artist, album and for an image of
 * an {@link Item}.
 * 
 * @author Oben Sonne
 * 
 */
public final class TitleScreeny extends Screeny {

	private final Item noPlob;

	private int colorBg, colorArtist, colorAlbum, colorTitle; // colorText;

	public TitleScreeny(PlayerInfo player) {

		super(player);

		noPlob = new Item();

		data = noPlob;

	}

	public void pointerPressed(int px, int py, IActionListener actionListener) {
		if (!isInScreeny(px, py)) {
			return;
		}
		actionListener.handleActionPressed(KeyBindings.ACTION_IMAGE);
	}

	protected void initRepresentation() throws ScreenyException {

		setImage(Image.createImage(width, height)); // occupy available space

		colorAlbum = theme.getColor(Theme.RTC_TEXT_ALBUM);
		colorArtist = theme.getColor(Theme.RTC_TEXT_ARTIST);
		colorBg = theme.getColor(Theme.RTC_BG_ITEM);
		// colorText = theme.getColor(Theme.RTC_TEXT_OTHER);
		colorTitle = theme.getColor(Theme.RTC_TEXT_TITLE);

		noPlob.setMeta(Item.META_TITLE, player.getName());
	}

	protected void updateRepresentation() {

		// clear

		g.setColor(colorBg);
		g.fillRect(0, 0, width, height);

		final Item item = data == null ? noPlob : (Item) data;

		String[] sa;
		int y = 0;

		if (item.hasAbstract()) { // only basic item info available

			g.setColor(colorArtist);
			g.setFont(Theme.FONT_ARTIST);
			sa = Theme.splitString(item.getMeta(Item.META_ABSTRACT), width,
				Theme.FONT_ARTIST);
			y = (height - sa.length * Theme.FONT_ARTIST.getHeight()) / 2;
			y = drawStrings(sa, width, y);

			return;
		} // else: full item info available

		// //// artist //////

		g.setColor(colorArtist);
		g.setFont(Theme.FONT_ARTIST);
		sa = Theme.splitString(item.getMeta(Item.META_ARTIST), width,
			Theme.FONT_ARTIST);
		y = drawStrings(sa, width, y);

		// //// title //////

		g.setColor(colorTitle);
		g.setFont(Theme.FONT_TITLE);
		sa = Theme.splitString(item.getMeta(Item.META_TITLE), width,
			Theme.FONT_TITLE);
		y = drawStrings(sa, width, y);

		// //// album //////

		g.setColor(colorAlbum);
		g.setFont(Theme.FONT_ALBUM);
		sa = Theme.splitString(item.getMeta(Item.META_ALBUM), width,
			Theme.FONT_ALBUM);
		y = drawStrings(sa, width, y);

		// //// genre, length, year and image //////

		y += Theme.FONT_SMALL.getHeight() / 2;

		final int maxImgHeight = height - y;
		if (maxImgHeight < 32) // not enough space for an image
			return;

		Image plobImg = item.getImg();

		if (plobImg == null) {
			// The logo should not get scaled as this drops the alpha channel.
			// Get a logo image that fits into the available space:
			plobImg = theme.getLogo(maxImgHeight);
		} else {
			plobImg = Theme.shrinkImageIfNeeded(plobImg, width, maxImgHeight);
		}

		g.drawImage(plobImg, width / 2, y + maxImgHeight / 2, Graphics.HCENTER
				| Graphics.VCENTER);

	}

	private int drawStrings(String[] sa, int width, int y) {

		final int fontHeight = g.getFont().getHeight();

		for (int i = 0; i < sa.length; i++) {
			g.drawString(sa[i], width / 2, y, Graphics.TOP | Graphics.HCENTER);
			y += fontHeight;
		}

		return y;
	}
}
