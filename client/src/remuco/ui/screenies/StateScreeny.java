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

		screenyPlayback = new SimpleScreeny(player, SimpleScreeny.TYPE_PLAYBACK);
		screenyRepeat = new SimpleScreeny(player, SimpleScreeny.TYPE_REPEAT);
		screenyShuffle = new SimpleScreeny(player, SimpleScreeny.TYPE_SHUFFLE);

		screenyVolume = new SliderScreeny(player, SliderScreeny.TYPE_VOLUME);
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
			theme.getColor(Theme.RTC_BG_ALL));

		int xOff = clip[0]; // x offset for elements
		final int yOff = clip[1]; // y offset for elements

		// ////// initially fill everything with spacer ////// //

		final int wSpacer = spacer.getWidth();
		for (int x = xOff; x < clip[2]; x += wSpacer) {
			g.drawImage(spacer, x, yOff, TOP_LEFT);
		}

		// ////// draw state elements ////// //

		int wRest = clip[2]; // available width for elements
		final int hRest = clip[3]; // available height for elements
		int wGap;;

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
