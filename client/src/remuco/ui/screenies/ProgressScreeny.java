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
package remuco.ui.screenies;

import javax.microedition.lcdui.Image;

import remuco.player.Feature;
import remuco.player.PlayerInfo;
import remuco.player.Progress;
import remuco.ui.Theme;

/**
 * A screeny to display the progress of an item.
 */
public class ProgressScreeny extends Screeny {

	private int colorBG = 0, colorFG;

	private int yOff, xOff;

	public ProgressScreeny(PlayerInfo player) {

		super(player);

	}

	protected void initRepresentation() throws ScreenyException {

		if (!player.supports(Feature.KNOWN_PROGRESS)) {
			setImage(INVISIBLE);
			return;
		}

		final int fontHeight = Theme.FONT_PROGRESS.getHeight();

		setImage(Image.createImage(width, fontHeight + Theme.LINE_GAP));

		xOff = width / 2;
		yOff = fontHeight;

		g.setFont(Theme.FONT_PROGRESS);

		colorBG = theme.getColor(Theme.RTC_BG);
		colorFG = theme.getColor(Theme.RTC_TEXT_OTHER);
	}

	protected void updateRepresentation() {

		g.setColor(colorBG);
		g.fillRect(0, 0, width, height);

		if (data == null) {
			return;
		}

		final Progress p = (Progress) data;

		final StringBuffer sb = new StringBuffer(13);

		if (p.getProgress() < 0) {
			sb.append("???");
		} else {
			sb.append(p.getProgressFormatted());
		}

		if (p.getLength() > 0) {
			sb.append("    -    ");
			sb.append(p.getLengthFormatted());
		}

		g.setColor(colorFG);

		g.drawString(sb.toString(), xOff, yOff, BOTTOM_CENTER);
	}

}
