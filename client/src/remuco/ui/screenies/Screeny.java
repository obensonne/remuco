package remuco.ui.screenies;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import remuco.player.IPlayerInfo;
import remuco.ui.Theme;

/**
 * Screenies represent some data in form of an image (so called
 * <i>representation image</i>). Screenies know their position and size so they
 * can draw themselves (i.e. their representation images) into a
 * {@link Graphics} object via {@link #draw(Graphics)} and give information
 * where to place adjacent screenies (see {@link #getNextX()} and
 * {@link #getNextY()} etc.).
 * <p>
 * Screenies create their representation images using graphical data from
 * {@link Theme}. So when ever the {@link Theme} changes,
 * {@link #initRepresentation(int, int, int, int)} must get called by classes
 * which use screenies. This method must also get called when a screenies
 * position or the available screen size changes. This is mostly a consequence
 * of a change in {@link Theme}. Screeny implementations must ensure that the
 * size of their representation images does <i>not</i> change between two calls
 * to {@link #initRepresentation(int, int, int, int)}. On the other side,
 * screeny users must ensure that this method gets called before the above
 * mentioned <code>update(..)</code> method gets called the first time.
 * <p>
 * Simple summary: The input of a screeny is some data and the output is a
 * corresponding image which can be drawn with {@link #draw(Graphics)} into an
 * {@link Graphics} object.
 * <p>
 * Notes for subclasses:<br>
 * Whenever the data to present by a screeny gets updated,
 * {@link #dataUpdated()} will be called. However this is just for notification,
 * the actual updating of the screeny's representation image must be done in
 * {@link #updateRepresentation()}.
 * 
 * @author Christian Buennig
 * 
 */
public abstract class Screeny {

	/** Used often .. */
	public static final int TOP_LEFT = Graphics.TOP | Graphics.LEFT;

	/** Used often .. */
	public static final int TOP_RIGHT = Graphics.TOP | Graphics.RIGHT;

	/** The data to represent (with an image) by a screeny implementation. */
	protected Object data;

	/**
	 * Graphics of the screeny's representation image. <i>Only usable if the
	 * screeny's image (set via {@link #setImage(Image)}) is mutable!</i>
	 */
	protected Graphics g;

	protected final IPlayerInfo player;

	protected final Theme theme;

	/**
	 * The image to set as the representation image in
	 * {@link #initRepresentation()} if a screeny wants to be invisible.
	 * 
	 * @see #setImage(Image)
	 */
	protected static final Image INVISIBLE = Image.createImage(1, 1);

	/**
	 * Size of the representation image. When entering the method
	 * {@link #initRepresentation()} this value specifies the maximum size that
	 * may be used by the representation image. This value gets updated
	 * automatically to the image's true size when {@link #setImage(Image)} gets
	 * called.
	 */
	protected int width, height;

	/** The screeny's representation image */
	private Image img;

	/**
	 * Indicates if the screeny's representation image has allready been updated
	 * after a call to {@link #updateData(Object)}.
	 */
	private boolean representationUpdated = false;

	/** Position of the screeny */
	private int x, y, anchor;

	/**
	 * Create a new screeny.
	 * 
	 * @param theme
	 *            the theme to get theme data from - this theme reference must
	 *            be valid the whole lifetime of the screeny, but its contents
	 *            may change (in this case the class that uses the screeny must
	 *            call {@link #initRepresentation(int, int, int, int, int)})
	 * @param player
	 *            the player reference may be used by the screeny to adjust its
	 *            representation according to characteristics of the player -
	 *            this player reference must be valid the whole lifetime of the
	 *            screeny, but its contents may change (in this case the class
	 *            that uses the screeny must call
	 *            {@link #initRepresentation(int, int, int, int, int)})
	 */
	public Screeny(Theme theme, IPlayerInfo player) {
		this.theme = theme;
		this.player = player;
	}

	/**
	 * Draws the screeny's representation image into the given graphics object
	 * at the position previously specified via
	 * {@link #initRepresentation(int, int, int, int, int)}. The drawn image
	 * has allways the same size between to calls to
	 * {@link #initRepresentation(int, int, int, int, int)}.
	 * 
	 * @param g
	 * 
	 * @throws IllegalStateException
	 *             if {@link #initRepresentation(int, int, int, int, int)} has
	 *             not been called before
	 */
	public final void draw(Graphics g) {

		checkState();

		if (img == INVISIBLE)
			return;

		if (!representationUpdated) {
			updateRepresentation();
			representationUpdated = true;
		}

		g.drawImage(img, x, y, anchor);
	}

