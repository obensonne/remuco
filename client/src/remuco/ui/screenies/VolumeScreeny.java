package remuco.ui.screenies;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import remuco.player.State;
import remuco.ui.Theme;

public final class VolumeScreeny extends Screeny {

	/** The images that make up the volume bar */
	private Image imgLeft, imgOn, imgOff, imgRight;

	/** Number of steps (pixel) displayable in the progress bar */
	private int resolution;

	private int volume = 0;

	public VolumeScreeny(Theme theme) {
		super(theme);
	}

	protected void dataUpdated() {

		volume = ((State) data).getVolume();

		if (volume < 0)
			volume = 0;
		else if (volume > 100)
			volume = 100;

	}

	protected void initRepresentation() throws ScreenyException {

		int w, h;

		imgLeft = theme.getImg(Theme.IMGID_STATE_VOLUME_LEFT);
		imgOn = theme.getImg(Theme.IMGID_STATE_VOLUME_ON);
		imgOff = theme.getImg(Theme.IMGID_STATE_VOLUME_OFF);
		imgRight = theme.getImg(Theme.IMGID_STATE_VOLUME_RIGHT);

		resolution = width - imgLeft.getWidth() - imgRight.getWidth();

		w = imgLeft.getWidth() + resolution + imgRight.getWidth();
		h = imgLeft.getHeight();

		setImage(Image.createImage(w, h));

	}

	protected void updateRepresentation() {

		int x = 0, y = 0, numOn;

		// convert volume to number of 'imgOn' images (based on resolution):
		numOn = (int) ((float) resolution / 100 * volume);

		g.drawImage(imgLeft, x, y, Graphics.TOP | Graphics.LEFT);
		x += imgLeft.getWidth();

		for (int i = 0; i < numOn; i++, x++) {
			g.drawImage(imgOn, x, y, Graphics.TOP | Graphics.LEFT);
		}
		for (int i = numOn; i < resolution; i++, x++) {
			g.drawImage(imgOff, x, y, Graphics.TOP | Graphics.LEFT);
		}

		g.drawImage(imgRight, x, y, Graphics.TOP | Graphics.LEFT);

	}

}
