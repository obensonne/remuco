package remuco.ui;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import remuco.Config;
import remuco.Remuco;
import remuco.util.Log;
import remuco.util.Tools;

public final class Theme {

	// ////////////////////////////////////////////////////////////////////////
	// static part
	// ////////////////////////////////////////////////////////////////////////

	/** Color ID */
	public static final byte COLOR_BG = 0, COLOR_TEXT = 1, COLOR_TITLE = 2,
			COLOR_ARTIST = 3, COLOR_ALBUM = 4, COLORS_COUNT = 5;

	/** Large font */
	public static final Font FONT_LARGE = Font.getFont(Font.FACE_PROPORTIONAL,
			Font.STYLE_PLAIN, Font.SIZE_LARGE);

	/** Normal font */
	public static final Font FONT_NORMAL = Font.getFont(Font.FACE_PROPORTIONAL,
			Font.STYLE_PLAIN, Font.SIZE_MEDIUM);

	/** Small font */
	public static final Font FONT_SMALL = Font.getFont(Font.FACE_PROPORTIONAL,
			Font.STYLE_PLAIN, Font.SIZE_SMALL);

	/** Font for a plob's title */
	public static final Font FONT_TITLE = Font.getFont(Font.FACE_PROPORTIONAL,
			Font.STYLE_BOLD, Font.SIZE_LARGE);

	/** Font for a plob's album */
	public static final Font FONT_ALBUM = FONT_SMALL;

	/** Font for a plob's artist */
	public static final Font FONT_ARTIST = FONT_NORMAL;

	/** Image ID */
	public static final byte IMGID_PLOB_BORDER_TOP = 0,
			IMGID_PLOB_BORDER_BOTTOM = 1, IMGID_PLOB_BORDER_LEFT = 2,
			IMGID_PLOB_BORDER_RIGHT = 3, IMGID_PLOB_CORNER_TOP_LEFT = 4,
			IMGID_PLOB_CORNER_TOP_RIGHT = 5, IMGID_PLOB_CORNER_BOTTOM_LEFT = 6,
			IMGID_PLOB_CORNER_BOTTOM_RIGHT = 7, IMGID_STATE_REPEAT_OFF = 8,
			IMGID_STATE_REPEAT_ON = 9, IMGID_STATE_SHUFFLE_OFF = 10,
			IMGID_STATE_SHUFFLE_ON = 11, IMGID_STATE_VOLUME_LEFT = 12,
			IMGID_STATE_VOLUME_RIGHT = 13, IMGID_STATE_VOLUME_OFF = 14,
			IMGID_STATE_VOLUME_ON = 15, IMGID_STATE_PLAYBACK_PLAY = 16,
			IMGID_STATE_PLAYBACK_PAUSE = 17, IMGID_STATE_PLAYBACK_STOP = 18,
			IMGID_STATE_SPACER = 19, IMGID_STATE_BORDER_LEFT = 20,
			IMGID_STATE_BORDER_RIGHT = 21, IMGID_PLOB_RATE_OFF = 22,
			IMGID_PLOB_RATE_ON = 23, IMGID_COLORS = 24;

	private static final String DEFAULT = "Korama";

	private static final Image imgFallBack = createFallBackImage();

	private static final String[] imgName = { "plob.border-top.png",
			"plob.border-bottom.png", "plob.border-left.png",
			"plob.border-right.png", "plob.corner-top-left.png",
			"plob.corner-top-right.png", "plob.corner-bottom-left.png",
			"plob.corner-bottom-right.png", "state.repeat-off.png",
			"state.repeat-on.png", "state.shuffle-off.png",
			"state.shuffle-on.png", "state.volume-left.png",
			"state.volume-right.png", "state.volume-off.png",
			"state.volume-on.png", "state.playback-play.png",
			"state.playback-pause.png", "state.playback-stop.png",
			"state.spacer.png", "state.border-left.png",
			"state.border-right.png", "plob.rate-off.png", "plob.rate-on.png",
			"colors.png" };

	private static String[] list = null;

	public static Font getBestFontForHeight(int h, int buffer) {

		if (FONT_LARGE.getHeight() <= h - 2 * buffer)
			return FONT_LARGE;

		if (FONT_NORMAL.getHeight() <= h - 2 * buffer)
			return FONT_LARGE;

		return FONT_SMALL;
	}

