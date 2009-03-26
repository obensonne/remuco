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

import remuco.player.Feature;
import remuco.player.PlayerInfo;
import remuco.player.SliderState;
import remuco.ui.Theme;

public final class SliderScreeny extends Screeny {

	public static final int TYPE_VOLUME = 0;

	/** The images that make up the slider */
	private Image imgLeft, imgOn, imgOff, imgRight;

	private final int imgLeftID, imgOnID, imgOffID, imgRightID;

	private int length = -1;

	private int position = 0;

	/** Number of steps (pixel) displayable in the progress bar */
	private int resolution;

	private final int type;

	// private final boolean displayLength;

	/** x position for first use of ({@link #imgOn} or {@link #imgOff}) */
	private int xBar;

	public SliderScreeny(PlayerInfo player, int type) {

		super(player);

		switch (type) {

		case TYPE_VOLUME:
			imgLeftID = Theme.RTE_STATE_VOLUME_LEFT;
			imgOnID = Theme.RTE_STATE_VOLUME_ON;
			imgOffID = Theme.RTE_STATE_VOLUME_OFF;
			imgRightID = Theme.RTE_STATE_VOLUME_RIGHT;
			break;

		default:
			throw new IllegalArgumentException();
		}

		this.type = type;

	}

	protected void dataUpdated() {

		final SliderState ss = (SliderState) data;
		position = ss.getPosition();
		length = ss.getLength();
		if (length <= 0) {
			length = 1;
		}

	}

	protected void initRepresentation() throws ScreenyException {

		int w, h;

		if (type == TYPE_VOLUME && !player.supports(Feature.KNOWN_VOLUME)) {
			setImage(INVISIBLE);
			return;
		}

		imgLeft = theme.getImg(imgLeftID);
		imgOn = theme.getImg(imgOnID);
		imgOff = theme.getImg(imgOffID);
		imgRight = theme.getImg(imgRightID);

		xBar = imgLeft.getWidth();
		resolution = width - imgLeft.getWidth() - imgRight.getWidth();

		w = imgLeft.getWidth() + resolution + imgRight.getWidth();
		h = imgLeft.getHeight();

		setImage(Image.createImage(w, h));

		g.drawImage(imgLeft, 0, 0, TOP_LEFT);

		g.drawImage(imgRight, width, 0, TOP_RIGHT);

	}

	protected void updateRepresentation() {

		int x, numOn;

		// convert position to number of 'imgOn' images (based on resolution):
		numOn = (int) ((float) resolution / length * position);

		x = xBar;

		for (int i = 0; i < numOn; i++, x++) {
			g.drawImage(imgOn, x, 0, TOP_LEFT);
		}
		for (int i = numOn; i < resolution; i++, x++) {
			g.drawImage(imgOff, x, 0, TOP_LEFT);
		}

	}

}
