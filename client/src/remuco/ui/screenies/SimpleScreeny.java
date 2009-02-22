package remuco.ui.screenies;

import javax.microedition.lcdui.Image;

import remuco.player.PlayerInfo;
import remuco.player.State;
import remuco.ui.Theme;
import remuco.util.Log;

/**
 * A simple screeny represents integer values with a specific immutable image
 * for each defined value. It is used to display certain elements of a
 * {@link State}.
 */
public final class SimpleScreeny extends Screeny {

	public static final int TYPE_REPEAT = 1;

	public static final int TYPE_SHUFFLE = 2;

	public static final int TYPE_PLAYBACK = 0;

	private static final int VALUE_REPEAT_OFF = 0;
	private static final int VALUE_REPEAT_ON = 1;
	private static final int VALUE_SHUFFLE_OFF = 0;
	private static final int VALUE_SHUFFLE_ON = 1;

	private static final int[] IMGIDS_REPEAT = new int[] {
			Theme.IMGID_STATE_REPEAT_OFF, Theme.IMGID_STATE_REPEAT_ON };

	private static final int[] IMGIDS_SHUFFLE = new int[] {
			Theme.IMGID_STATE_SHUFFLE_OFF, Theme.IMGID_STATE_SHUFFLE_ON };

	private static final int[] IMGIDS_PLAYBACK = new int[] {
			Theme.IMGID_STATE_PLAYBACK_STOP, Theme.IMGID_STATE_PLAYBACK_PAUSE,
			Theme.IMGID_STATE_PLAYBACK_PLAY };

	private static final int[] VALUES_REPEAT = new int[] { VALUE_REPEAT_OFF,
			VALUE_REPEAT_ON };

	private static final int[] VALUES_SHUFFLE = new int[] { VALUE_SHUFFLE_OFF,
			VALUE_SHUFFLE_ON };

	private static final int[] VALUES_PLAYBACK = new int[] {
			State.PLAYBACK_STOP, State.PLAYBACK_PAUSE, State.PLAYBACK_PLAY };

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
	 *            {@link #TYPE_PLAYBACK}
	 * 
	 * @throws IllegalArgumentException
	 *             if <code>type</code> has an invalid value
	 */
	public SimpleScreeny(PlayerInfo player, int type) {

		super(player);

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

		case TYPE_PLAYBACK:
			imgIDs = IMGIDS_PLAYBACK;
			values = VALUES_PLAYBACK;
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
			val = s.isRepeat() ? VALUE_REPEAT_ON : VALUE_REPEAT_OFF;
			break;

		case TYPE_SHUFFLE:
			val = s.isShuffle() ? VALUE_SHUFFLE_ON : VALUE_SHUFFLE_OFF;
			break;

		case TYPE_PLAYBACK:
			val = s.getPlayback();
			break;

		default:
			Log.asssertNotReached(this);
			break;
		}

	}

	protected void initRepresentation() throws ScreenyException {

		if (type == TYPE_PLAYBACK && !player.supportsPlaybackStatus()) {
			setImage(INVISIBLE);
			return;
		}

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
