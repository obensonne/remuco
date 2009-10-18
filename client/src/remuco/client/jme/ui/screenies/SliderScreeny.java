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

import javax.microedition.lcdui.Image;

import remuco.client.common.data.PlayerInfo;
import remuco.client.jme.ui.IActionListener;
import remuco.client.jme.ui.KeyBindings;
import remuco.client.jme.ui.SliderState;

/**
 * A generic screeny to display a {@link SliderState}. It issues lower and raise
 * actions (from {@link KeyBindings}) on pointer interaction.
 */
public final class SliderScreeny extends Screeny {

	public static final int TYPE_VOLUME = 0;

	private final int actionLower, actionRaise;

	/** Last action issued by pointer pressed (-1 means no active action). */
	private int activeAction = -1;

	/** The images that make up the slider */
	private Image imgLeft, imgOn, imgOff, imgRight;

	private final int imgLeftID, imgOnID, imgOffID, imgRightID;

	private int length = -1;

	private int position = 0;

	/** Number of steps (pixel) displayable in the progress bar */
	private int resolution;

	/** x position for first use of ({@link #imgOn} or {@link #imgOff}) */
	private int xBar;

	/**
	 * Create a new slider screeny.
	 * 
	 * @param player
	 * @param imgIdLeft
	 *            ID of the image for the left part/border
	 * @param imgIdFilled
	 *            ID of the image for the filled part
	 * @param imgIdUnfilled
	 *            ID of the image for the unfilled part
	 * @param imgIdRight
	 *            ID of the image for the right part/border
	 * @param actionLower
	 *            action (from {@link KeyBindings}) to issue if the left or
	 *            filled part is pressed - lowers the slider
	 * @param actionRaise
	 *            action (from {@link KeyBindings}) to issue if the right or
	 *            unfilled part is pressed - raises the slider
	 */
	public SliderScreeny(PlayerInfo player, int imgIdLeft, int imgIdFilled,
			int imgIdUnfilled, int imgIdRight, int actionLower, int actionRaise) {

		super(player);

		imgLeftID = imgIdLeft;
		imgOnID = imgIdFilled;
		imgOffID = imgIdUnfilled;
		imgRightID = imgIdRight;

		this.actionLower = actionLower;
		this.actionRaise = actionRaise;
	}

	public void pointerPressed(int px, int py, IActionListener actionListener) {

		if (!isInScreeny(px, py)) {
			return;
		}

		if (activeAction != -1) {
			// unlikely, but safety first
			actionListener.handleActionReleased(activeAction);
		}

		// convert position to number of filled pixels (based on resolution):
		final int filled = (int) ((float) resolution / length * position);

		if (px < getX() + xBar + filled) {
			activeAction = actionLower;
		} else {
			activeAction = actionRaise;
		}
		actionListener.handleActionPressed(activeAction);
	}

	public void pointerReleased(int px, int py, IActionListener actionListener) {

		if (activeAction != -1) {
			actionListener.handleActionReleased(activeAction);
			activeAction = -1;
		}
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

		imgLeft = theme.getImg(imgLeftID);
		imgOn = theme.getImg(imgOnID);
		imgOff = theme.getImg(imgOffID);
		imgRight = theme.getImg(imgRightID);

		final int wLeft = imgLeft.getWidth();
		final int wRight = imgRight.getWidth();

		xBar = wLeft;
		resolution = width - wLeft - wRight;
		if (resolution < 5) {
			throw new ScreenyException("screen to small for slider");
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