	public static Image getFallBackImage() {
		return imgFallBack;
	}

	/**
	 * Get a list of available Themes.
	 * 
	 * @return string array with the names of available themes
	 * 
	 */
	public static String[] getList() {

		String themes;

		if (list != null)
			return list;

		themes = Config.getApplicationProperty(Config.APP_PROP_THEMES);

		if (themes == null) {

			list = new String[] { DEFAULT };

		} else {

			list = Tools.splitString(themes, ",");
			if (list.length == 0)
				list = new String[] { DEFAULT };
		}

		return list;
	}

	/**
	 * Scale an image. The width and height get scaled by the factor
	 * <code>numerator/denominator</code>.
	 * 
	 * @param img
	 *            the image to get a scaled copy from
	 * @param numerator
	 * @param denominator
	 * 
	 * @return a scaled immutable copy of the source image with
	 *         <code>widthScaled = widthSource * numerator / denominator</code>
	 *         and
	 *         <code>heightScaled = heightSource * numerator / denominator</code>
	 *         or the same image if <code>numerator == denominator</code>
	 */
	public static Image scaleImage(Image img, int numerator, int denominator) {

		int[] rgb, rgbSchrink;
		int w, h, wOrig, yScaled, yOrig;

		if (numerator == denominator)
			return img;

		try {

			rgb = new int[img.getWidth() * img.getHeight()];
			img.getRGB(rgb, 0, img.getWidth(), 0, 0, img.getWidth(), img
					.getHeight());

			wOrig = img.getWidth();
			w = wOrig * numerator / denominator;
			h = img.getHeight() * numerator / denominator;
			rgbSchrink = new int[w * h];

			for (int y = 0; y < h; y++) {
				yOrig = y * denominator / numerator * wOrig;
				yScaled = y * w;
				for (int x = 0; x < w; x++) {
					rgbSchrink[x + yScaled] = rgb[x * denominator / numerator
							+ yOrig];
				}
			}

			return Image.createRGBImage(rgbSchrink, w, h, false);

		} catch (Exception e) {
			Log.ln("[TH] shrink implementation error", e);
			return img;
		}

	}

	// ////////////////////////////////////////////////////////////////////////
	// instance part
	// ////////////////////////////////////////////////////////////////////////

	/**
	 * Shrinks an image if its width or height exceeds the boundaries given by
	 * <code>maxWidth</code> and <code>maxHeight</code>.
	 * 
	 * @param img
	 *            the image to shrink
	 * @param maxWidth
	 *            the maximum allowed width of the image
	 * @param maxHeight
	 *            the maximum allowed height of the image
	 * @return a shrunk copy of the image (<i>immutable!</i>) or
	 *         <code>img</code> if shrinking is not needed
	 */
	public static Image shrinkImageIfNeeded(Image img, int maxWidth,
			int maxHeight) {

		if (img.getHeight() > maxHeight)
			img = Theme.scaleImage(img, 10 * maxHeight / img.getHeight(), 10);
		if (img.getWidth() > maxWidth)
			img = Theme.scaleImage(img, 10 * maxWidth / img.getWidth(), 10);

		return img;

	}

	/**
	 * Split a one-line string to multi-line string, depending on available
	 * width and font size.
	 * 
	 * @param s
	 *            the string to split
	 * @param maxWidth
	 *            available width for strings
	 * @param f
	 *            font to use to calculate width of strings
	 * @return a string array with every string not exceeding <code>width</code>
	 *         when displayed in font <code>f</code>
	 */
	public static String[] splitString(String s, int maxWidth, Font f) {

		if (f.stringWidth(s) <= maxWidth) {
			return new String[] { s };
		}

		int w, slen, i, n, goodBreakPos;

		String cSpace = " ", cCurrent;

		String[] sa;

		Vector v = new Vector(3);

		w = f.stringWidth(s);

		while ((slen = s.length()) > 0) {

			goodBreakPos = 0;
			i = 1;
			w = 0;
			while (w < maxWidth && i < slen) {
				cCurrent = s.substring(i, i + 1);
				if (cCurrent.equals(cSpace))
					goodBreakPos = i;
				w = f.substringWidth(s, 0, i);
				i++;
			}
			if (w >= maxWidth) {
				if (goodBreakPos > 0) {
					v.addElement(s.substring(0, goodBreakPos));
					s = s.substring(goodBreakPos < slen - 1 ? goodBreakPos + 1
							: goodBreakPos);
				} else {
					v.addElement(s.substring(0, i - 1));
					s = s.substring(i - 1);
				}
			} else {
				break;
			}
		}

		if (slen > 0) {
			v.addElement(s);
		}

		n = v.size();
		sa = new String[n];
		for (i = 0; i < n; i++) {
			sa[i] = (String) v.elementAt(i);
		}

		return sa;

	}

