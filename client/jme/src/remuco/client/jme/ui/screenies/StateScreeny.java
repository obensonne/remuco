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
import remuco.client.common.data.State;
import remuco.client.common.player.Feature;
import remuco.client.common.util.Log;
import remuco.client.jme.ui.IActionListener;
import remuco.client.jme.ui.KeyBindings;
import remuco.client.jme.ui.SliderState;
import remuco.client.jme.ui.Theme;

/**
 * A container screeny to display the state of a player.
 */
public class StateScreeny extends Screeny {

	/** Image IDs for the playback button screeny. */
	private static final int BS_PLAYBACK_IMAGES[] = {
			Theme.RTE_BUTTON_PLAYBACK_PAUSE, Theme.RTE_BUTTON_PLAYBACK_PLAY,
			Theme.RTE_BUTTON_PLAYBACK_STOP };

	/** Values for the playback button screeny. */
	private static final Object BS_PLAYBACK_VALUES[] = {
			new Integer(State.PLAYBACK_PAUSE),
			new Integer(State.PLAYBACK_PLAY), new Integer(State.PLAYBACK_STOP) };

	/** Image IDs for the repeat button screeny. */
	private static final int BS_REPEAT_IMAGES[] = {
			Theme.RTE_BUTTON_REPEAT_OFF, Theme.RTE_BUTTON_REPEAT_ON };

	/** Values for the repeat button screeny. */
	private static final Object BS_REPEAT_VALUES[] = { Boolean.FALSE,
			Boolean.TRUE };

	/** Image IDs for the shuffle button screeny. */
	private static final int BS_SHUFFLE_IMAGES[] = {
			Theme.RTE_BUTTON_SHUFFLE_OFF, Theme.RTE_BUTTON_SHUFFLE_ON };

	/** Values for the shuffle button screeny. */
	private static final Object BS_SHUFFLE_VALUES[] = { Boolean.FALSE,
			Boolean.TRUE };

	private final ButtonScreeny screenyPlayback, screenyRepeat, screenyShuffle;

	private final SliderScreeny screenyVolume;

	private final SliderState sliderStateVolume;

	public StateScreeny(PlayerInfo player) {

		super(player);

		screenyPlayback = new ButtonScreeny(player, BS_PLAYBACK_VALUES,
				BS_PLAYBACK_IMAGES, KeyBindings.ACTION_PLAYPAUSE);
		if (player.supports(Feature.COMB_REPEAT)) {
			screenyRepeat = new ButtonScreeny(player, BS_REPEAT_VALUES,
					BS_REPEAT_IMAGES, KeyBindings.ACTION_REPEAT);
		} else {
			screenyRepeat = null;
		}
		if (player.supports(Feature.COMB_SHUFFLE)) {
			screenyShuffle = new ButtonScreeny(player, BS_SHUFFLE_VALUES,
					BS_SHUFFLE_IMAGES, KeyBindings.ACTION_SHUFFLE);
		} else {
			screenyShuffle = null;
		}
		screenyVolume = new SliderScreeny(player, Theme.RTE_SLIDER_VOLUME_LEFT,
				Theme.RTE_SLIDER_VOLUME_ON, Theme.RTE_SLIDER_VOLUME_OFF,
				Theme.RTE_SLIDER_VOLUME_RIGHT, KeyBindings.ACTION_VOLDOWN,
				KeyBindings.ACTION_VOLUP);

		sliderStateVolume = new SliderState();
		sliderStateVolume.setLength(100);
	}

	public void pointerPressed(int px, int py, IActionListener actionListener) {
		final int rx = px - getPreviousX();
		final int ry = py - getPreviousY();
		screenyPlayback.pointerPressed(rx, ry, actionListener);
		if (screenyRepeat != null) {
			screenyRepeat.pointerPressed(rx, ry, actionListener);
		}
		if (screenyShuffle != null) {
			screenyShuffle.pointerPressed(rx, ry, actionListener);
		}
		screenyVolume.pointerPressed(rx, ry, actionListener);
	}

	public void pointerReleased(int px, int py, IActionListener actionListener) {
		final int rx = px - getPreviousX();
		final int ry = py - getPreviousY();
		screenyPlayback.pointerReleased(rx, ry, actionListener);
		if (screenyRepeat != null) {
			screenyRepeat.pointerReleased(rx, ry, actionListener);
		}
		if (screenyShuffle != null) {
			screenyShuffle.pointerReleased(rx, ry, actionListener);
		}
		screenyVolume.pointerReleased(rx, ry, actionListener);
	}

	protected void dataUpdated() {

		State s = (State) data;

		screenyPlayback.updateData(new Integer(s.getPlayback()));
		if (screenyRepeat != null) {
			screenyRepeat.updateData(new Boolean(s.isRepeat()));
		}
		if (screenyShuffle != null) {
			screenyShuffle.updateData(new Boolean(s.isShuffle()));
		}
		sliderStateVolume.setPosition(s.getVolume());
		screenyVolume.updateData(sliderStateVolume);

	}

	protected void initRepresentation() throws ScreenyException {

		final Image imgLeft = theme.getImg(Theme.RTE_STATE_LEFT);
		final Image imgSpacer = theme.getImg(Theme.RTE_STATE_SPACER);
		final Image imgRight = theme.getImg(Theme.RTE_STATE_RIGHT);

		final int h = imgLeft.getHeight();

		setImage(Image.createImage(width, h));

		final int wSpacer = imgSpacer.getWidth();
		final int xEnd = width - imgRight.getWidth();
		int x = 0;
		g.drawImage(imgLeft, x, 0, TOP_LEFT);
		x += imgLeft.getWidth();

		screenyPlayback.initRepresentation(x, 0, TOP_LEFT, xEnd - x, h);

		x = screenyPlayback.getNextX();
		g.drawImage(imgSpacer, x, 0, TOP_LEFT);
		x += wSpacer;

		if (screenyRepeat != null) {
			screenyRepeat.initRepresentation(x, 0, TOP_LEFT, xEnd - x, h);

			x = screenyRepeat.getNextX();
			g.drawImage(imgSpacer, x, 0, TOP_LEFT);
			x += wSpacer;
		}

		if (screenyShuffle != null) {
			screenyShuffle.initRepresentation(x, 0, TOP_LEFT, xEnd - x, h);

			x = screenyShuffle.getNextX();
			g.drawImage(imgSpacer, x, 0, TOP_LEFT);
			x += wSpacer;
		}

		screenyVolume.initRepresentation(x, 0, TOP_LEFT, xEnd - x, h);

		x = screenyVolume.getNextX();
		g.drawImage(imgRight, x, 0, TOP_LEFT);
		x += imgRight.getWidth();

		if (x != width) {
			Log.bug("Bug in TS: " + x + "!=" + width);
		}

	}

	protected void updateRepresentation() {

		screenyPlayback.draw(g);
		if (screenyRepeat != null) {
			screenyRepeat.draw(g);
		}
		if (screenyShuffle != null) {
			screenyShuffle.draw(g);
		}
		screenyVolume.draw(g);

	}
}
