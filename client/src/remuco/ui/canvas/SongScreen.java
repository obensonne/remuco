package remuco.ui.canvas;

import java.io.IOException;
import java.util.Vector;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import remuco.data.PlayerState;
import remuco.data.Song;
import remuco.proto.Remuco;
import remuco.util.Log;

public class SongScreen extends Canvas {

	private static final byte
		COLOR_BG = 0,
		COLOR_TEXT = 1,
		COLOR_TITLE = 2,
		COLOR_ARTIST = 3,
		COLOR_ALBUM = 4;

	private static final int COLORS_COUNT = 5;

	private static final byte IMGID_BORDER_TOP = 0, IMGID_BORDER_BOTTOM = 1,
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

	private int[] colors;

	private int currentRating;

	private Song currentSong;

	private byte currentVolume;

	private Image[] img;

	private Image imgFallBack, imgScreenBG, imgSongAreaBG;

	private KeyListener kl;

	private ScreenElement seState, seRepeat, seShuffle, seVolume, seSong;

	private Song songDefault;

	/**
	 * Create a new SongScreen.
	 * 
	 * @param pcl
	 *            the parent {@link CommandListener} to delegate commands to
	 */
	public SongScreen(KeyListener kl) {

		super();

		int i;

		this.kl = kl;

		songDefault = new Song();
		songDefault.setTag(Remuco.REM_TAG_NAME_TITLE, "Remuco");
		currentSong = songDefault;
		currentVolume = 55;

		createFallBackImage();

		img = new Image[IMGS_COUNT];
		for (i = 0; i < IMGS_COUNT; i++) {
			try {
				img[i] = Image.createImage(imgName[i]);
			} catch (IOException e) {
				System.out.println("loading image " + imgName[i] + " failed!");
				img[i] = imgFallBack;
			}
		}

		colors = new int[COLORS_COUNT];
		img[IMGID_COLORS]
				.getRGB(colors, 0, COLORS_COUNT, 0, 0, COLORS_COUNT, 1);

		initScreenElements();

		createBackgroundImage();

		updateVolumeBar();

		updateSongArea();

	}

	public void update(PlayerState ps) {

		switch (ps.getState()) {
		case Remuco.REM_PS_STATE_PLAY:
			seState.setImage(img[IMGID_STATE_PLAY]);
			break;
		case Remuco.REM_PS_STATE_PAUSE:
			seState.setImage(img[IMGID_STATE_PAUSE]);
			break;
		case Remuco.REM_PS_STATE_STOP:
			seState.setImage(img[IMGID_STATE_STOP]);
			break;
		case Remuco.REM_PS_STATE_OFF:
			seState.setImage(img[IMGID_STATE_OFF]);
			break;
		case Remuco.REM_PS_STATE_SRVOFF:
			seState.setImage(img[IMGID_STATE_SRVOFF]);
			break;
		case Remuco.REM_PS_STATE_PROBLEM:
			seState.setImage(img[IMGID_STATE_PROBLEM]);
			break;
		case Remuco.REM_PS_STATE_ERROR:
			seState.setImage(img[IMGID_STATE_ERROR]);
			break;
		default:
			seState.setImage(img[IMGID_STATE_PROBLEM]);
			Log.ln(this, "unknown state");
			break;
		}

		if (ps.playlistIsRepeat()) {
			seRepeat.setImage(img[IMGID_REPEAT_ON]);
		} else {
			seRepeat.setImage(img[IMGID_REPEAT_OFF]);
		}

		if (ps.playlistIsShuffle()) {
			seShuffle.setImage(img[IMGID_SHUFFLE_ON]);
		} else {
			seShuffle.setImage(img[IMGID_SHUFFLE_OFF]);
		}

		if (currentVolume != ps.getVolume()) {
			currentVolume = ps.getVolume();
			updateVolumeBar();
		}

		Song s = ps.getCurrentSong();
		if (s == null)
			s = songDefault;
		if (currentSong != s || currentRating != s.getRating()) {
			currentSong = s;
			currentRating = s.getRating();
			updateSongArea();
		}

	}

	protected void keyPressed(int key) {
		kl.keyPressed(translateKey(key));
	}

	protected void keyReleased(int key) {
		kl.keyReleased(translateKey(key));
	}

