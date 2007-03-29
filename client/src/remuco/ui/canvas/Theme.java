package remuco.ui.canvas;

import java.io.IOException;
import java.util.Vector;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import remuco.util.Log;

public class Theme {

	protected static final byte COLOR_BG = 0, COLOR_TEXT = 1, COLOR_TITLE = 2,
			COLOR_ARTIST = 3, COLOR_ALBUM = 4;

	protected static int[] colors;

	protected static final Font FONT = Font.getDefaultFont();

	protected static final Font FONT_ALBUM = Font.getFont(Font.FACE_SYSTEM,
			Font.STYLE_PLAIN, Font.SIZE_SMALL);

	protected static final Font FONT_ARTIST = FONT;

	protected static final Font FONT_TEXT = FONT_ALBUM;

	protected static final Font FONT_TITLE = Font.getFont(Font.FACE_SYSTEM,
			Font.STYLE_BOLD, Font.SIZE_LARGE);

	protected static Image[] img;

	protected static Image imgFallBack;

	protected static final byte IMGID_BORDER_TOP = 0, IMGID_BORDER_BOTTOM = 1,
			IMGID_BORDER_LEFT = 2, IMGID_BORDER_RIGHT = 3,
			IMGID_CORNER_TOP_LEFT = 4, IMGID_CORNER_TOP_RIGHT = 5,
			IMGID_CORNER_BOTTOM_LEFT = 6, IMGID_CORNER_BOTTOM_RIGHT = 7,
			IMGID_SONG_AREA = 8, IMGID_TOP_LEFT = 9, IMGID_TOP_RIGHT = 10,
			IMGID_TOP_SPACER = 11, IMGID_REPEAT_OFF = 12, IMGID_REPEAT_ON = 13,
			IMGID_SHUFFLE_OFF = 14, IMGID_SHUFFLE_ON = 15,
			IMGID_VOLUME_SYMBOL = 16, IMGID_VOLUME_LEFT = 17,
			IMGID_VOLUME_RIGHT = 18, IMGID_VOLUME_OFF = 19,
			IMGID_VOLUME_ON = 20, IMGID_STATE_PLAY = 21,
			IMGID_STATE_PAUSE = 22, IMGID_STATE_STOP = 23,
			IMGID_STATE_OFF = 24, IMGID_STATE_SRVOFF = 25,
			IMGID_STATE_PROBLEM = 26, IMGID_STATE_ERROR = 27,
			IMGID_RATE_OFF = 28, IMGID_RATE_ON = 29, IMGID_COLORS = 30;

	private static final int COLORS_COUNT = 5;

	private static String current;

	private static final String[] imgName = { "border-top.png",
			"border-bottom.png", "border-left.png", "border-right.png",
			"corner-top-left.png", "corner-top-right.png",
			"corner-bottom-left.png", "corner-bottom-right.png",
			"song-area.png", "top-left.png", "top-right.png", "top-spacer.png",
			"repeat-off.png", "repeat-on.png", "shuffle-off.png",
			"schuffle-on.png", "volume-symbol.png", "volume-left.png",
			"volume-right.png", "volume-off.png", "volume-on.png",
			"state-play.png", "state-pause.png", "state-stop.png",
			"state-off.png", "state-srvoff.png", "state-problem.png",
			"state-error.png", "rate-off.png", "rate-on.png", "colors.png", };

	private static final int IMGS_COUNT = imgName.length;

	// ////////////////////////////////////////////////////////////////////////
	// methods
	// ////////////////////////////////////////////////////////////////////////

	private static boolean initialized = false;

	protected static boolean isInitialized() {
		return initialized;
	}

	protected static void load(String name) {

		if (!initialized)
			init();

		if (current == null || !name.equals(current))
			update(name);
	}

	protected static String[] splitString(String s, int maxWidth, Font f) {

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

	private static void createFallBackImage() {

		int size = 20;

		imgFallBack = Image.createImage(size, size);
		Graphics g = imgFallBack.getGraphics();
		g.setColor(0);
		g.drawLine(0, 0, size - 1, size - 1);
		g.drawLine(size - 1, 0, 0, size - 1);
	}

	private static void init() {

		initialized = true;

		createFallBackImage();

		img = new Image[IMGS_COUNT];

		colors = new int[COLORS_COUNT];

	}

	private static void update(String name) {

		int i;

		current = name;

		for (i = 0; i < IMGS_COUNT; i++) {
			try {
				img[i] = Image.createImage("/" + name + "/" + imgName[i]);
			} catch (IOException e) {
				Log.ln("[TH]: loading image " + imgName[i] + " failed!");
				img[i] = imgFallBack;
			}
		}

		img[IMGID_COLORS]
				.getRGB(colors, 0, COLORS_COUNT, 0, 0, COLORS_COUNT, 1);

	}

}
