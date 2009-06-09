package remuco.ui.screenies;

import javax.microedition.lcdui.Image;

import remuco.player.PlayerInfo;
import remuco.ui.IActionListener;
import remuco.ui.KeyBindings;
import remuco.util.Log;

public class ButtonScreeny extends Screeny {

	private static final Object DUMMY_VALUES[] = { new Object() };

	private final int action;

	private final int imageIDs[];

	private final Image images[];

	private boolean pressed = false;

	private final Object values[];

	/**
	 * Create a button screeny which has always the same button image.
	 * 
	 * @param player
	 * @param imgID
	 *            ID of the image to use as button image
	 * @param action
	 *            action (from {@link KeyBindings}) to issue when this button is
	 *            pressed/released
	 */
	public ButtonScreeny(PlayerInfo player, int imgID, int action) {
		this(player, DUMMY_VALUES, new int[] { imgID }, action);
	}

	/**
	 * Create a button screeny which has varying button images, depending on
	 * values represented by the button.
	 * 
	 * @param player
	 * @param values
	 *            values to represent by this button's image (must contain all
	 *            values potentially set by {@link #updateData(Object)})
	 * @param imgageIDs
	 *            IDs of the images corresponding to the values
	 * @param action
	 *            action (from {@link KeyBindings}) to issue when this button is
	 *            pressed/released
	 * @throws IllegalArgumentException
	 *             if <em>values</em> and <em>imageIDs</em> are empty or differ
	 *             in length
	 */
	public ButtonScreeny(PlayerInfo player, Object values[], int imgageIDs[],
			int action) {
		super(player);
		if (values.length != imgageIDs.length || values.length == 0) {
			throw new IllegalArgumentException();
		}
		this.values = values;
		this.imageIDs = imgageIDs;
		this.images = new Image[imgageIDs.length];
		this.action = action;
	}

	public void pointerPressed(int px, int py, IActionListener actionListener) {
		if (!isInScreeny(px, py)) {
			return;
		}
		pressed = true;
		actionListener.handleActionPressed(this.action);
	}

	public void pointerReleased(int px, int py, IActionListener actionListener) {
		if (!pressed) {
			return;
		}
		pressed = false;
		actionListener.handleActionReleased(this.action);
	}

	protected void initRepresentation() throws ScreenyException {

		for (int i = 0; i < imageIDs.length; i++) {
			images[i] = theme.getImg(imageIDs[i]);
		}
		setImage(images[0]);
	}

	protected void updateRepresentation() {

		if (values.length == 1) {
			return;
		}

		for (int i = 0; i < values.length; i++) {
			if (values[i].equals(data)) {
				try {
					setImage(images[i]);
				} catch (ScreenyException e) {
					// does not happen on well formed theme
				}
				return;
			}
		}
		Log.bug("unexpected value: " + data);
	}

}