	protected void paint(Graphics g) {

		g.drawImage(imgScreenBG, 0, 0, Graphics.TOP | Graphics.LEFT);

		seState.draw(g);
		seRepeat.draw(g);
		seShuffle.draw(g);
		seVolume.draw(g);
		seSong.draw(g);

	}

	private void createBackgroundImage() {

		int i, n, m;
		int width = getWidth();
		int height = getHeight();

		imgScreenBG = Image.createImage(width, height);

		Graphics g = imgScreenBG.getGraphics();

		Point p = new Point();

		g.setColor(colors[COLOR_BG]);

		g.fillRect(0, 0, width, height);

		// //// icon bar at the top //////

		drawImageX(g, img[IMGID_TOP_LEFT], p);
		drawImageX(g, img[IMGID_STATE_PLAY], p);
		drawImageX(g, img[IMGID_SHUFFLE_OFF], p);
		drawImageX(g, img[IMGID_REPEAT_ON], p);
		drawImageX(g, img[IMGID_VOLUME_SYMBOL], p);
		drawImageX(g, img[IMGID_VOLUME_LEFT], p);

		p.x = width - img[IMGID_TOP_RIGHT].getWidth()
				- img[IMGID_VOLUME_RIGHT].getWidth();

		drawImageX(g, img[IMGID_VOLUME_RIGHT], p);
		drawImageX(g, img[IMGID_TOP_RIGHT], p);

		// //// song area background //////

		imgSongAreaBG = Image
				.createImage(seSong.getWidth(), seSong.getHeigth());
		Graphics gTmp = imgSongAreaBG.getGraphics();
		p.y = 0;
		while (p.y < height) {
			p.x = 0;
			while (p.x < width) {
				drawImageX(gTmp, img[IMGID_SONG_AREA], p);
			}
			p.y += img[IMGID_SONG_AREA].getHeight();
		}

//		p.x = img[IMGID_BORDER_LEFT].getWidth();
//		p.y = img[IMGID_TOP_RIGHT].getHeight()
//				+ img[IMGID_BORDER_TOP].getHeight();
//		drawImageX(g, imgSongAreaBG, p);

		// //// song area borders //////

		n = img[IMGID_CORNER_TOP_LEFT].getWidth(); // x
		m = width - img[IMGID_CORNER_TOP_LEFT].getWidth()
				- img[IMGID_CORNER_TOP_RIGHT].getWidth(); // width
		g.setClip(n, 0, m, height);
		p.x = 0;
		p.y = img[IMGID_TOP_LEFT].getHeight();
		while (p.x < width) {
			drawImageX(g, img[IMGID_BORDER_TOP], p);
		}
		p.x = 0;
		p.y = height - img[IMGID_BORDER_BOTTOM].getHeight();
		while (p.x < width) {
			drawImageX(g, img[IMGID_BORDER_BOTTOM], p);
		}

		n = img[IMGID_TOP_LEFT].getHeight()
				+ img[IMGID_CORNER_TOP_LEFT].getHeight(); // y
		m = height - img[IMGID_TOP_LEFT].getHeight()
				- img[IMGID_CORNER_TOP_LEFT].getHeight()
				- img[IMGID_CORNER_BOTTOM_LEFT].getHeight(); // height
		g.setClip(0, n, width, m);
		p.x = 0;
		p.y = img[IMGID_TOP_RIGHT].getHeight();
		while (p.y < height) {
			drawImageY(g, img[IMGID_BORDER_LEFT], p);
		}
		p.x = width - img[IMGID_BORDER_RIGHT].getWidth();
		p.y = img[IMGID_TOP_RIGHT].getHeight();
		while (p.y < height) {
			drawImageY(g, img[IMGID_BORDER_RIGHT], p);
		}
		
		g.setClip(0, 0, width, height);

		// //// song area corners //////

		p.x = 0;
		p.y = img[IMGID_TOP_RIGHT].getHeight();
		drawImageX(g, img[IMGID_CORNER_TOP_LEFT], p);

		p.x = width - img[IMGID_CORNER_TOP_RIGHT].getWidth();
		p.y = img[IMGID_TOP_RIGHT].getHeight();
		drawImageX(g, img[IMGID_CORNER_TOP_RIGHT], p);

		p.x = width - img[IMGID_CORNER_BOTTOM_RIGHT].getWidth();
		p.y += height - img[IMGID_CORNER_BOTTOM_RIGHT].getHeight();
		drawImageX(g, img[IMGID_CORNER_BOTTOM_LEFT], p);

		p.x = 0;
		p.y += height - img[IMGID_CORNER_BOTTOM_LEFT].getHeight();
		drawImageX(g, img[IMGID_CORNER_BOTTOM_LEFT], p);

		n = width - img[IMGID_CORNER_TOP_RIGHT].getWidth();
		while (p.x < n) {
			drawImageX(g, img[IMGID_BORDER_TOP], p);
		}
		p.x = n;
		drawImageX(g, img[IMGID_CORNER_TOP_RIGHT], p);

		n = height - img[IMGID_BORDER_BOTTOM].getHeight();
		m = width - img[IMGID_BORDER_RIGHT].getWidth();
		while (p.y < n) {
			p.x = 0;
			drawImageX(g, img[IMGID_BORDER_LEFT], p);
			while (p.x < m) {
				drawImageX(g, img[IMGID_SONG_AREA], p);
			}
			p.x = m;
			drawImageX(g, img[IMGID_BORDER_RIGHT], p);
			p.y += img[IMGID_SONG_AREA].getHeight();
		}

		p.x = 0;
		p.y = n;
		drawImageX(g, img[IMGID_CORNER_BOTTOM_LEFT], p);
		n = width - img[IMGID_CORNER_BOTTOM_RIGHT].getWidth();
		while (p.x < n) {
			drawImageX(g, img[IMGID_BORDER_BOTTOM], p);
		}
		p.x = n;
		drawImageX(g, img[IMGID_CORNER_BOTTOM_RIGHT], p);

		g.drawString("Hey ho up da für", width / 2, height / 2,
				Graphics.BASELINE | Graphics.HCENTER);

	}

