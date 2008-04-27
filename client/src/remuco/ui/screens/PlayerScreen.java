package remuco.ui.screens;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;

import remuco.player.Player;
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

	private final StateScreeny screenyState;

	private final PlobScreeny screenyPlob;

	private final Theme theme;

	public PlayerScreen(Theme theme, Player player, IKeyListener kl) {

		this.theme = theme;
		this.kl = kl;

		screenyState = new StateScreeny(theme, player);
		screenyPlob = new PlobScreeny(theme, player);
	}

	/**
	 * Let the player screen update its representations due to a theme change.
	 * 
	 */
	public void themeOrPlayerChanged() {

		initScreenies();
	}

	/**
	 * Toggle whether the player screen shall display the image of its current
	 * plob as full screen or not. Note: When the player screen receives a new
	 * plob via {@link #update(Plob)} it always displays plob images in normal
	 * mode.
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
	public void update(Plob plob) {

		screenyPlob.updateData(plob);
		repaint(screenyPlob);
	}

	/**
	 * Let the player screen show an new state.
	 * 
	 * @param state
	 *            then new state to show
	 */
	public void update(State state) {

		screenyState.updateData(state);
		repaint(screenyState);
	}

	protected void keyPressed(int keyCode) {
		kl.keyPressed(keyCode);
	}

	protected void keyReleased(int keyCode) {
		kl.keyReleased(keyCode);
	}

	/**
	 * This methods finally paints the player screen. Gets called externally -
	 * e.g. as a result of {@link #repaint(Screeny)} or {@link #repaint()}
	 * (which may get called by ourselves).
	 * 
	 * @param g
	 *            the graphics where we can draw our screen into
	 * @see Canvas
	 */
	protected void paint(Graphics g) {

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

		initScreenies();

		repaint(); // guess we need this
	}

	private void initScreenies() {

		int w, h, x, y, anchor;

		if (getWidth() == 0 || getHeight() == 0)
			return;

		try {
			// ////// playback, volume, repeat and shuffle ////// //

			anchor = Screeny.TOP_LEFT;
			x = 0;
			w = getWidth();
			h = getHeight() / 4; // max 1/4 for state
			y = 0;
			screenyState.initRepresentation(x, y, anchor, w, h);

			// ////// plob ////// //

			anchor = Screeny.TOP_LEFT;
			x = 0;
			w = getWidth();
			y = screenyState.getNextY();
			h = getHeight() - y;
			screenyPlob.initRepresentation(x, y, anchor, w, h);

			screenTooSmall = false;

		} catch (ScreenyException e) {

			screenTooSmall = true;
			Log.ln("[PD] screen too small", e);
		}
	}

	/** Request a repaint for the region occupied by a screeny. */
	private void repaint(Screeny s) {

		repaint(s.getX(), s.getY(), s.getWidth(), s.getHeight());
	}
}
