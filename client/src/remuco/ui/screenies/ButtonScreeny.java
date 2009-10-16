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

import remuco.client.common.data.PlayerInfo;
import remuco.ui.IActionListener;
import remuco.ui.KeyBindings;
import remuco.util.Log;

/**
 * A generic screeny to display a button (which may have multiple states). It
 * issues pressed and released actions (from {@link KeyBindings}) on pointer
 * interaction.
 */
public class ButtonScreeny extends Screeny {

	private static final Object DUMMY_VALUES[] = { new Object() };

	private final int action;

	private final int imageIDs[];

	private final Image images[];

	private boolean pressed = false;

	private final Object values[];

	/**
	 * Create a button screeny which has always the same button image.
	 * 
	 * @param player
	 * @param imgID
	 *            ID of the image to use as button image
	 * @param action
	 *            action (from {@link KeyBindings}) to issue when this button is
	 *            pressed/released
	 */
	public ButtonScreeny(PlayerInfo player, int imgID, int action) {
		this(player, DUMMY_VALUES, new int[] { imgID }, action);
	}

	/**
	 * Create a button screeny which has varying button images, depending on
	 * values represented by the button.
	 * 
	 * @param player
	 * @param values
	 *            values to represent by this button's image (must contain all
	 *            values potentially set by {@link #updateData(Object)})
	 * @param imgageIDs
	 *            IDs of the images corresponding to the values
	 * @param action
	 *            action (from {@link KeyBindings}) to issue when this button is
	 *            pressed/released
	 * @throws IllegalArgumentException
	 *             if <em>values</em> and <em>imageIDs</em> are empty or differ
	 *             in length
	 */
	public ButtonScreeny(PlayerInfo player, Object values[], int imgageIDs[],
			int action) {
		super(player);
		if (values.length != imgageIDs.length || values.length == 0) {
			throw new IllegalArgumentException();
		}
		this.values = values;
		this.imageIDs = imgageIDs;
		this.images = new Image[imgageIDs.length];
		this.action = action;
	}

	public void pointerPressed(int px, int py, IActionListener actionListener) {
		if (!isInScreeny(px, py)) {
			return;
		}
		pressed = true;
		actionListener.handleActionPressed(this.action);
	}

	public void pointerReleased(int px, int py, IActionListener actionListener) {
		if (!pressed) {
			return;
		}
		pressed = false;
		actionListener.handleActionReleased(this.action);
	}

	protected void initRepresentation() throws ScreenyException {

		for (int i = 0; i < imageIDs.length; i++) {
			images[i] = theme.getImg(imageIDs[i]);
		}
		setImage(images[0]);
	}

	protected void updateRepresentation() {

		if (values.length == 1) {
			return;
		}

		for (int i = 0; i < values.length; i++) {
			if (values[i].equals(data)) {
				try {
					setImage(images[i]);
				} catch (ScreenyException e) {
					// does not happen on well formed theme
				}
				return;
			}
		}
		Log.bug("unexpected value: " + data);
	}

}
