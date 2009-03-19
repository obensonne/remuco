package remuco.ui.screenies;

import javax.microedition.lcdui.Image;

import remuco.player.Feature;
import remuco.player.PlayerInfo;
import remuco.player.SliderState;
import remuco.ui.Theme;

public final class SliderScreeny extends Screeny {

	public static final int TYPE_VOLUME = 0;

	/** The images that make up the slider */
	private Image imgLeft, imgOn, imgOff, imgRight;

	private final int imgLeftID, imgOnID, imgOffID, imgRightID;

	private int length = -1;

	private int position = 0;

	/** Number of steps (pixel) displayable in the progress bar */
	private int resolution;

	private final int type;

	// private final boolean displayLength;

	/** x position for first use of ({@link #imgOn} or {@link #imgOff}) */
	private int xBar;

	public SliderScreeny(PlayerInfo player, int type) {

		super(player);

		switch (type) {

		case TYPE_VOLUME:
			imgLeftID = Theme.RTE_STATE_VOLUME_LEFT;
			imgOnID = Theme.RTE_STATE_VOLUME_ON;
			imgOffID = Theme.RTE_STATE_VOLUME_OFF;
			imgRightID = Theme.RTE_STATE_VOLUME_RIGHT;
			break;

		default:
			throw new IllegalArgumentException();
		}

		this.type = type;

	}

	protected void dataUpdated() {

		final SliderState ss = (SliderState) data;
		position = ss.getPosition();
		length = ss.getLength();
		if (length <= 0) {
			length = 1;
		}

	}

	protected void initRepresentation() throws ScreenyException {

		int w, h;

		if (type == TYPE_VOLUME && !player.supports(Feature.KNOWN_VOLUME)) {
			setImage(INVISIBLE);
			return;
		}

		imgLeft = theme.getImg(imgLeftID);
		imgOn = theme.getImg(imgOnID);
		imgOff = theme.getImg(imgOffID);
		imgRight = theme.getImg(imgRightID);

		xBar = imgLeft.getWidth();
		resolution = width - imgLeft.getWidth() - imgRight.getWidth();

		w = imgLeft.getWidth() + resolution + imgRight.getWidth();
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
