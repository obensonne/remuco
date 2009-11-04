/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2009 by the Remuco team, see AUTHORS.
 *
 *   This file is part of Remuco.
 *
 *   Remuco is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Remuco is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Remuco.  If not, see <http://www.gnu.org/licenses/>.
 *   
 */
package remuco.client.jme.ui.screenies;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import remuco.client.common.data.PlayerInfo;
import remuco.client.common.util.Log;
import remuco.client.jme.ui.IActionListener;
import remuco.client.jme.ui.Theme;
import remuco.client.jme.ui.screens.PlayerScreen;

/**
 * Screenies are the pieces which compose the {@link PlayerScreen}.
 * <p>
 * A screeny represents some data in form of an image (so called
 * <i>representation image</i>). It knows its position and size so it can draw
 * itself (i.e. its representation images) into a {@link Graphics} object via
 * {@link #draw(Graphics)} and give information where to place adjacent
 * screenies (see {@link #getNextX()} and {@link #getNextY()} etc.).
 * <p>
 * A screeny creates its <em>representation image</em> using graphical data from
 * {@link Theme}. So when ever the {@link Theme} changes,
 * {@link #initRepresentation(int, int, int, int)} must get called by classes
 * which use screenies. This method must also get called when a screeny's
 * position or the available screen size changes. This is mostly a consequence
 * of a change in {@link Theme}. Screeny implementations must ensure that the
 * size of their representation images does <em>not</em> change between two
 * calls to {@link #initRepresentation(int, int, int, int)}. On the other side,
 * a screeny user must ensure that this method gets called before the above
 * mentioned <code>update(..)</code> method gets called the first time.
 * <p>
 * <h5>Simple summary:</h5>
 * The input of a screeny is some data and the output is a corresponding image
 * which can be drawn with {@link #draw(Graphics)} into an {@link Graphics}
 * object.
 * <p>
 * <h5>Special types of screenies:</h5>
 * Conceptually there are 2 special types of screenies: container screenies
 * (e.g. {@link StateScreeny}) and generic screenies (e.g. {@link ButtonScreeny}
 * ). Container screenies are screenies which contain screenies themselves.
 * Generic screenies are screenies whose appearance and behavior (pointer
 * handling) is configured by constructor parameters.
 * <p>
 * <h5>Notes for subclasses:</h5>
 * Whenever the data to present by a screeny gets updated,
 * {@link #dataUpdated()} will be called. However this is just for notification
 * and mainly intended for <em>container</em> screenies to forward the data
 * update to sub screenies. The actual updating of the screeny's representation
 * image must be done later, in {@link #updateRepresentation()}.
 */
public abstract class Screeny {

	/** Combination of {@link Graphics#BOTTOM} and {@link Graphics#HCENTER} */
	public static final int BOTTOM_CENTER = Graphics.BOTTOM | Graphics.HCENTER;

	/** Combination of {@link Graphics#BOTTOM} and {@link Graphics#LEFT} */
	public static final int BOTTOM_LEFT = Graphics.BOTTOM | Graphics.LEFT;

	/** Combination of {@link Graphics#BOTTOM} and {@link Graphics#RIGHT} */
	public static final int BOTTOM_RIGHT = Graphics.BOTTOM | Graphics.RIGHT;

	/** Combination of {@link Graphics#TOP} and {@link Graphics#HCENTER} */
	public static final int TOP_CENTER = Graphics.TOP | Graphics.HCENTER;

	/** Combination of {@link Graphics#TOP} and {@link Graphics#LEFT} */
	public static final int TOP_LEFT = Graphics.TOP | Graphics.LEFT;

	/** Combination of {@link Graphics#TOP} and {@link Graphics#RIGHT} */
	public static final int TOP_RIGHT = Graphics.TOP | Graphics.RIGHT;

	/**
	 * The image to set as the representation image in
	 * {@link #initRepresentation()} if a screeny wants to be invisible.
	 * 
	 * @see #setImage(Image)
	 */
	protected static final Image INVISIBLE = Image.createImage(1, 1);

	/** The data to represent (with an image) by a screeny implementation. */
	protected Object data;

	/**
	 * Graphics of the screeny's representation image. <i>Only usable if the
	 * screeny's image (set via {@link #setImage(Image)}) is mutable!</i>
	 */
	protected Graphics g;

	protected final PlayerInfo player;

	protected final Theme theme;

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
	 * @param player
	 *            the player reference may be used by the screeny to adjust its
	 *            representation according to characteristics of the player
	 */
	public Screeny(PlayerInfo player) {
		this.player = player;
		theme = Theme.getInstance();
	}

	/**
	 * Draws the screeny's representation image into the given graphics object
	 * at the position previously specified via
	 * {@link #initRepresentation(int, int, int, int, int)}. The drawn image has
	 * allways the same size between to calls to
	 * {@link #initRepresentation(int, int, int, int, int)}.
	 * 
	 * @param g
	 * 
	 * @throws IllegalStateException
	 *             if {@link #initRepresentation(int, int, int, int, int)} has
	 *             not been called before
	 */
	public final void draw(Graphics g) {

		if (img == null) {
			Log.bug("Mar 19, 2009.8:37:19 PM");
			return;
		}

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
	 * description for param <code>anchor</code> in
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

		img = null;
		initRepresentation();
		if (img == null) {
			Log.bug("Mar 19, 2009.8:42:06 PM");
		}

		representationUpdated = false; // enforce update on next call to draw()

	}

