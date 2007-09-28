package remuco.ui.screens;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

import remuco.Remuco;
import remuco.player.Info;
import remuco.player.Plob;
import remuco.player.State;
import remuco.ui.IKeyListener;
import remuco.ui.Theme;
import remuco.ui.screenies.PlobScreeny;
import remuco.ui.screenies.Screeny;
import remuco.ui.screenies.ScreenyException;
import remuco.ui.screenies.StateScreeny;
import remuco.util.Log;

public final class PlayerScreen extends Canvas {

	private final IKeyListener kl;

	private boolean screenTooSmall = false;

	private final PlobScreeny screenyPlob;

	private final StateScreeny screenyState;

	private final Theme theme;

	public PlayerScreen(Theme theme, IKeyListener kl, Info pi) {

		this.theme = theme;
		this.kl = kl;

		screenyState = new StateScreeny(theme);
		screenyPlob = new PlobScreeny(theme, pi);

	}

	/**
	 * Toogle whether the player screen shall display the image of its current
	 * plob as fullscreen or not. Note: When the player screen receives a new
	 * plob via {@link #updatePlob(Plob)} it allways displays plob images in
	 * normal mode.
	 */
	public void tooglePlobImageFullscreen() {

		screenyPlob.updateData(PlobScreeny.ToogleImageFullScreen);

		repaint(screenyPlob.getX(), screenyPlob.getY(), screenyPlob.getWidth(),
				screenyPlob.getHeight());

	}

	/**
	 * Let the player screen show an new plob (or none if <code>plob</code> is
	 * <code>null</code>).
	 * 
	 * @param plob
	 *            the new plob to show
	 */
	public void updatePlob(Plob plob) {

		screenyPlob.updateData(plob);
		repaint(screenyPlob.getX(), screenyPlob.getY(), screenyPlob.getWidth(),
				screenyPlob.getHeight());

	}

	/**
	 * Let the player screen show an new state.
	 * 
	 * @param state
	 *            then new state to show
	 */
	public void updateState(State state) {

		screenyState.updateData(state);
		repaint(screenyState.getX(), screenyState.getY(), screenyState
				.getWidth(), screenyState.getHeight());

	}

	/**
	 * Let the player screen update its representations due to a theme change.
	 * 
	 */
	public void updateTheme() {

		Log.debug("[PD] update theme");

		initScreenies();

	}

	protected void keyPressed(int keyCode) {
		kl.keyPressed(keyCode);
	}

	protected void keyReleased(int keyCode) {
		kl.keyReleased(keyCode);
	}

	protected void paint(Graphics g) {

		Log.debug("[PD] paint called");

		if (screenTooSmall) {

			int y;

			g.setColor(0xFFFFFF);
			g.fillRect(0, 0, getWidth(), getHeight());

			g.setColor(0);
			g.setFont(Theme.FONT_SMALL);

			y = getHeight() / 2 - Theme.FONT_SMALL.getHeight();
			g.drawString("The screen is too small", getWidth() / 2, y,
					Graphics.HCENTER | Graphics.BASELINE);

			y += Theme.FONT_SMALL.getHeight() * 2;
			g.drawString("for the theme " + theme.getName() + "!",
					getWidth() / 2, y, Graphics.HCENTER | Graphics.BASELINE);

		} else {

			screenyState.draw(g);
			screenyPlob.draw(g);
		}
	}

	protected void sizeChanged(int w, int h) {

		super.sizeChanged(w, h);

		Log.debug("PlayerScreen.sizeChanged() called");

		initScreenies();

		repaint(); // XXX needed ?
	}

	private void initScreenies() {

		if (getWidth() == 0 || getHeight() == 0)
			return;

		try {
			screenyState.initRepresentation(0, 0, Screeny.TOP_LEFT, getWidth(),
					getHeight() / 3); // max 1/3 for state symbols
			screenyPlob.initRepresentation(0, screenyState.getNextY(),
					Screeny.TOP_LEFT, getWidth(), getHeight()
							- screenyState.getHeight());
			screenTooSmall = false;
		} catch (ScreenyException e) {
			if (Remuco.EMULATION)
				e.printStackTrace();
			screenTooSmall = true;
			Log.ln("[PD] screen too small");
		}

	}

}
