/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2009 Oben Sonne <obensonne@googlemail.com>
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
package remuco.ui;

import java.io.IOException;
import java.util.Vector;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import remuco.Config;
import remuco.ui.screenies.Screeny;
import remuco.ui.screenies.ScreenyException;
import remuco.util.Log;

public final class Theme {

	/** Large font */
	public static final Font FONT_LARGE = Font.getFont(Font.FACE_PROPORTIONAL,
		Font.STYLE_PLAIN, Font.SIZE_LARGE);

	/** Normal font */
	public static final Font FONT_NORMAL = Font.getFont(Font.FACE_PROPORTIONAL,
		Font.STYLE_PLAIN, Font.SIZE_MEDIUM);

	/** Small font */
	public static final Font FONT_SMALL = Font.getFont(Font.FACE_PROPORTIONAL,
		Font.STYLE_PLAIN, Font.SIZE_SMALL);

	/** Font for an item's album */
	public static final Font FONT_ALBUM = FONT_SMALL;

	/** Font for an item's artist */
	public static final Font FONT_ARTIST = FONT_NORMAL;

	/** Font for progress value */
	public static final Font FONT_PROGRESS = Font.getFont(Font.FACE_MONOSPACE,
		Font.STYLE_PLAIN, Font.SIZE_SMALL);

	/** Font for an item's title */
	public static final Font FONT_TITLE = Font.getFont(Font.FACE_PROPORTIONAL,
		Font.STYLE_BOLD, Font.SIZE_LARGE);

	/** Font for a volume level */
	public static final Font FONT_VOLUME = Font.getFont(Font.FACE_MONOSPACE,
		Font.STYLE_PLAIN, Font.SIZE_LARGE);

	public static final int LINE_GAP = FONT_SMALL.getHeight() / 2;

	/**
	 * Theme element ID (<code>RTE_..</code>) or color ID (<code>RTC_..</code>)
	 */
	public static final byte RTE_BUTTONBAR_LEFT = 0, RTE_BUTTONBAR_SPACER = 1,
			RTE_BUTTONBAR_RIGHT = 2, RTE_BUTTON_FULLSCREEN = 3,
			RTE_BUTTON_NEXT = 4, RTE_BUTTON_PREV = 5, RTE_BUTTON_RATE = 6,
			RTE_BUTTON_TAGS = 7, RTE_STATE_LEFT = 8, RTE_STATE_SPACER = 9,
			RTE_STATE_RIGHT = 10, RTE_BUTTON_PLAYBACK_PAUSE = 11,
			RTE_BUTTON_PLAYBACK_PLAY = 12, RTE_BUTTON_PLAYBACK_STOP = 13,
			RTE_BUTTON_REPEAT_OFF = 14, RTE_BUTTON_REPEAT_ON = 15,
			RTE_BUTTON_SHUFFLE_OFF = 16, RTE_BUTTON_SHUFFLE_ON = 17,
			RTE_SLIDER_VOLUME_LEFT = 18, RTE_SLIDER_VOLUME_OFF = 19,
			RTE_SLIDER_VOLUME_ON = 20, RTE_SLIDER_VOLUME_RIGHT = 21,
			RTC_BG = 22, RTC_TEXT_ALBUM = 23, RTC_TEXT_ARTIST = 24,
			RTC_TEXT_OTHER = 25, RTC_TEXT_TITLE = 26, RTE_ICON_RATING_OFF = 27,
			RTE_ICON_RATING_ON = 28;

	/** Name of the default theme to load. */
	private static final String DEFAULT = "Vilanco";

	private static final Image IMG_FALLBACK;

	/** Theme element file names (without extension) */
	private static final String[] IMG_NAME = {

			// button bar elements
			"rte.buttonbar.left",
			"rte.buttonbar.spacer",
			"rte.buttonbar.right",
			"rte.button.fullscreen",
			"rte.button.next",
			"rte.button.prev",
			"rte.button.rate",
			"rte.button.tags",

			// state bar elements
			"rte.state.left", "rte.state.spacer", "rte.state.right",
			"rte.button.playback.pause", "rte.button.playback.play",
			"rte.button.playback.stop", "rte.button.repeat.off",
			"rte.button.repeat.on", "rte.button.shuffle.off",
			"rte.button.shuffle.on", "rte.slider.volume.left",
			"rte.slider.volume.off", "rte.slider.volume.on",
			"rte.slider.volume.right",

			// colors
			"rte.color.bg", "rte.color.text.album", "rte.color.text.artist",
			"rte.color.text.other", "rte.color.text.title",

			// misc icons
			"rte.icon.rating.off", "rte.icon.rating.on" };