	/**
	 * Implementations may override this method to handle pointer <i>pressed</i>
	 * events and notify corresponding key action events to the given action
	 * listener.
	 * <p>
	 * The method {@link #isInScreeny(int, int)} may be used to check if the
	 * given point is within the screeny.
	 * <p>
	 * Screenies with sub screenies which forward this call to their sub
	 * screenies should care about making the given coordinates relative before
	 * forwarding.
	 */
	public void pointerPressed(int px, int py, IActionListener actionListener) {
	}

	/**
	 * Implementations may override this method to handle pointer
	 * <i>released</i> events and notify corresponding key action events to the
	 * given action listener.
	 * <p>
	 * The method {@link #isInScreeny(int, int)} may be used to check if the
	 * given point is within the screeny.
	 * <p>
	 * Screenies with sub screenies which forward this call to their sub
	 * screenies should care about making the given coordinates relative before
	 * forwarding.
	 */
	public void pointerReleased(int px, int py, IActionListener actionListener) {
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

		try {
			this.data = data;
			dataUpdated();
			representationUpdated = false;
		} catch (Exception e) {
			Log.bug("Mar 16, 2009.9:29:20 PM", e);
		}

	}

	/**
	 * May get overridden by implementations if they have to do anything if
	 * {@link #data} has been updated. This is especially useful for screenies
	 * which contain sub screenies to call the {@link #updateData(Object)}
	 * methods of the sub screenies. Notice that representation updates get
	 * triggered separately via {@link #updateRepresentation()}!
	 * <p>
	 * The default implementation does nothing.
	 */
	protected void dataUpdated() {

	}

	/**
	 * Draw border images in this screeny. Uses {@link #width} and
	 * {@link #height} for that task, so these values should be up to date.
	 * 
	 * @param bNW
	 * @param bN
	 * @param bNE
	 * @param bW
	 * @param bE
	 * @param bSW
	 * @param bS
	 * @param bSE
	 * @param fill
	 * 
	 * @return a 4 element integer array describing the area which may be used
	 *         for content inside the borders (first 2 elements are x and y
	 *         position, second 2 elements are width and height)
	 */
	protected int[] drawBorders(Image bNW, Image bN, Image bNE, Image bW,
			Image bE, Image bSW, Image bS, Image bSE, int bgColor) {

		int xStep, yStep;

		// fill with background color

		g.setColor(bgColor);
		g.fillRect(0, 0, width, height);

		// draw top border and corners

		g.drawImage(bNW, 0, 0, TOP_LEFT);

		xStep = bN.getWidth();

		for (int x = bNW.getWidth(); x < width; x += xStep) {
			g.drawImage(bN, x, 0, TOP_LEFT);
		}

		g.drawImage(bNE, width, 0, TOP_RIGHT);

		// draw side borders

		yStep = bW.getHeight();

		for (int y = bNW.getHeight(); y < height; y += yStep) {
			g.drawImage(bW, 0, y, TOP_LEFT);
		}

		yStep = bE.getHeight();

		for (int y = bNE.getHeight(); y < height; y += yStep) {
			g.drawImage(bE, width, y, TOP_RIGHT);
		}

		// draw bottom border and corners

		g.drawImage(bSW, 0, height, BOTTOM_LEFT);

		xStep = bS.getWidth();

		for (int x = bSW.getWidth(); x < width; x += xStep) {
			g.drawImage(bS, x, height, BOTTOM_LEFT);
		}

		g.drawImage(bSE, width, height, BOTTOM_RIGHT);

		final int clip[] = new int[4];

		clip[0] = bW.getWidth();
		clip[1] = bN.getHeight();
		clip[2] = width - bW.getWidth() - bE.getWidth();
		clip[3] = height - bN.getHeight() - bS.getHeight();

		return clip;
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

	/** Check if the given point is inside the area of this screeny. */
	protected boolean isInScreeny(int px, int py) {

		boolean isIn = true;

		isIn &= px >= getPreviousX(); // right anchored -> '>='
		isIn &= px < getNextX();
		isIn &= py >= getPreviousY(); // bottom anchored -> '>='
		isIn &= py < getNextY();

		return isIn;
	}

	/**
	 * To be used by {@link Screeny} implementations to set their
	 * <em>representation
	 * image</em>. This must be called at least once in the implemenation of
	 * {@link #initRepresentation()}. Subsequent, implementations can call this
	 * to update their <em>representation image</em> (in this case <em>i</em>
	 * must have the same widht and height as the image set within
	 * {@link #initRepresentation()}). Alternatively, if <em>i</em> is mutable,
	 * implementations can make use of {@link #g}, which is the {@link Graphics}
	 * object of <em>i</em>, to update the <em>representation image</em>
	 * 
	 * @param i
	 *            the screeny's representation image
	 * 
	 * @throws ScreenyException
	 *             if the image is too large, i.e. it exceeds {@link #width} and
	 *             {@link #height}
	 */
	protected final void setImage(Image i) throws ScreenyException {

		img = i;

		if (img == INVISIBLE) {

			width = 0;
			height = 0;

		} else {

			if (i.getWidth() > width)
				throw new ScreenyException("screeny too width, have " + width
						+ ", need " + i.getWidth());

			if (i.getHeight() > height)
				throw new ScreenyException("screeny too high, have " + height
						+ ", need " + i.getHeight());

			width = i.getWidth();
			height = i.getHeight();

			if (img.isMutable()) {
				g = img.getGraphics();
			} else {
				g = null;
			}

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

}