	private void createFallBackImage() {
		imgFallBack = Image.createImage(12, 12);
		Graphics g = imgFallBack.getGraphics();
		g.setColor(0);
		g.drawLine(0, 0, 11, 11);
		g.drawLine(11, 0, 0, 11);
	}

	private void drawImageX(Graphics g, Image img, Point p) {
		g.drawImage(img, p.x, p.y, Graphics.TOP | Graphics.LEFT);
		p.x += img.getWidth();
	}

	private void drawImageY(Graphics g, Image img, Point p) {
		g.drawImage(img, p.x, p.y, Graphics.TOP | Graphics.LEFT);
		p.y += img.getHeight();
	}

	private int drawStrings(String[] sa, int width, int vOffset, Graphics g) {

		int i, saLen, fontHeight;
		saLen = sa.length;
		fontHeight = g.getFont().getHeight();

		for (i = 0; i < saLen; i++) {
			g.drawString(sa[i], width / 2, vOffset, Graphics.TOP
					| Graphics.HCENTER);
			vOffset += fontHeight;
		}

		return vOffset;
	}

	private void initScreenElements() {

		int x, y, w, h;
		Image i;
		Graphics g;

		x = img[IMGID_TOP_LEFT].getWidth();
		y = 0;
		seState = new ScreenElement(x, y, img[IMGID_STATE_PROBLEM]);

		x = seState.getNextX();
		y = 0;
		seRepeat = new ScreenElement(x, y, img[IMGID_REPEAT_OFF]);

		x = seRepeat.getNextX();
		y = 0;
		seShuffle = new ScreenElement(x, y, img[IMGID_SHUFFLE_OFF]);

		x = seShuffle.getNextX() + img[IMGID_VOLUME_SYMBOL].getWidth()
				+ img[IMGID_VOLUME_RIGHT].getWidth();
		y = 0;
		w = getWidth() - img[IMGID_TOP_RIGHT].getWidth()
				- img[IMGID_VOLUME_RIGHT].getWidth() - x;
		h = img[IMGID_VOLUME_OFF].getHeight();
		i = Image.createImage(w, h);
		g = i.getGraphics();
		g.setColor(colors[COLOR_BG]);
		g.drawRect(0, 0, w, h);
		seVolume = new ScreenElement(x, y, i);

		x = img[IMGID_BORDER_LEFT].getWidth();
		y = seState.getNextY() + img[IMGID_BORDER_TOP].getHeight();
		w = getWidth() - img[IMGID_BORDER_RIGHT].getWidth() - x;
		h = getHeight() - img[IMGID_BORDER_BOTTOM].getHeight() - y;
		i = Image.createImage(w, h);
		g = i.getGraphics();
		g.setColor(colors[COLOR_BG]);
		g.drawRect(0, 0, w, h);
		seSong = new ScreenElement(x, y, i);

	}

