package remuco.ui.screenies;

import javax.microedition.lcdui.Image;

import remuco.player.PlayerInfo;
import remuco.player.SliderState;
import remuco.player.State;
import remuco.ui.Theme;

public class StateScreeny extends Screeny {

	private final SimpleScreeny screenyPlayback, screenyRepeat, screenyShuffle;

	private final SliderScreeny screenyVolume;

	private final SliderState sliderStateVolume;

	public StateScreeny(PlayerInfo player) {

		super(player);

		screenyPlayback = new SimpleScreeny(player,
				SimpleScreeny.TYPE_PLAYBACK);
		screenyRepeat = new SimpleScreeny(player,
				SimpleScreeny.TYPE_REPEAT);
		screenyShuffle = new SimpleScreeny(player,
				SimpleScreeny.TYPE_SHUFFLE);

		screenyVolume = new SliderScreeny(player);

		sliderStateVolume = new SliderState();
		sliderStateVolume.setLength(100);
	}

	protected void dataUpdated() {

		State s = (State) data;

		screenyPlayback.updateData(s);
		screenyRepeat.updateData(s);
		screenyShuffle.updateData(s);

		sliderStateVolume.setPosition(s.getVolume());
		screenyVolume.updateData(sliderStateVolume);

	}

	protected void initRepresentation() throws ScreenyException {

		int w, x, wSpacer;
		Image borderLeft, borderRight, spacer;

		borderLeft = Theme.getImg(Theme.IMGID_STATE_BORDER_LEFT);
		borderRight = Theme.getImg(Theme.IMGID_STATE_BORDER_RIGHT);
		spacer = Theme.getImg(Theme.IMGID_STATE_SPACER);

		setImage(Image.createImage(width, borderLeft.getHeight()));

		// ////// initially fill everything with spacer ////// //

		wSpacer = spacer.getWidth();
		for (x = 0; x < width; x += wSpacer) {
			g.drawImage(spacer, x, 0, TOP_LEFT);
		}

		// ////// draw left and right border ////// //

		g.drawImage(borderLeft, 0, 0, TOP_LEFT);
		g.drawImage(borderRight, width, 0, TOP_RIGHT);

		// ////// draw state elements ////// //
		
		w = width - borderLeft.getWidth() - borderRight.getWidth();

		x = borderLeft.getWidth();
		screenyPlayback.initRepresentation(x, 0, TOP_LEFT, w, height);
		w -= screenyPlayback.getWidth();
		
		x = width - borderRight.getWidth();
		screenyShuffle.initRepresentation(x, 0, TOP_RIGHT, w, height);
		w -= screenyShuffle.getWidth();

		x = screenyShuffle.getPreviousX();
		screenyRepeat.initRepresentation(x, 0, TOP_RIGHT, w, height);
		w -= screenyRepeat.getWidth();

		x = screenyPlayback.getNextX();
		screenyVolume.initRepresentation(x, 0, TOP_LEFT, w, height);

	}

	protected void updateRepresentation() {

		screenyPlayback.draw(g);
		screenyRepeat.draw(g);
		screenyShuffle.draw(g);
		screenyVolume.draw(g);

	}
}
