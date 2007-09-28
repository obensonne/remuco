package remuco.ui.screenies;

import javax.microedition.lcdui.Image;

import remuco.player.State;
import remuco.ui.Theme;

public final class StateSubScreeny extends Screeny {

	public static final int TYPE_REPEAT = 1;

	public static final int TYPE_SHUFFLE = 2;

	public static final int TYPE_STATE = 0;

	private static final int[] IMGIDS_REPEAT = new int[] {
			Theme.IMGID_STATE_REPEAT_ALBUM, Theme.IMGID_STATE_REPEAT_OFF,
			Theme.IMGID_STATE_REPEAT_PL, Theme.IMGID_STATE_REPEAT_PLOB };

	private static final int[] IMGIDS_SHUFFLE = new int[] {
			Theme.IMGID_STATE_SHUFFLE_OFF, Theme.IMGID_STATE_SHUFFLE_ON };

	private static final int[] IMGIDS_STATE = new int[] {
			Theme.IMGID_STATE_GPS_ERROR, Theme.IMGID_STATE_GPS_OFF,
			Theme.IMGID_STATE_GPS_PAUSE, Theme.IMGID_STATE_GPS_PLAY,
			Theme.IMGID_STATE_GPS_PROBLEM, Theme.IMGID_STATE_GPS_SRVOFF,
			Theme.IMGID_STATE_GPS_STOP, Theme.IMGID_STATE_GPS_PROBLEM };

	private static final int[] VALUES_REPEAT = new int[] {
			State.REPEAT_MODE_ALBUM, State.REPEAT_MODE_NONE,
			State.REPEAT_MODE_PL, State.REPEAT_MODE_PLOB };

	private static final int[] VALUES_SHUFFLE = new int[] {
			State.SHUFFLE_MODE_OFF, State.SHUFFLE_MODE_ON };

	private static final int[] VALUES_STATE = new int[] { State.ST_ERROR,
			State.ST_OFF, State.ST_PAUSE, State.ST_PLAY, State.ST_PROBLEM,
			State.ST_SRVOFF, State.ST_STOP, State.ST_UNKNOWN };

	/**
	 * The images to use to represent a value. The image to use to represent
	 * value {@link #values}<code>[i]</code> is located in
	 * <code>images[i]</code>. Content may change when calling
	 * {@link #initRepresentation()}.
	 */
	private final Image[] images;

	/**
	 * The IDs of the images (as set in {@link Theme}) to use to represent the
	 * several values of this screeny. Gets set once and is dependent of the
	 * screeny's type.
	 */
	private final int[] imgIDs;

	private final int type;

	private int val;

	/**
	 * The values this screeny may represent. Gets set once and is dependent of
	 * the screeny's type.
	 */
	private final int[] values;

	/**
	 * Constructor.
	 * 
	 * @param type
	 *            on of {@link #TYPE_REPEAT}, {@link #TYPE_SHUFFLE} or
	 *            {@link #TYPE_STATE}
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>type</code> has an invalid value
	 */
	public StateSubScreeny(Theme theme, int type) {

		super(theme);

		this.type = type;

		switch (type) {

		case TYPE_REPEAT:
			imgIDs = IMGIDS_REPEAT;
			values = VALUES_REPEAT;
			break;

		case TYPE_SHUFFLE:
			imgIDs = IMGIDS_SHUFFLE;
			values = VALUES_SHUFFLE;
			break;

		case TYPE_STATE:
			imgIDs = IMGIDS_STATE;
			values = VALUES_STATE;
			break;

		default:
			throw new IllegalArgumentException();
		}

		images = new Image[imgIDs.length];

	}

	protected void dataUpdated() {

		State s = (State) data;

		if (s == null) {
			val = 0;
			return;
		}

		switch (type) {

		case TYPE_REPEAT:
			val = s.getRepeatMode();
			break;

		case TYPE_SHUFFLE:
			val = s.getShuffleMode();
			break;

		case TYPE_STATE:
			val = s.getState();
			break;
		}

	}

	protected void initRepresentation() throws ScreenyException {

		for (int i = 0; i < images.length; i++) {
			images[i] = theme.getImg(imgIDs[i]);
		}

		setImage(images[0]);

	}

	protected void updateRepresentation() {

		for (int i = 0; i < values.length; i++) {

			if (val == values[i]) {
				try {
					setImage(images[i]);
				} catch (ScreenyException e) {
					// if the Theme is well formed (i.e. all state images of one
					// type have the same size) this should not happen (already
					// catched in initRepresentation)
				}
				return;
			}
		}
	}

}
