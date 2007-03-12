package remuco.ui.canvas;

import java.io.IOException;

import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import remuco.Main;
import remuco.util.Log;

public class Theme {

	//////////////////////////////////////////////////////////////////////////
	// colors
	//////////////////////////////////////////////////////////////////////////

	public static final int COLOR_BG = 0;

	public static final int COLOR_LINES = 1;

	public static final int COLOR_TEXT = 2;

	private static final int COLORS_COUNT = 3;

	private static final int[] colorFallBacks = { 0, 0x902020, 0xAA2222 };

	private static final String[] colorNames = {
			"remuco-ui-canvas-color-background",
			"remuco-ui-canvas-color-lines", "remuco-ui-canvas-color-text" };

	private static final int[] colors = new int[COLORS_COUNT];

	//////////////////////////////////////////////////////////////////////////
	// state icons
	//////////////////////////////////////////////////////////////////////////

	protected static final int IMG_ST_OTHER = -1;

	protected static final int IMG_ST_PLAY = 0;

	protected static final int IMG_ST_PAUSE = 1;

	protected static final int IMG_ST_STOP = 2;

	protected static final int IMG_ST_OFF = 3;

	protected static final int IMG_ST_SRVOFF = 4;

	protected static final int IMG_ST_PROBLEM = 5;

	protected static final int IMG_ST_ERROR = 6;

	protected static final int IMG_ST_COUNT = 7;

	private static final String[] stateImageFiles = { "play", "pause", "stop",
			"off", "srvoff", "problem", "error" };

	private static Image fallBackImage;

	private static final Image[] stateImages = new Image[IMG_ST_COUNT];

	//////////////////////////////////////////////////////////////////////////
	// fonts
	//////////////////////////////////////////////////////////////////////////

	protected static final Font FONT_TITLE = Font.getFont(Font.FACE_SYSTEM,
			Font.STYLE_BOLD, Font.SIZE_LARGE);

	protected static final Font FONT_STD = Font.getDefaultFont();

	protected static final Font FONT_ALBUM = FONT_STD;

	protected static final Font FONT_ARTIST = FONT_STD;
	
	//////////////////////////////////////////////////////////////////////////
	// methods
	//////////////////////////////////////////////////////////////////////////

	protected static int getColor(int type) {

		if (type < 0 || type >= COLORS_COUNT)
			return 0x123456;

		return colors[type];

	}

	protected static Image getIcon(int type) {

		if (type < 0 || type >= IMG_ST_COUNT)
			return fallBackImage;

		return stateImages[type];
	}

	protected static void init() {

		int i;

		fallBackImage = getFallBackImage();

		for (i = 0; i < IMG_ST_COUNT; i++) {
			try {
				stateImages[i] = Image.createImage(stateImageFiles[i] + ".png");
			} catch (IOException e) {
				Log.ln("Loading " + stateImageFiles[i] + ".png" + " failed !");
				stateImages[i] = fallBackImage;
			}
		}

		for (i = 0; i < COLORS_COUNT; i++) {
			colors[i] = Main.getAPropInt(colorNames[i], colorFallBacks[i]);
		}

	}

	private static Image getFallBackImage() {
		Image img = Image.createImage(12, 12);
		Graphics g = img.getGraphics();
		g.setColor(0);
		g.drawLine(0, 0, 11, 11);
		g.drawLine(11, 0, 0, 11);
		return img;
	}

}