	/**
	 * Get the position of this screeny.
	 * <p>
	 * Does not change between 2 calls to
	 * {@link #initRepresentation(int, int, int, int, int)}.
	 */
	public int getAnchor() {
		return anchor;
	}

	/**
	 * Get the size of this screeny.
	 * <p>
	 * Does not change between 2 calls to
	 * {@link #initRepresentation(int, int, int, int, int)}.
	 */
	public final int getHeight() {
		return height;
	}

	// /**
	// * Get the screeny's representation image.
	// */
	// public final Image getImage() {
	// return img;
	// }

	/**
	 * Get the (left anchored) possible position of a screeny right to this one
	 * (to be used with an anchor point {@link Graphics#LEFT}).
	 * <p>
	 * Does not change between 2 calls to
	 * {@link #initRepresentation(int, int, int, int, int)}.
	 */
	public final int getNextX() {
		if ((anchor & Graphics.LEFT) != 0)
			return x + width;
		else if ((anchor & Graphics.RIGHT) != 0)
			return x;
		else
			return x + width / 2;
	}

	/**
	 * Get the (top anchored) possible position of a screeny below this one. (to
	 * be used with an anchor point {@link Graphics#TOP}).
	 * <p>
	 * Does not change between 2 calls to
	 * {@link #initRepresentation(int, int, int, int, int)}.
	 */
	public final int getNextY() {
		if ((anchor & Graphics.TOP) != 0)
			return y + height;
		else if ((anchor & Graphics.BOTTOM) != 0)
			return y;
		else
			return y + height / 2;
	}

	/**
	 * Get the (right anchored) possible position of a screeny left of this one
	 * (to be used with an anchor point {@link Graphics#RIGHT}).
	 * <p>
	 * Does not change between 2 calls to
	 * {@link #initRepresentation(int, int, int, int, int)}.
	 */
	public final int getPreviousX() {
		if ((anchor & Graphics.LEFT) != 0)
			return x;
		else if ((anchor & Graphics.RIGHT) != 0)
			return x - width;
		else
			return x - width / 2;
	}

	/**
	 * Get the (bottom anchored) possible position of a screeny above this one
	 * (to be used with an anchor point {@link Graphics#BOTTOM}). See
	 * description for param <code>achnor</code> in
	 * {@link #initRepresentation(int, int, int, int, int)} for when this method
	 * may be used and when not.
	 * <p>
	 * Does not change between 2 calls to
	 * {@link #initRepresentation(int, int, int, int, int)}.
	 */
	public final int getPreviousY() {
		if ((anchor & Graphics.TOP) != 0)
			return y;
		else if ((anchor & Graphics.BOTTOM) != 0)
			return y - height;
		else
			return y - height / 2;
	}

	/**
	 * Get the size of this screeny.
	 * <p>
	 * Does not change between 2 calls to
	 * {@link #initRepresentation(int, int, int, int, int)}.
	 * 
	 */
	public final int getWidth() {
		return width;
	}

	/**
	 * Get the position of this screeny.
	 * <p>
	 * Does not change between 2 calls to
	 * {@link #initRepresentation(int, int, int, int, int)}.
	 */
	public final int getX() {
		return x;
	}

	/**
	 * Get the position of this screeny.
	 * <p>
	 * Does not change between 2 calls to
	 * {@link #initRepresentation(int, int, int, int, int)}.
	 */
	public final int getY() {
		return y;
	}

