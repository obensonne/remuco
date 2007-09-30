package remuco.ui.screenies;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import remuco.player.Info;
import remuco.player.Plob;
import remuco.ui.Theme;
import remuco.util.Log;

public final class PlobScreeny extends Screeny {

	/**
	 * Object to use for {@link #updateData(Object)} when this screeny shall
	 * toogle whether to display the image of the current plob (given by a
	 * previous call to {@link #updateData(Object)}) as fullscreen.
	 */
	public static final Object ToogleImageFullScreen = new Object();

	private boolean fullScreenImage = false;

	private ImageScreeny screenyImage;

	private PlobDescScreeny screenyPlobDesc;

	private RateScreeny screenyRate;

	public PlobScreeny(Theme theme, Info pi) {

		super(theme);

		screenyPlobDesc = new PlobDescScreeny(theme);
		screenyRate = new RateScreeny(theme, pi);
		screenyImage = new ImageScreeny(theme);

	}

	protected void dataUpdated() {

		if (data == ToogleImageFullScreen) {

			fullScreenImage = fullScreenImage ? false : true;

		} else {

			screenyImage.updateData(data != null ? ((Plob) data).getImg()
					: null);
			screenyPlobDesc.updateData(data);
			screenyRate.updateData(data);

			fullScreenImage = false;

		}

	}

	protected void initRepresentation() throws ScreenyException {

		Image i;
		int borderSize, x, y;

		setImage(Image.createImage(width, height));

		// fill with background color

		g.setColor(theme.getColor(Theme.COLOR_BG));
		g.fillRect(0, 0, width, height);

		// draw top border and corners

		x = 0;
		y = 0;

		i = theme.getImg(Theme.IMGID_PLOB_CORNER_TOP_LEFT);
		borderSize = i.getWidth(); // border images are squares

		g.drawImage(i, x, y, Graphics.LEFT | Graphics.TOP);

		x += borderSize;

		i = theme.getImg(Theme.IMGID_PLOB_BORDER_TOP);

		for (; x < width; x += borderSize) {
			g.drawImage(i, x, y, Graphics.LEFT | Graphics.TOP);
		}

		i = theme.getImg(Theme.IMGID_PLOB_CORNER_TOP_RIGHT);

		x = width;
		g.drawImage(i, x, y, Graphics.RIGHT | Graphics.TOP);

		// draw side borders

		y = borderSize;

		i = theme.getImg(Theme.IMGID_PLOB_BORDER_LEFT);

		for (int j = y; j < height; j += borderSize) {
			g.drawImage(i, 0, j, Graphics.LEFT | Graphics.TOP);
		}

		i = theme.getImg(Theme.IMGID_PLOB_BORDER_RIGHT);

		for (int j = y; j < height; j += borderSize) {
			g.drawImage(i, width, j, Graphics.RIGHT | Graphics.TOP);
		}

		// draw bottom border and corners

		i = theme.getImg(Theme.IMGID_PLOB_CORNER_BOTTOM_LEFT);

		g.drawImage(i, 0, height, Graphics.LEFT | Graphics.BOTTOM);

		x = borderSize;

		i = theme.getImg(Theme.IMGID_PLOB_BORDER_BOTTOM);

		for (; x < width; x += borderSize) {
			g.drawImage(i, x, height, Graphics.LEFT | Graphics.BOTTOM);
		}

		i = theme.getImg(Theme.IMGID_PLOB_CORNER_BOTTOM_RIGHT);

		g.drawImage(i, width, height, Graphics.RIGHT | Graphics.BOTTOM);

		// ok, borders and corners done, now the sub screenies

		screenyRate.initRepresentation(width / 2, height - borderSize,
				Graphics.HCENTER | Graphics.BOTTOM, width - 2 * borderSize,
				(height - 2 * borderSize) / 3); // max 1/3 for rating

		screenyPlobDesc.initRepresentation(borderSize, borderSize, TOP_LEFT,
				width - 2 * borderSize, height - 2 * borderSize
						- screenyRate.getHeight());

		screenyImage.initRepresentation(borderSize, borderSize, TOP_LEFT, width
				- 2 * borderSize, height - 2 * borderSize);

	}

	protected void updateRepresentation() {

		if (fullScreenImage) {

			screenyImage.draw(g);

		} else {

			screenyPlobDesc.draw(g);
			screenyRate.draw(g);

		}

	}

}
