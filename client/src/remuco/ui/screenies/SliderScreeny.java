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
import remuco.ui.IActionListener;
import remuco.ui.KeyBindings;
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

	// private final boolean displayLength;

	private final int type;

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

	public void pointerPressed(int px, int py, IActionListener actionListener) {
		
		if (!isInScreeny(px, py)) {
			return;
		}

		// convert position to number of filled pixels (based on resolution):
		final int filled = (int) ((float) resolution / length * position);

		if (px < getX() + xBar + filled) {
			actionListener.handleActionPressed(KeyBindings.ACTION_VOLDOWN);
		} else {
			actionListener.handleActionPressed(KeyBindings.ACTION_VOLUP);
		}
	}

	public void pointerReleased(int px, int py, IActionListener actionListener) {

		// stop any running volume adjustments (also if pointer has been
		// released outside the area of this screeny)
		actionListener.handleActionReleased(KeyBindings.ACTION_VOLDOWN);
		actionListener.handleActionReleased(KeyBindings.ACTION_VOLUP);

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

		if (type == TYPE_VOLUME && !player.supports(Feature.KNOWN_VOLUME)) {
			setImage(INVISIBLE);
			return;
		}

		imgLeft = theme.getImg(imgLeftID);
		imgOn = theme.getImg(imgOnID);
		imgOff = theme.getImg(imgOffID);
		imgRight = theme.getImg(imgRightID);

		final int wLeft = imgLeft.getWidth();
		final int wRight = imgRight.getWidth();

		xBar = wLeft;
		resolution = width - wLeft - wRight;
		if (resolution < 5) {
			throw new ScreenyException("screen to small for volume bar");
		}

		setImage(Image.createImage(width, imgLeft.getHeight()));

		g.drawImage(imgLeft, 0, 0, TOP_LEFT);

		// do not use TOP_RIGHT because this gets handled wrong by Nokia 5310
		g.drawImage(imgRight, wLeft + resolution, 0, TOP_LEFT);

	}

	protected void updateRepresentation() {

		// convert position to number of filled pixels (based on resolution):
		final int filled = (int) ((float) resolution / length * position);

		int x = xBar;

		for (int i = 0; i < filled; i++, x++) {
			g.drawImage(imgOn, x, 0, TOP_LEFT);
		}
		for (int i = filled; i < resolution; i++, x++) {
			g.drawImage(imgOff, x, 0, TOP_LEFT);
		}

	}

}