	/**
	 * Triggers the screeny to reset its representation due to changes in
	 * position, available screen size or theme.
	 * 
	 * @param x
	 *            position
	 * @param y
	 *            position
	 * @param anchor
	 *            anchor of the position ({@link #getNextX()} and
	 *            {@link #getPreviousX()} may be not perfect if
	 *            {@link Graphics#HCENTER} is set, {@link #getNextY()} and
	 *            {@link #getPreviousY()} may not be perfect if
	 *            {@link Graphics#VCENTER} is set, {@link Graphics#BASELINE} is
	 *            not allowed)
	 * @param width
	 *            maximum allowed width of the screeny's representation image
	 *            (the real width may be less and will be accessible via
	 *            {@link #getWidth()})
	 * @param height
	 *            maximum allowed width of the screeny's representation image
	 *            (the real height may be less and will be accessible via
	 *            {@link #getWidth()})
	 * 
	 * @throws ScreenyException
	 *             if the screeny's representation image is to big for the given
	 *             width and height
	 * @throws IllegalArgumentException
	 *             if {@link Graphics#BASELINE} is set in <code>anchor</code>
	 */
	public final void initRepresentation(int x, int y, int anchor, int width,
			int height) throws ScreenyException {

		if ((anchor & Graphics.BASELINE) != 0)
			throw new IllegalArgumentException("invalid anchor");

		this.x = x;
		this.y = y;
		this.anchor = anchor;

		this.width = width;
		this.height = height;

		initRepresentation();

		representationUpdated = false; // to enforce representation update on
		// next call to #draw(..)

	}

	public final String toString() {

		StringBuffer sb = new StringBuffer("Screeny: ");
		sb.append("x=").append(x).append(",y=").append(y);
		sb.append(",width=").append(width).append(",height=").append(height);
		if (img == null)
			sb.append(" (uninitialized)");
		else
			sb.append(" (initialized)");

		return sb.toString();
	}

	public final void updateData(Object data) {

		this.data = data;
		dataUpdated();
		representationUpdated = false;

	}

	/**
	 * May get overridden by implementations if they have to do anything if
	 * {@link #data} has been updated. This is especially useful for screenies
	 * which contain sub screenies to call the {@link #updateData(Object)}
	 * methods of the sub screenies. Notice that representation updates get
	 * triggered seperately via {@link #updateRepresentation()}!
	 * <p>
	 * The default implementation does nothing.
	 */
	protected void dataUpdated() {

	}

	/**
	 * Called by {@link #initRepresentation(int, int, int, int, int)} to trigger
	 * a screeny implementation to update its representation due to changes in
	 * position, possible size or theme.
	 * <p>
	 * Note: Between 2 calls of this method it is expected that the screeny's
	 * representation image (setted via {@link #setImage(Image)}) has allways
	 * the same size!
	 * 
	 * @throws ScreenyException
	 *             see {@link #initRepresentation(int, int, int, int, int)}
	 */
	protected abstract void initRepresentation() throws ScreenyException;

	/**
	 * To be used by {@link Screeny} implementations to set their representation
	 * image once they have updated it due to a call to
	 * {@link #initRepresentation()} or an update of the data they represent.
	 * 
	 * @param i
	 *            the screeny's representation image
	 * 
	 * @throws ScreenyException
	 *             if the image is too large, i.e. it exceeds its possible
	 *             maximum witdth and/or height previously specified via
	 *             {@link #initRepresentation(int, int, int, int, int)}.
	 */
	protected final void setImage(Image i) throws ScreenyException {

		img = i;

		if (img == INVISIBLE) {

			width = 0;
			height = 0;

		} else {

			if (i.getWidth() > width)
				throw new ScreenyException("screeny too large (width)");

			if (i.getHeight() > height)
				throw new ScreenyException("screeny too alrge (height)");

			width = i.getWidth();
			height = i.getHeight();

			if (img.isMutable())
				g = img.getGraphics();
		}
	}

	/**
	 * Implementations shall here update their representation in dependence of
	 * the data previously set via {@link #updateData(Object)}. If
	 * {@link #updateData(Object)} has not been called before, the
	 * implementation shall fall back to some dummy or default data. When this
	 * method gets called, it is ensured that {@link #initRepresentation()} has
	 * been called before, so no init check is needed by implementations.
	 * 
	 */
	protected abstract void updateRepresentation();

	/**
	 * Checks if the screeny has been initialized, i.e.
	 * {@link #initRepresentation(int, int, int, int)} has been called at least
	 * once.
	 * 
	 * @throws IllegalStateException
	 *             if the screeny has not yet been initialized
	 */
	private final void checkState() {

		if (img == null)
			throw new IllegalStateException("screeny not yet intialized");

	}

}
