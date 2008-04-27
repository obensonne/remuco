package remuco.ui.screenies;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import remuco.player.IPlayerInfo;
import remuco.player.Plob;
import remuco.ui.Theme;

public final class RateScreeny extends Screeny {

	private int rating;

	private int ratingMax;

	private Image ratingOn, ratingOff;

	private int ratingWidth, ratingHeight;

	/**
	 * Create new rate screeny. The given player info <code>player</code> is
	 * used to get the maximum rating value. The player info is expected to be
	 * valid the whole lifetime of the screeny. The screeny does not care about
	 * changes within the player info. If there are changes, the class which
	 * uses this screeny must care about this by calling
	 * {@link #initRepresentation(int, int, int, int, int)}.
	 * 
	 * @param player
	 *            the player info
	 */
	public RateScreeny(Theme theme, IPlayerInfo player) {

		super(theme, player);
	}

	protected void dataUpdated() {

		Plob plob = (Plob) data;

		if (plob == null)
			rating = player.getMaxRating();
		else
			rating = plob.getRating();

	}

	protected void initRepresentation() throws ScreenyException {

		Image i;

		ratingMax = player.getMaxRating();

		if (ratingMax == 0) {
			setImage(INVISIBLE);
			return;
		}

		ratingOn = theme.getImg(Theme.IMGID_PLOB_RATE_ON);
		ratingOff = theme.getImg(Theme.IMGID_PLOB_RATE_OFF);

		// scale rate images if needed (may happen if the remote player has a
		// wide rating range)
		ratingOn = Theme.shrinkImageIfNeeded(ratingOn, width / ratingMax,
				height);
		ratingOff = Theme.shrinkImageIfNeeded(ratingOff, width / ratingMax,
				height);

		ratingWidth = ratingOn.getWidth();
		ratingHeight = ratingOn.getHeight();

		i = Image.createImage(ratingMax * ratingWidth, ratingHeight);

		setImage(i);

		g.setColor(theme.getColor(Theme.COLOR_BG));
		g.fillRect(0, 0, width, height);

	}

	protected void updateRepresentation() {

		int x;

		if (ratingMax == 0)
			return;

		x = 0;
		for (int j = 1; j <= rating; j++) {
			g.drawImage(ratingOn, x, 0, Graphics.LEFT | Graphics.TOP);
			x += ratingWidth;
		}
		for (int j = rating + 1; j <= ratingMax; j++) {
			g.drawImage(ratingOff, x, 0, Graphics.LEFT | Graphics.TOP);
			x += ratingWidth;
		}

	}

}