	/**
	 * @emulator Only used for testing!
	 * @param imgs
	 * @param checkWidth
	 * @param checkHeight
	 */
	private static boolean checkSizesEqual(Vector imgs, boolean checkWidth,
			boolean checkHeight) {

		Image i;
		int w1, w2, h1, h2;
		Enumeration enu;
		boolean ok = true;

		enu = imgs.elements();

		if (!enu.hasMoreElements())
			return ok;

		i = (Image) enu.nextElement();
		w1 = i.getWidth();
		h1 = i.getHeight();

		while (enu.hasMoreElements()) {

			i = (Image) enu.nextElement();
			w2 = i.getWidth();
			h2 = i.getHeight();

			if (checkWidth && w1 != w2) {
				Log.ln("[TH] VALIDATION:     width differs");
				ok = false;
			}
			if (checkHeight && h1 != h2) {
				Log.ln("[TH] VALIDATION:     height differs");
				ok = false;
			}

			w1 = w2;
			h1 = h2;
		}

		return ok;
	}

	private static Image createFallBackImage() {

		Image i = Image.createImage(5, 5);

		Graphics g = i.getGraphics();

		g.setColor(0);
		g.drawLine(0, 0, 20 - 1, 20 - 1);
		g.drawLine(20 - 1, 0, 0, 20 - 1);

		return i;
	}

	private final int[] colors = new int[COLORS_COUNT];

	private String current;

	private final Image[] img = new Image[imgName.length];

	/**
	 * Create a new theme using the default theme ({@value #DEFAULT}).
	 */
	public Theme() {
		this(DEFAULT);
	}

	/**
	 * Create a new theme. If <code>name</code> is <code>null</code> or not
	 * found in the list of available themes (see {@link #getList()}) then the
	 * default theme ({@value #DEFAULT}) is used.
	 */
	public Theme(String name) {
		load(name);
	}

	/**
	 * Get a specific color of this theme.
	 * 
	 * @param id
	 *            the color id, one of <code>COLOR_...</code>
	 * @return the color value
	 */
	public int getColor(int id) {
		return colors[id];
	}

	/**
	 * Get a specific image of this theme.
	 * 
	 * @param id
	 *            the image id, one of <code>IMIGID_...</code>
	 * @return the image
	 */
	public Image getImg(int id) {
		return img[id];
	}

	/**
	 * Get the name of the currently loaded theme.
	 * 
	 * @return the name or <code>null</code> if no theme has been loaded yet
	 */
	public String getName() {
		return current;
	}

	/**
	 * Load a theme.
	 * 
	 * @param name
	 *            theme name (may be <code>null</code> - in this case the
	 *            default theme gets loaded)
	 */
	protected void load(String name) {

		if (name == null)
			name = DEFAULT;

		if (current == null || !current.equals(name))
			loadPrivate(name);

		Log.ln("[TH] loaded theme " + name);

	}

