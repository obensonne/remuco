/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2009 by the Remuco team, see AUTHORS.
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

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import remuco.Config;
import remuco.OptionDescriptor;
import remuco.client.common.data.Item;
import remuco.client.common.data.PlayerInfo;
import remuco.client.jme.ui.Theme;
import remuco.client.jme.util.JMETools;

/**
 * A screeny to display meta information like title, artist, album and cover art
 * of an {@link Item}.
 */
public final class TitleScreeny extends Screeny {

	private int colorBg, colorArtist, colorAlbum, colorTitle, colorText;

	/** Width dependent spacer string for values in item details line. */
	private String detailsSpacer = "  ";

	private final Item noPlob;

	private static final String INFO_LEVEL_NORMAL = "Normal";
	private static final String INFO_LEVEL_DETAILED = "Detailed";

	public static final OptionDescriptor OD_INFO_LEVEL = new OptionDescriptor(
			"item-info", "Item information", INFO_LEVEL_NORMAL, new String[] {
					INFO_LEVEL_NORMAL, INFO_LEVEL_DETAILED });

	private boolean showDetails;

	public TitleScreeny(PlayerInfo player) {

		super(player);

		noPlob = new Item();

		data = noPlob;
	}

	protected void initRepresentation() throws ScreenyException {

		setImage(Image.createImage(width, height)); // occupy available space

		colorAlbum = theme.getColor(Theme.RTC_TEXT_ALBUM);
		colorArtist = theme.getColor(Theme.RTC_TEXT_ARTIST);
		colorBg = theme.getColor(Theme.RTC_BG);
		colorText = theme.getColor(Theme.RTC_TEXT_OTHER);
		colorTitle = theme.getColor(Theme.RTC_TEXT_TITLE);

		char ca[] = new char[(width / Theme.FONT_SMALL.charWidth(' ')) / 8];
		for (int i = 0; i < ca.length; i++) {
			ca[i] = ' ';
		}
		detailsSpacer = new String(ca);

		noPlob.setMeta(Item.META_TITLE, player.getName());

		showDetails = Config.getInstance().getOption(OD_INFO_LEVEL).equals(
			INFO_LEVEL_DETAILED);
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

		y += Theme.LINE_GAP_SMALL; // top border for image

		// //// details //////

		final int yDetails = height - Theme.LINE_GAP - g.getFont().getHeight();
		if (yDetails < y) {
			// not enough space for item details
			return;
		}

		final String details = buildDetailsString(item);
		if (showDetails && details != null) {
			g.setColor(colorText);
			g.setFont(Theme.FONT_SMALL);
			g.drawString(details.toString(), width / 2, yDetails, TOP_CENTER);
		}

		// //// image //////

		final int maxImgHeight;

		if (!showDetails || details == null) {
			maxImgHeight = height - Theme.LINE_GAP - y;
		} else {
			maxImgHeight = yDetails - Theme.LINE_GAP_SMALL - y;
		}

		if (maxImgHeight < 32) // not enough space for an image
			return;

		Image plobImg = JMETools.baToImage(item.getImg());

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

	/**
	 * Build a nice item details line string. Returns <code>null</code> if there
	 * are no details.
	 */
	private String buildDetailsString(Item item) {

		final StringBuffer details = new StringBuffer();

		final String year = item.getMeta(Item.META_YEAR);
		final String genre = item.getMeta(Item.META_GENRE);
		final String bitrate = item.getMeta(Item.META_BITRATE);

		if (year.length() > 0 && !year.equals("0")) {
			details.append(year);
		}
		if (genre.length() > 0 && !genre.equalsIgnoreCase("unknown")) {
			if (details.length() > 0) {
				details.append(detailsSpacer);
			}
			details.append(genre);
		}
		if (bitrate.length() > 0 && !bitrate.equals("0")) {
			if (details.length() > 0) {
				details.append(detailsSpacer);
			}
			details.append(bitrate).append('k');
		}

		return details.length() > 0 ? details.toString() : null;
	}

	private int drawStrings(String[] sa, int width, int y) {

		final int fontHeight = g.getFont().getHeight();

		for (int i = 0; i < sa.length; i++) {
			g.drawString(sa[i], width / 2, y, TOP_CENTER);
			y += fontHeight;
		}

		return y;
	}

}
