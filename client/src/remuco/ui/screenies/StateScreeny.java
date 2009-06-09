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

import remuco.player.PlayerInfo;
import remuco.player.SliderState;
import remuco.player.State;
import remuco.ui.IActionListener;
import remuco.ui.KeyBindings;
import remuco.ui.Theme;

public class StateScreeny extends Screeny {

	/** Image IDs for the playback button screeny. */
	private static final int BS_PLAYBACK_IMAGES[] = {
			Theme.RTE_STATE_PLAYBACK_PAUSE, Theme.RTE_STATE_PLAYBACK_PLAY,
			Theme.RTE_STATE_PLAYBACK_STOP };

	/** Values for the playback button screeny. */
	private static final Object BS_PLAYBACK_VALUES[] = {
			new Integer(State.PLAYBACK_PAUSE),
			new Integer(State.PLAYBACK_PLAY), new Integer(State.PLAYBACK_STOP) };

	/** Image IDs for the repeat button screeny. */
	private static final int BS_REPEAT_IMAGES[] = { Theme.RTE_STATE_REPEAT_OFF,
			Theme.RTE_STATE_REPEAT_ON };

	/** Values for the repeat button screeny. */
	private static final Object BS_REPEAT_VALUES[] = { Boolean.FALSE,
			Boolean.TRUE };

	/** Image IDs for the shuffle button screeny. */
	private static final int BS_SHUFFLE_IMAGES[] = {
			Theme.RTE_STATE_SHUFFLE_OFF, Theme.RTE_STATE_SHUFFLE_ON };

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
		screenyRepeat = new ButtonScreeny(player, BS_REPEAT_VALUES,
				BS_REPEAT_IMAGES, KeyBindings.ACTION_REPEAT);
		screenyShuffle = new ButtonScreeny(player, BS_SHUFFLE_VALUES,
				BS_SHUFFLE_IMAGES, KeyBindings.ACTION_SHUFFLE);

		screenyVolume = new SliderScreeny(player, Theme.RTE_STATE_VOLUME_LEFT,
				Theme.RTE_STATE_VOLUME_ON, Theme.RTE_STATE_VOLUME_OFF,
				Theme.RTE_STATE_VOLUME_RIGHT, KeyBindings.ACTION_VOLDOWN,
				KeyBindings.ACTION_VOLUP);
		
		sliderStateVolume = new SliderState();
		sliderStateVolume.setLength(100);
	}

	public void pointerPressed(int px, int py, IActionListener actionListener) {
		final int rx = px - getPreviousX();
		final int ry = py - getPreviousY();
		screenyPlayback.pointerPressed(rx, ry, actionListener);
		screenyRepeat.pointerPressed(rx, ry, actionListener);
		screenyShuffle.pointerPressed(rx, ry, actionListener);
		screenyVolume.pointerPressed(rx, ry, actionListener);
	}

	public void pointerReleased(int px, int py, IActionListener actionListener) {
		final int rx = px - getPreviousX();
		final int ry = py - getPreviousY();
		screenyPlayback.pointerReleased(rx, ry, actionListener);
		screenyRepeat.pointerReleased(rx, ry, actionListener);
		screenyShuffle.pointerReleased(rx, ry, actionListener);
		screenyVolume.pointerReleased(rx, ry, actionListener);
	}

	protected void dataUpdated() {

		State s = (State) data;

		screenyPlayback.updateData(new Integer(s.getPlayback()));
		screenyRepeat.updateData(s.isRepeat() ? Boolean.TRUE : Boolean.FALSE);
		screenyShuffle.updateData(s.isShuffle() ? Boolean.TRUE : Boolean.FALSE);

		sliderStateVolume.setPosition(s.getVolume());
		screenyVolume.updateData(sliderStateVolume);

	}

	protected void initRepresentation() throws ScreenyException {

		final Image spacer = theme.getImg(Theme.RTE_STATE_SPACER);

		final int h = theme.getImg(Theme.RTE_STATE_BORDER_N).getHeight()
				+ spacer.getHeight()
				+ theme.getImg(Theme.RTE_STATE_BORDER_S).getHeight();

		setImage(Image.createImage(width, h));

		final int clip[] = drawBorders(theme.getImg(Theme.RTE_STATE_BORDER_NW),
			theme.getImg(Theme.RTE_STATE_BORDER_N),
			theme.getImg(Theme.RTE_STATE_BORDER_NE),
			theme.getImg(Theme.RTE_STATE_BORDER_W),
			theme.getImg(Theme.RTE_STATE_BORDER_E),
			theme.getImg(Theme.RTE_STATE_BORDER_SW),
			theme.getImg(Theme.RTE_STATE_BORDER_S),
			theme.getImg(Theme.RTE_STATE_BORDER_SE),
			theme.getColor(Theme.RTC_BG_STATE));

		int xOff = clip[0]; // x offset for elements
		final int yOff = clip[1]; // y offset for elements
		int wRest = clip[2]; // available width for elements
		final int hRest = clip[3]; // available height for elements

		// ////// initially fill everything with spacer ////// //

		final int wSpacer = spacer.getWidth();
		for (int x = xOff; x < xOff + wRest; x += wSpacer) {
			g.drawImage(spacer, x, yOff, TOP_LEFT);
		}

		// ////// draw state elements ////// //

		int wGap;

		xOff = clip[0];
		screenyPlayback.initRepresentation(xOff, yOff, TOP_LEFT, wRest, hRest);

		if (screenyPlayback.getWidth() > 0) {
			wGap = wSpacer;
		} else {
			wGap = 0;
		}
		wRest -= wGap + screenyPlayback.getWidth();
		xOff = screenyPlayback.getNextX() + wGap;
		screenyRepeat.initRepresentation(xOff, yOff, TOP_LEFT, wRest, hRest);

		if (screenyRepeat.getWidth() > 0) {
			wGap = wSpacer;
		} else {
			wGap = 0;
		}
		wRest -= wGap + screenyRepeat.getWidth();
		xOff = screenyRepeat.getNextX() + wGap;
		screenyShuffle.initRepresentation(xOff, yOff, TOP_LEFT, wRest, hRest);

		if (screenyShuffle.getWidth() > 0) {
			wGap = wSpacer;
		} else {
			wGap = 0;
		}
		wRest -= wGap + screenyShuffle.getWidth();
		xOff = screenyShuffle.getNextX() + wGap;
		screenyVolume.initRepresentation(xOff, yOff, TOP_LEFT, wRest, hRest);

	}

	protected void updateRepresentation() {

		screenyPlayback.draw(g);
		screenyRepeat.draw(g);
		screenyShuffle.draw(g);
		screenyVolume.draw(g);

	}
}