	/**
	 * @emulator Only used for testing!
	 */
	private boolean checkSizes() {

		Vector v = new Vector(30);
		boolean ok = true;

		Log.ln("[TH] VALIDATION: "
				+ "check plob area borders/corners for same size");

		v.addElement(img[IMGID_PLOB_BORDER_BOTTOM]);
		v.addElement(img[IMGID_PLOB_BORDER_LEFT]);
		v.addElement(img[IMGID_PLOB_BORDER_RIGHT]);
		v.addElement(img[IMGID_PLOB_BORDER_TOP]);
		v.addElement(img[IMGID_PLOB_CORNER_BOTTOM_LEFT]);
		v.addElement(img[IMGID_PLOB_CORNER_BOTTOM_RIGHT]);
		v.addElement(img[IMGID_PLOB_CORNER_TOP_LEFT]);
		v.addElement(img[IMGID_PLOB_CORNER_TOP_RIGHT]);

		ok &= checkSizesEqual(v, true, true);

		v.removeAllElements();

		Log.ln("[TH] VALIDATION: check state area images for same height");

		v.addElement(img[IMGID_STATE_BORDER_LEFT]);
		v.addElement(img[IMGID_STATE_BORDER_RIGHT]);
		v.addElement(img[IMGID_STATE_PLAYBACK_PAUSE]);
		v.addElement(img[IMGID_STATE_PLAYBACK_PLAY]);
		v.addElement(img[IMGID_STATE_PLAYBACK_STOP]);
		v.addElement(img[IMGID_STATE_REPEAT_OFF]);
		v.addElement(img[IMGID_STATE_REPEAT_ON]);
		v.addElement(img[IMGID_STATE_SHUFFLE_OFF]);
		v.addElement(img[IMGID_STATE_SHUFFLE_ON]);
		v.addElement(img[IMGID_STATE_VOLUME_LEFT]);
		v.addElement(img[IMGID_STATE_VOLUME_OFF]);
		v.addElement(img[IMGID_STATE_VOLUME_ON]);
		v.addElement(img[IMGID_STATE_VOLUME_RIGHT]);

		ok &= checkSizesEqual(v, false, true);

		v.removeAllElements();

		Log.ln("[TH] VALIDATION: check state-gps images for same width");

		v.addElement(img[IMGID_STATE_PLAYBACK_PAUSE]);
		v.addElement(img[IMGID_STATE_PLAYBACK_PLAY]);
		v.addElement(img[IMGID_STATE_PLAYBACK_STOP]);

		ok &= checkSizesEqual(v, false, true);

		v.removeAllElements();

		Log.ln("[TH] VALIDATION: check state-repeat images for same width");

		v.addElement(img[IMGID_STATE_REPEAT_OFF]);
		v.addElement(img[IMGID_STATE_REPEAT_ON]);

		ok &= checkSizesEqual(v, false, true);

		v.removeAllElements();

		Log.ln("[TH] VALIDATION: check state-shuffle images for same width");

		v.addElement(img[IMGID_STATE_SHUFFLE_OFF]);
		v.addElement(img[IMGID_STATE_SHUFFLE_ON]);

		ok &= checkSizesEqual(v, false, true);

		v.removeAllElements();

		Log.ln("[TH] VALIDATION: check state-volume-bar images");

		if (img[IMGID_STATE_VOLUME_OFF].getWidth() != 1) {
			Log.ln("[TH] VALIDATION:     "
					+ "volume bar image (on/off) must have 1px width!");
			ok = false;
		}
		v.addElement(img[IMGID_STATE_VOLUME_OFF]);
		v.addElement(img[IMGID_STATE_VOLUME_ON]);

		ok &= checkSizesEqual(v, false, true);

		v.removeAllElements();

		Log.ln("[TH] VALIDATION: check size of color image");

		if (img[IMGID_COLORS].getWidth() != COLORS_COUNT) {
			Log.ln("[TH] VALIDATION:     color image must have" + COLORS_COUNT
					+ "px width!");
			ok = false;
		}
		if (img[IMGID_COLORS].getHeight() != 1) {
			Log.ln("[TH] VALIDATION:     color image must have 1px width!");
			ok = false;
		}

		return ok;

	}

	private void loadPrivate(String name) {

		int i;
		boolean ok = true;
		boolean existent = false;

		getList();

		for (int j = 0; j < list.length; j++) {
			if (name.equals(list[j])) {
				existent = true;
				break;
			}
		}

		if (existent)
			current = name;
		else {
			Log.ln("[TH] Theme " + name + " not found, using " + DEFAULT);
			current = DEFAULT;
		}

		for (i = 0; i < img.length; i++) {
			try {
				img[i] = Image.createImage("/themes/" + current + "/"
						+ imgName[i]);
			} catch (IOException e) {
				Log.ln("[TH] VALIDATION: missing image " + imgName[i]);
				img[i] = imgFallBack;
				ok = false;
			}
		}

		if (ok && Remuco.EMULATION) {
			ok &= checkSizes();
		}

		if (ok)
			img[IMGID_COLORS].getRGB(colors, 0, COLORS_COUNT, 0, 0,
					COLORS_COUNT, 1);
		else {
			Log.ln("[TH] VALIDATION: FAILED !!!");
			colors[COLOR_BG] = 0xFF33AA;
		}

	}

}