	private static Theme instance = null;

	private static final int LIST_ICON_SIZES[] = new int[] { 12, 16, 22, 24,
			32, 48 };

	static {

		// //// create fall back image for missing theme images //////

		IMG_FALLBACK = Image.createImage(5, 5);

		final Graphics g = IMG_FALLBACK.getGraphics();

		g.setColor(0);
		g.drawLine(0, 0, 20 - 1, 20 - 1);
		g.drawLine(20 - 1, 0, 0, 20 - 1);

	}

	public static Font getBestFontForHeight(int h, int buffer) {

		if (FONT_LARGE.getHeight() <= h - 2 * buffer)
			return FONT_LARGE;

		if (FONT_NORMAL.getHeight() <= h - 2 * buffer)
			return FONT_LARGE;

		return FONT_SMALL;
	}

	/**
	 * Get the singleton theme instance. <em>Must not</em> get called from a
	 * static context!
	 * 
	 * @return the theme
	 */
	public static Theme getInstance() {
		return instance;
	}

	public static void init(Display display) {

		if (instance == null) {
			instance = new Theme(display);
		}

	}

	/**
	 * Stretch an image by adding transparent pixels to the left and right side.
	 * 
	 * @param img
	 *            the image
	 * @param wNew
	 *            the width of the new image
	 * @return a new Image where transparent pixels has been added to the left
	 *         and right side of the given image so that it has the given width
	 *         and so that the given image is centered in the new image (if
	 *         <i>wNew</i> is less or equal to the width of <i>img</i>,
	 *         <i>img</i> itself returned)
	 */
	public static Image pseudoStretch(Image img, final int wNew) {

		final int wOrig = img.getHeight();

		if (wNew <= wOrig)
			return img;

		final int h = img.getHeight();

		try {
			final int rgbOrig[] = new int[wOrig * h];
			img.getRGB(rgbOrig, 0, wOrig, 0, 0, wOrig, h);

			final int rgbNew[] = new int[wNew * h];

			final int xOffset = (wNew - wOrig) / 2;

			for (int y = 0; y < h; y++) {
				for (int x = 0; x < xOffset; x++) {
					rgbNew[y * wNew + x] = 0x00FFFFFF;
				}
				for (int x = xOffset; x < xOffset + wOrig; x++) {
					rgbNew[y * wNew + x] = rgbOrig[y * wOrig + x - xOffset];
				}
				for (int x = xOffset + wOrig; x < wNew; x++) {
					rgbNew[y * wNew + x] = 0x00FFFFFF;
				}
			}

			return Image.createRGBImage(rgbNew, wNew, h, true);

		} catch (Exception e) {
			Log.bug("Jan 31, 2009.5:16:04 PM", e);
			return img;
		}

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

		if (numerator == denominator)
			return img;

		try {

			final int rgb[] = new int[img.getWidth() * img.getHeight()];

			img.getRGB(rgb, 0, img.getWidth(), 0, 0, img.getWidth(),
				img.getHeight());

			final int wOrig = img.getWidth();
			final int w = wOrig * numerator / denominator;
			final int h = img.getHeight() * numerator / denominator;
			final int rgbSchrink[] = new int[w * h];

			for (int y = 0; y < h; y++) {
				final int yOrig = y * denominator / numerator * wOrig;
				final int yScaled = y * w;
				for (int x = 0; x < w; x++) {
					rgbSchrink[x + yScaled] = rgb[x * denominator / numerator
							+ yOrig];
				}
			}

			return Image.createRGBImage(rgbSchrink, w, h, false);

		} catch (Exception e) {
			Log.bug("Jan 31, 2009.5:05:48 PM", e);
			return img;
		}

	}

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