	private String[] splitString(String s, int maxWidth, Font f) {

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

	private int translateKey(int key) {
		switch (key) {
		case Canvas.KEY_STAR:
			Log.ln("*");
			return KeyListener.KEY_RATE_DOWN;
		case Canvas.KEY_POUND:
			Log.ln("#");
			return KeyListener.KEY_RATE_UP;
		case Canvas.KEY_NUM0:
			Log.ln("0");
			return KeyListener.KEY_SHOW_PLAYLIST;
		}
		int ga = getGameAction(key);
		if (ga != 0) {
			switch (ga) {
			case Canvas.FIRE:
				Log.ln("GA fire");
				return KeyListener.KEY_PLAY_PAUSE;
			case Canvas.RIGHT:
				Log.ln("GA right");
				return KeyListener.KEY_VOLUME_UP;
			case Canvas.LEFT:
				Log.ln("GA left");
				return KeyListener.KEY_VOLUME_DOWN;
			case Canvas.UP:
				Log.ln("GA up");
				return KeyListener.KEY_PREV;
			case Canvas.DOWN:
				Log.ln("GA down");
				return KeyListener.KEY_NEXT;
			}
		}
		return KeyListener.KEY_NOOP;
	}

	private void updateSongArea() {

		Image si = seSong.getImage();
		Graphics g = si.getGraphics();
		int width = si.getWidth();
		int height = si.getHeight();
		int y = 0;
		String[] sa;

		g.drawImage(imgSongAreaBG, 0, 0, Graphics.LEFT | Graphics.TOP);

		// g.drawString(System.currentTimeMillis() + "", si.getWidth() / 2, si
		// .getHeight() / 2, Graphics.BASELINE | Graphics.HCENTER);
		//
		g.setColor(colors[COLOR_ARTIST]);
		g.setFont(Theme.FONT_ARTIST);
		sa = splitString(currentSong.getTag(Song.TAG_ARTIST), width - 4, g
				.getFont());
		y = drawStrings(sa, width, y, g);

		g.setColor(colors[COLOR_TITLE]);
		g.setFont(Theme.FONT_TITLE);
		sa = splitString(currentSong.getTag(Song.TAG_TITLE), width - 4, g
				.getFont());
		y = drawStrings(sa, width, y, g);

		g.setColor(colors[COLOR_ALBUM]);
		g.setFont(Theme.FONT_ALBUM);
		sa = splitString(currentSong.getTag(Song.TAG_ALBUM), width - 4, g
				.getFont());
		y = drawStrings(sa, width, y, g);

		int rating = currentSong.getRating();

		Log.ln("display rating of " + rating);

		if (rating < 0)
			return;

		y = height - img[IMGID_BORDER_BOTTOM].getHeight();

		int rsw = img[IMGID_RATE_OFF].getWidth();
		int x, i;
		x = (width - rsw * currentSong.getRatingMax()) / 2;
		for (i = 1; i <= rating; i++) {
			g.drawImage(img[IMGID_RATE_ON], x + rsw * (i - 1), y, Graphics.LEFT
					| Graphics.BOTTOM);
		}
		for (i = rating + 1; i <= currentSong.getRatingMax(); i++) {
			g.drawImage(img[IMGID_RATE_OFF], x + rsw * (i - 1), y,
					Graphics.LEFT | Graphics.BOTTOM);
		}

	}

	private void updateVolumeBar() {

//		Graphics g = seVolume.getImage().getGraphics();
//		Point p = new Point(0, 0);
//		int i;
//		int width = seVolume.getImage().getWidth();
//		int on = width * currentVolume / 100;
//
//		for (i = 0; i <= on; i++) {
//			drawImageX(g, img[IMGID_VOLUME_ON], p);
//		}
//		for (i = on; i < width; i++) {
//			drawImageX(g, img[IMGID_VOLUME_OFF], p);
//		}

	}

}
