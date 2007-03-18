package remuco.ui.canvas;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import remuco.data.PlayerState;
import remuco.data.Song;
import remuco.proto.Remuco;
import remuco.util.Log;

public class SongScreen extends Canvas {

	private int currentRating;

	private Song currentSong;

	private byte currentVolume;

	private Image imgScreenBG, imgSongAreaBG;

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

		this.kl = kl;

		songDefault = new Song();
		songDefault.setTag(Remuco.REM_TAG_NAME_TITLE, "Remuco");
		currentSong = songDefault;
		currentVolume = 55;

		applyTheme();
	}

	public void updatePlayerState(PlayerState ps) {

		switch (ps.getState()) {
		case Remuco.REM_PS_STATE_PLAY:
			seState.setImage(Theme.img[Theme.IMGID_STATE_PLAY]);
			break;
		case Remuco.REM_PS_STATE_PAUSE:
			seState.setImage(Theme.img[Theme.IMGID_STATE_PAUSE]);
			break;
		case Remuco.REM_PS_STATE_STOP:
			seState.setImage(Theme.img[Theme.IMGID_STATE_STOP]);
			break;
		case Remuco.REM_PS_STATE_OFF:
			seState.setImage(Theme.img[Theme.IMGID_STATE_OFF]);
			break;
		case Remuco.REM_PS_STATE_SRVOFF:
			seState.setImage(Theme.img[Theme.IMGID_STATE_SRVOFF]);
			break;
		case Remuco.REM_PS_STATE_PROBLEM:
			seState.setImage(Theme.img[Theme.IMGID_STATE_PROBLEM]);
			break;
		case Remuco.REM_PS_STATE_ERROR:
			seState.setImage(Theme.img[Theme.IMGID_STATE_ERROR]);
			break;
		default:
			seState.setImage(Theme.img[Theme.IMGID_STATE_PROBLEM]);
			Log.ln(this, "unknown state");
			break;
		}

		if (ps.playlistIsRepeat()) {
			seRepeat.setImage(Theme.img[Theme.IMGID_REPEAT_ON]);
		} else {
			seRepeat.setImage(Theme.img[Theme.IMGID_REPEAT_OFF]);
		}

		if (ps.playlistIsShuffle()) {
			seShuffle.setImage(Theme.img[Theme.IMGID_SHUFFLE_ON]);
		} else {
			seShuffle.setImage(Theme.img[Theme.IMGID_SHUFFLE_OFF]);
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

	public void updateTheme() {
		applyTheme();
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

	private void applyTheme() {

		initScreenElements();

		createBackgroundImage();

		updateVolumeBar();

		updateSongArea();

	}

	private void createBackgroundImage() {

		int n, m;
		int width = getWidth();
		int height = getHeight();

		imgScreenBG = Image.createImage(width, height);

		Graphics g = imgScreenBG.getGraphics();

		Point p = new Point();

		g.setColor(Theme.colors[Theme.COLOR_BG]);

		g.fillRect(0, 0, width, height);

		// //// icon bar at the top //////

		drawImageX(g, Theme.img[Theme.IMGID_TOP_LEFT], p);
		drawImageX(g, Theme.img[Theme.IMGID_STATE_PLAY], p);
		drawImageX(g, Theme.img[Theme.IMGID_SHUFFLE_OFF], p);
		drawImageX(g, Theme.img[Theme.IMGID_REPEAT_ON], p);
		drawImageX(g, Theme.img[Theme.IMGID_VOLUME_SYMBOL], p);
		drawImageX(g, Theme.img[Theme.IMGID_VOLUME_LEFT], p);

		p.x = width - Theme.img[Theme.IMGID_TOP_RIGHT].getWidth()
				- Theme.img[Theme.IMGID_VOLUME_RIGHT].getWidth();

		drawImageX(g, Theme.img[Theme.IMGID_VOLUME_RIGHT], p);
		drawImageX(g, Theme.img[Theme.IMGID_TOP_RIGHT], p);

		// //// song area background //////

		imgSongAreaBG = Image
				.createImage(seSong.getWidth(), seSong.getHeigth());
		Graphics gTmp = imgSongAreaBG.getGraphics();
		p.y = 0;
		while (p.y < height) {
			p.x = 0;
			while (p.x < width) {
				drawImageX(gTmp, Theme.img[Theme.IMGID_SONG_AREA], p);
			}
			p.y += Theme.img[Theme.IMGID_SONG_AREA].getHeight();
		}

		// p.x = Theme.img[Theme.IMGID_BORDER_LEFT].getWidth();
		// p.y = Theme.img[Theme.IMGID_TOP_RIGHT].getHeight()
		// + Theme.img[Theme.IMGID_BORDER_TOP].getHeight();
		// drawImageX(g, imgSongAreaBG, p);

		// //// song area borders //////

		n = Theme.img[Theme.IMGID_CORNER_TOP_LEFT].getWidth(); // x
		m = width - Theme.img[Theme.IMGID_CORNER_TOP_LEFT].getWidth()
				- Theme.img[Theme.IMGID_CORNER_TOP_RIGHT].getWidth(); // width
		g.setClip(n, 0, m, height);
		p.x = 0;
		p.y = Theme.img[Theme.IMGID_TOP_LEFT].getHeight();
		while (p.x < width) {
			drawImageX(g, Theme.img[Theme.IMGID_BORDER_TOP], p);
		}
		p.x = 0;
		p.y = height - Theme.img[Theme.IMGID_BORDER_BOTTOM].getHeight();
		while (p.x < width) {
			drawImageX(g, Theme.img[Theme.IMGID_BORDER_BOTTOM], p);
		}

		n = Theme.img[Theme.IMGID_TOP_LEFT].getHeight()
				+ Theme.img[Theme.IMGID_CORNER_TOP_LEFT].getHeight(); // y
		m = height - Theme.img[Theme.IMGID_TOP_LEFT].getHeight()
				- Theme.img[Theme.IMGID_CORNER_TOP_LEFT].getHeight()
				- Theme.img[Theme.IMGID_CORNER_BOTTOM_LEFT].getHeight(); // height
		g.setClip(0, n, width, m);
		p.x = 0;
		p.y = Theme.img[Theme.IMGID_TOP_RIGHT].getHeight();
		while (p.y < height) {
			drawImageY(g, Theme.img[Theme.IMGID_BORDER_LEFT], p);
		}
		p.x = width - Theme.img[Theme.IMGID_BORDER_RIGHT].getWidth();
		p.y = Theme.img[Theme.IMGID_TOP_RIGHT].getHeight();
		while (p.y < height) {
			drawImageY(g, Theme.img[Theme.IMGID_BORDER_RIGHT], p);
		}

		g.setClip(0, 0, width, height);

		// //// song area corners //////

		p.x = 0;
		p.y = Theme.img[Theme.IMGID_TOP_RIGHT].getHeight();
		drawImageX(g, Theme.img[Theme.IMGID_CORNER_TOP_LEFT], p);

		p.x = width - Theme.img[Theme.IMGID_CORNER_TOP_RIGHT].getWidth();
		p.y = Theme.img[Theme.IMGID_TOP_RIGHT].getHeight();
		drawImageX(g, Theme.img[Theme.IMGID_CORNER_TOP_RIGHT], p);

		p.x = width - Theme.img[Theme.IMGID_CORNER_BOTTOM_RIGHT].getWidth();
		p.y += height - Theme.img[Theme.IMGID_CORNER_BOTTOM_RIGHT].getHeight();
		drawImageX(g, Theme.img[Theme.IMGID_CORNER_BOTTOM_LEFT], p);

		p.x = 0;
		p.y += height - Theme.img[Theme.IMGID_CORNER_BOTTOM_LEFT].getHeight();
		drawImageX(g, Theme.img[Theme.IMGID_CORNER_BOTTOM_LEFT], p);

		n = width - Theme.img[Theme.IMGID_CORNER_TOP_RIGHT].getWidth();
		while (p.x < n) {
			drawImageX(g, Theme.img[Theme.IMGID_BORDER_TOP], p);
		}
		p.x = n;
		drawImageX(g, Theme.img[Theme.IMGID_CORNER_TOP_RIGHT], p);

		n = height - Theme.img[Theme.IMGID_BORDER_BOTTOM].getHeight();
		m = width - Theme.img[Theme.IMGID_BORDER_RIGHT].getWidth();
		while (p.y < n) {
			p.x = 0;
			drawImageX(g, Theme.img[Theme.IMGID_BORDER_LEFT], p);
			while (p.x < m) {
				drawImageX(g, Theme.img[Theme.IMGID_SONG_AREA], p);
			}
			p.x = m;
			drawImageX(g, Theme.img[Theme.IMGID_BORDER_RIGHT], p);
			p.y += Theme.img[Theme.IMGID_SONG_AREA].getHeight();
		}

		p.x = 0;
		p.y = n;
		drawImageX(g, Theme.img[Theme.IMGID_CORNER_BOTTOM_LEFT], p);
		n = width - Theme.img[Theme.IMGID_CORNER_BOTTOM_RIGHT].getWidth();
		while (p.x < n) {
			drawImageX(g, Theme.img[Theme.IMGID_BORDER_BOTTOM], p);
		}
		p.x = n;
		drawImageX(g, Theme.img[Theme.IMGID_CORNER_BOTTOM_RIGHT], p);

		g.drawString("Hey ho up da für", width / 2, height / 2,
				Graphics.BASELINE | Graphics.HCENTER);

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

		x = Theme.img[Theme.IMGID_TOP_LEFT].getWidth();
		y = 0;
		seState = new ScreenElement(x, y, Theme.img[Theme.IMGID_STATE_PROBLEM]);

		x = seState.getNextX();
		y = 0;
		seRepeat = new ScreenElement(x, y, Theme.img[Theme.IMGID_REPEAT_OFF]);

		x = seRepeat.getNextX();
		y = 0;
		seShuffle = new ScreenElement(x, y, Theme.img[Theme.IMGID_SHUFFLE_OFF]);

		x = seShuffle.getNextX()
				+ Theme.img[Theme.IMGID_VOLUME_SYMBOL].getWidth()
				+ Theme.img[Theme.IMGID_VOLUME_RIGHT].getWidth();
		y = 0;
		w = getWidth() - Theme.img[Theme.IMGID_TOP_RIGHT].getWidth()
				- Theme.img[Theme.IMGID_VOLUME_RIGHT].getWidth() - x;
		h = Theme.img[Theme.IMGID_VOLUME_OFF].getHeight();
		i = Image.createImage(w, h);
		g = i.getGraphics();
		g.setColor(Theme.colors[Theme.COLOR_BG]);
		g.fillRect(0, 0, w, h);
		seVolume = new ScreenElement(x, y, i);

		x = Theme.img[Theme.IMGID_BORDER_LEFT].getWidth();
		y = seState.getNextY() + Theme.img[Theme.IMGID_BORDER_TOP].getHeight();
		w = getWidth() - Theme.img[Theme.IMGID_BORDER_RIGHT].getWidth() - x;
		h = getHeight() - Theme.img[Theme.IMGID_BORDER_BOTTOM].getHeight() - y;
		i = Image.createImage(w, h);
		g = i.getGraphics();
		g.setColor(Theme.colors[Theme.COLOR_BG]);
		g.fillRect(0, 0, w, h);
		seSong = new ScreenElement(x, y, i);

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
		int heightRatingImg = Theme.img[Theme.IMGID_RATE_OFF].getHeight();
		int i, x = 0, y = 0, rsw, rating, ratingMax, rest;
		String[] sa;
		String s;

		// //// background //////

		g.drawImage(imgSongAreaBG, 0, 0, Graphics.LEFT | Graphics.TOP);

		// //// artist //////

		g.setColor(Theme.colors[Theme.COLOR_ARTIST]);
		g.setFont(Theme.FONT_ARTIST);
		sa = Theme.splitString(currentSong.getTag(Song.TAG_ARTIST), width - 4,
				g.getFont());
		y = drawStrings(sa, width, y, g);

		// //// title //////

		g.setColor(Theme.colors[Theme.COLOR_TITLE]);
		g.setFont(Theme.FONT_TITLE);
		sa = Theme.splitString(currentSong.getTag(Song.TAG_TITLE), width - 4, g
				.getFont());
		y = drawStrings(sa, width, y, g);

		// //// album //////

		g.setColor(Theme.colors[Theme.COLOR_ALBUM]);
		g.setFont(Theme.FONT_ALBUM);
		sa = Theme.splitString(currentSong.getTag(Song.TAG_ALBUM), width - 4, g
				.getFont());
		y = drawStrings(sa, width, y, g);

		// //// year and track length //////

		rest = height - y - heightRatingImg;

		if (rest > Theme.FONT_ALBUM.getHeight() * 2) {

			g.setColor(Theme.colors[Theme.COLOR_TEXT]);
			g.setFont(Theme.FONT_TEXT);

			y = height - heightRatingImg - Theme.FONT_ALBUM.getHeight() / 2;

			s = currentSong.getTag(Song.TAG_YEAR);
			try {
				i = Integer.parseInt(s);
			} catch (NumberFormatException e) {
				i = 0;
			}
			if (i != 0) {
				x = 0;
				g.drawString(s, x, y, Graphics.LEFT | Graphics.BOTTOM);
			}

			s = currentSong.getTag(Song.TAG_GENRE);
			g.drawString(s, width/2, y, Graphics.HCENTER | Graphics.BOTTOM);
			
			s = currentSong.getLenFormatted();
			g.drawString(s, width, y, Graphics.RIGHT | Graphics.BOTTOM);

		}

		// //// rating //////

		rating = currentSong.getRating();
		ratingMax = currentSong.getRatingMax();

		if (rating == Song.RATING_NONE)
			return;

		y = height;

		if (rating < 0 || rating > ratingMax) {

			g.drawString(Integer.toString(rating), width / 2, y,
					Graphics.HCENTER | Graphics.BOTTOM);

		} else {

			rsw = Theme.img[Theme.IMGID_RATE_OFF].getWidth();
			x = (width - rsw * currentSong.getRatingMax()) / 2;
			for (i = 1; i <= rating; i++) {
				g.drawImage(Theme.img[Theme.IMGID_RATE_ON], x + rsw * (i - 1),
						y, Graphics.LEFT | Graphics.BOTTOM);
			}
			for (i = rating + 1; i <= ratingMax; i++) {
				g.drawImage(Theme.img[Theme.IMGID_RATE_OFF], x + rsw * (i - 1),
						y, Graphics.LEFT | Graphics.BOTTOM);
			}

		}

	}

	private void updateVolumeBar() {

		Graphics g = seVolume.getImage().getGraphics();
		Point p = new Point(0, 0);
		int i;
		int width = seVolume.getImage().getWidth();
		int on = width * currentVolume / 100;

		for (i = 0; i <= on; i++) {
			drawImageX(g, Theme.img[Theme.IMGID_VOLUME_ON], p);
		}
		for (i = on; i < width; i++) {
			drawImageX(g, Theme.img[Theme.IMGID_VOLUME_OFF], p);
		}

	}

}