		maxWidth -= f.charWidth('W'); // tweak the algorithm below

		int w, slen, i, goodBreakPos;

		final Vector v = new Vector(3);

		w = f.stringWidth(s);

		while ((slen = s.length()) > 0) {

			goodBreakPos = 0;
			i = 1;
			w = 0;
			while (w < maxWidth && i < slen) {
				if (s.charAt(i) == ' ') {
					goodBreakPos = i;
				}
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

		final int n = v.size();
		final String[] sa = new String[n];
		for (i = 0; i < n; i++) {
			sa[i] = (String) v.elementAt(i);
		}

		return sa;

	}

	/**
	 * Load an image file.
	 * 
	 * @param file
	 *            path to the file
	 * @param fallBackSize
	 *            size of the fallback image to return if loading fails (if set
	 *            to zero then {@link #IMG_FALLBACK} is used as fallback image)
	 * @return the image (never <code>null</code>)
	 */
	private static Image loadImage(String file, int fallBackSize) {

		try {
			return Image.createImage(file);
		} catch (IOException e) {
			Log.ln("[TH] missing " + file);
			if (fallBackSize == 0) {
				return IMG_FALLBACK;
			} else {
				final Image img = Image.createImage(fallBackSize, fallBackSize);
				img.getGraphics().setColor(0);
				img.getGraphics().drawString("X", 2, 2,
					Graphics.TOP | Graphics.LEFT);
				return img;
			}
		}
	}

	/** Alert icon */
	public final Image aicBluetooth, aicWifi, aicConnecting, aicRefresh,
			aicHmpf, aicYes;

	/** List icon */
	public final Image licBluetooth, licWifi, licItem, licItemMarked,
			licList, licAdd, licThemes, licKeys, licOff, licLog,
			licDisconnect, licQueue, licSearch, licMLib, licFiles;

	private String current = null;

	private final Image[] img;

	private final Image logos[];

	private Theme(Display display) {

		img = new Image[IMG_NAME.length];

		// alert icons //

		aicBluetooth = loadImage("/icons/bluetooth_48.png", 48);
		aicWifi = loadImage("/icons/wifi_48.png", 48);
		aicConnecting = loadImage("/icons/connecting_48.png", 48);
		aicRefresh = loadImage("/icons/refresh_48.png", 48);
		aicHmpf = loadImage("/icons/hmpf_48.png", 48);
		aicYes = loadImage("/icons/yes_48.png", 48);

		// list icons //

		int size = -1;

		final int suggested = display.getBestImageWidth(Display.LIST_ELEMENT);

		for (int i = LIST_ICON_SIZES.length - 1; i >= 0; i--) {
			if (suggested >= LIST_ICON_SIZES[i]) {
				size = LIST_ICON_SIZES[i];
				break;
			}
		}
		if (size == -1) {
			size = LIST_ICON_SIZES[0];
		}

		licBluetooth = loadImage("/icons/bluetooth_" + size + ".png", size);
		licWifi = loadImage("/icons/wifi_" + size + ".png", size);
		licItem = loadImage("/icons/item_" + size + ".png", size);
		licItemMarked = loadImage("/icons/item_blue_" + size + ".png", size);
		licList = loadImage("/icons/list_" + size + ".png", size);
		licQueue = loadImage("/icons/queue_" + size + ".png", size);
		licMLib = loadImage("/icons/mlib_" + size + ".png", size);
		licFiles = loadImage("/icons/files_" + size + ".png", size);
		licSearch = loadImage("/icons/search_" + size + ".png", size);
		licAdd = loadImage("/icons/add_" + size + ".png", size);
		licThemes = loadImage("/icons/theme_" + size + ".png", size);
		licKeys = loadImage("/icons/keys_" + size + ".png", size);
		licOff = loadImage("/icons/off_" + size + ".png", size);
		licDisconnect = loadImage("/icons/disconnect_" + size + ".png", size);
		licLog = licList;

		// logo icons

		final int sizes[] = { 128, 96, 64, 48, 0 };

		logos = new Image[sizes.length];

		for (int i = 0; i < sizes.length - 1; i++) {
			logos[i] = loadImage("/icons/remuco_" + sizes[i] + ".png", sizes[i]);

		}
		logos[sizes.length - 1] = loadImage("/icons/tp.png", 1);

		// load default theme

		load(null);

	}

	/**
	 * Calculate uniform gaps between images given a fixed overall width for the
	 * images.
	 * 
	 * @param width
	 *            the complete width available
	 * @param imgIDs
	 *            IDs of the images to distribute equally in the given width
	 * @return an array of gaps - gap <em>i</em> is the gap width right of image
	 *         <em>i</em> (hence, this array is one element shorter than
	 *         <em>imgIDs</em>)
	 * @throws ScreenyException
	 *             if there is no space for gaps
	 */
	public int[] calculateGaps(int width, int imgWidth[])
			throws ScreenyException {

		if (imgWidth.length < 2) {
			return new int[0];
		}
		int widthSum = 0;
		for (int i = 0; i < imgWidth.length; i++) {
			widthSum += imgWidth[i];
		}
		if (widthSum >= width + imgWidth.length * 2) {
			throw new ScreenyException("no space for gaps");
		}
		int gaps[] = new int[imgWidth.length - 1];
		for (int i = 0; i < gaps.length; i++) {
			gaps[i] = (width - widthSum) / gaps.length;
		}
		gaps[gaps.length - 1] += (width - widthSum) % gaps.length;

		return gaps;
	}

	/**
	 * Calculate uniform gaps between screenies given a fixed overall width for
	 * the screenies.
	 * 
	 * @param width
	 *            the complete width available
	 * @param screenies
	 *            screenes to distribute equally
	 * @return an array of gaps - gap <em>i</em> is the gap width right of
	 *         screeny <em>i</em> (hence, this array is one element shorter than
	 *         <em>scrennies</em>)
	 * @throws ScreenyException
	 *             if there is no space for gaps
	 */
	public int[] calculateGaps(int width, Vector screenies)
			throws ScreenyException {

		int imgWidth[] = new int[screenies.size()];
		for (int i = 0; i < imgWidth.length; i++) {
			imgWidth[i] = ((Screeny) screenies.elementAt(i)).getWidth();
		}
		return calculateGaps(width, imgWidth);

	}

	/**
	 * Get a specific color of this theme.
	 * 
	 * @param id
	 *            the color id, one of <code>RTC_...</code>
	 * @return the color value
	 */
	public int getColor(int id) {
		final int rgb[] = new int[1];
		img[id].getRGB(rgb, 0, 1, 0, 0, 1, 1);
		return rgb[0];
	}

	/**
	 * Get a specific image of this theme.
	 * 
	 * @param id
	 *            the image id, one of <code>RTE_...</code>
	 * @return the image
	 */
	public Image getImg(int id) {
		return img[id];
	}

	/** Get a logo image which has a maximum height of <em>maxHeight</em>. */
	public Image getLogo(int maxHeight) {

		if (maxHeight <= 0) {
			return logos[0];
		}

		for (int i = 0; i < logos.length; i++) {
			if (maxHeight >= logos[i].getHeight()) {
				return logos[i];
			}
		} // last logo has size 1x1

		Log.bug("Mar 19, 2009.11:01:07 PM");

		return logos[logos.length - 1];

	}

	/**
	 * Get the name of the currently loaded theme.
	 * 
	 * @return the name
	 */
	public String getName() {
		return current;
	}

	/**
	 * Load a new theme.
	 * 
	 * @param name
	 *            theme name (may be <code>null</code> - in this case the
	 *            default theme gets loaded)
	 */
	public void load(String name) {

		if (name == null)
			name = DEFAULT;

		if (name.equals(current)) {
			return;
		}

		final String themes[] = Config.getInstance().getThemeList();

		int i;
		for (i = 0; i < themes.length; i++) {
			if (name.equals(themes[i])) {
				break;
			}
		}
		if (i == themes.length) {
			// 'name' seems to be an old, invalid name from the config
			name = themes[0];
		}

		current = name;

		final String themeDir = "/themes/" + current + "/";

		for (i = 0; i < img.length; i++) {
			img[i] = loadImage(themeDir + IMG_NAME[i] + ".png", 0);
		}

		Log.ln("[TH] loaded theme " + name);
	}

}
