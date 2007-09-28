package remuco.ui.screenies;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import remuco.player.State;
import remuco.ui.Theme;

public final class StateScreeny extends Screeny {

	private final StateSubScreeny screenyState, screenyRepeat, screenyShuffle;

	private final VolumeScreeny screenyVolume;

	public StateScreeny(Theme theme) {

		super(theme);

		screenyState = new StateSubScreeny(theme, StateSubScreeny.TYPE_STATE);
		screenyRepeat = new StateSubScreeny(theme, StateSubScreeny.TYPE_REPEAT);
		screenyShuffle = new StateSubScreeny(theme,
				StateSubScreeny.TYPE_SHUFFLE);
		screenyVolume = new VolumeScreeny(theme);

	}

	protected void dataUpdated() {

		State s = (State) data;

		screenyState.updateData(s);
		screenyRepeat.updateData(s);
		screenyShuffle.updateData(s);
		screenyVolume.updateData(s);

	}

	protected void initRepresentation() throws ScreenyException {

		int w = width;

		Image borderLeft, borderRight;

		borderLeft = theme.getImg(Theme.IMGID_STATE_BORDER_LEFT);
		borderRight = theme.getImg(Theme.IMGID_STATE_BORDER_RIGHT);

		setImage(Image.createImage(width, borderLeft.getHeight()));

		g.drawImage(borderLeft, 0, 0, TOP_LEFT);
		g.drawImage(borderRight, width, 0, Graphics.RIGHT | Graphics.TOP);

		w = width - borderLeft.getWidth() - borderRight.getWidth();

		screenyState.initRepresentation(borderLeft.getWidth(), 0, TOP_LEFT, w,
				height);
		w -= screenyState.getWidth();
		screenyRepeat.initRepresentation(screenyState.getNextX(), 0, TOP_LEFT,
				w, height);
		w -= screenyRepeat.getWidth();
		screenyShuffle.initRepresentation(screenyRepeat.getNextX(), 0,
				TOP_LEFT, w, height);
		w -= screenyShuffle.getWidth();
		screenyVolume.initRepresentation(screenyShuffle.getNextX(), 0,
				TOP_LEFT, w, height);

	}

	protected void updateRepresentation() {

		screenyState.draw(g);
		screenyRepeat.draw(g);
		screenyShuffle.draw(g);
		screenyVolume.draw(g);

	}

}
