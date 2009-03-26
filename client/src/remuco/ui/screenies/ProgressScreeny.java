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

import remuco.player.Feature;
import remuco.player.PlayerInfo;
import remuco.player.Progress;
import remuco.ui.Theme;

/**
 * Sub screeny of an It
 * @author Oben Sonne
 *
 */
public class ProgressScreeny extends Screeny {

	private int colorBG = 0, colorFG;

	private int yOff = 0;

	public ProgressScreeny(PlayerInfo player) {

		super(player);

	}

	protected void initRepresentation() throws ScreenyException {

		if (!player.supports(Feature.KNOWN_PROGRESS)) {
			setImage(INVISIBLE);
			return;
		}

		final int fontHeight = Theme.FONT_PROGRESS.getHeight();

		setImage(Image.createImage(width, fontHeight * 3/2));

		yOff = fontHeight;

		g.setFont(Theme.FONT_PROGRESS);

		colorBG = theme.getColor(Theme.RTC_BG_ITEM);
		colorFG = theme.getColor(Theme.RTC_TEXT_OTHER);

	}

	protected void updateRepresentation() {

		g.setColor(colorBG);
		g.fillRect(0, 0, width, height);

		if (data == null) {
			return;
		}

		final Progress p = (Progress) data;

		final String len;
		if (p.getLength() < 0) {
			len = "???";
		} else {
			len = p.getLengthFormatted();
		}

		final String val;
		if (p.getProgress() < 0) {
			val = "???";
		} else {
			val = p.getProgressFormatted();
		}

		//Log.debug("update porgress values to " + val + "/" + len);
		
		g.setColor(colorFG);

		g.drawString(val, 0, yOff, Graphics.LEFT | Graphics.BASELINE);

		g.drawString(len, width, yOff, Graphics.RIGHT | Graphics.BASELINE);

	}

}
