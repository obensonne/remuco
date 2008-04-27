package remuco.ui.screenies;

import javax.microedition.lcdui.Image;

import remuco.player.IPlayerInfo;
import remuco.player.SliderState;
import remuco.ui.Theme;

public final class SliderScreeny extends Screeny {

	/** The images that make up the slider */
	private Image imgLeft, imgOn, imgOff, imgRight;

	private int length = -1;

	private int position = 0;

	/** Number of steps (pixel) displayable in the progress bar */
	private int resolution;

	/** x position for first use of ({@link #imgOn} or {@link #imgOff}) */
	private int xBar;

	public SliderScreeny(Theme theme, IPlayerInfo player) {

		super(theme, player);
	}

	protected void dataUpdated() {

		position = ((SliderState) data).getPosition();
		length = ((SliderState) data).getLength();

	}

	protected void initRepresentation() throws ScreenyException {

		int w, h;

		if (!player.isVolumeKnown()) {
			setImage(INVISIBLE);
			return;
		}

		imgLeft = theme.getImg(Theme.IMGID_STATE_VOLUME_LEFT);
		imgOn = theme.getImg(Theme.IMGID_STATE_VOLUME_ON);
		imgOff = theme.getImg(Theme.IMGID_STATE_VOLUME_OFF);
		imgRight = theme.getImg(Theme.IMGID_STATE_VOLUME_RIGHT);

		xBar = imgLeft.getWidth();

		resolution = width - imgRight.getWidth()- xBar;

		w = imgLeft.getWidth() + resolution
				+ imgRight.getWidth();
		h = imgLeft.getHeight();

		setImage(Image.createImage(w, h));

		g.drawImage(imgLeft, 0, 0, TOP_LEFT);

		g.drawImage(imgRight, width, 0, TOP_RIGHT);

	}

	protected void updateRepresentation() {

		int x, numOn;

		// convert position to number of 'imgOn' images (based on resolution):
		numOn = (int) ((float) resolution / length * position);

		x = xBar;

		for (int i = 0; i < numOn; i++, x++) {
			g.drawImage(imgOn, x, 0, TOP_LEFT);
		}
		for (int i = numOn; i < resolution; i++, x++) {
			g.drawImage(imgOff, x, 0, TOP_LEFT);
		}

	}

}
