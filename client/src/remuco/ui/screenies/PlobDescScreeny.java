package remuco.ui.screenies;

import java.io.IOException;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import remuco.player.IPlayerInfo;
import remuco.player.Plob;
import remuco.ui.Theme;

/**
 * A screeny for textual meta informaton of a {@link Plob}. Represents a
 * plobs's title, artist, album, year, genre and duration.
 * 
 * @author Christian Buennig
 * 
 */
public final class PlobDescScreeny extends Screeny {

	private Image noPlobImage;

	private final Plob noPlob;

	private int colorBg, colorArtist, colorAlbum, colorTitle, colorText;

	public PlobDescScreeny(Theme theme, IPlayerInfo player) {

		super(theme, player);

		noPlob = new Plob();

		data = noPlob;

	}

	protected void initRepresentation() throws ScreenyException {

		Image i = Image.createImage(width, height);

		colorAlbum = theme.getColor(Theme.COLOR_ALBUM);
		colorArtist = theme.getColor(Theme.COLOR_ARTIST);
		colorBg = theme.getColor(Theme.COLOR_BG);
		colorText = theme.getColor(Theme.COLOR_TEXT);
		colorTitle = theme.getColor(Theme.COLOR_TITLE);

		try {
			noPlobImage = Image.createImage("/monkey.png");
		} catch (IOException e) {
			noPlobImage = Image.createImage(1, 1);
		}

		noPlobImage = Theme.shrinkImageIfNeeded(noPlobImage, width, height);

		noPlob.setMeta(Plob.META_TITLE, player.getName());

		setImage(i);
	}

	protected void dataUpdated() {

		if (data == null)
			data = noPlob;

	}

	protected void updateRepresentation() {

		Plob plob = (Plob) data;
		String[] sa;
		int x, y, yYGL, maxImgHeight, yImg;
		Image plobImg;

		// clear

		g.setColor(colorBg);
		g.fillRect(0, 0, width, height);

		if (plob == null) { // should not happen due to dataUpdated()

			g.drawImage(noPlobImage, width / 2, height / 2, Graphics.HCENTER
					| Graphics.VCENTER);

			return;
		}

		y = 0;

		if (plob.hasAbstract()) { // only basic plob info available

			g.setColor(colorArtist);
			g.setFont(Theme.FONT_ARTIST);
			sa = Theme.splitString(plob.getMeta(Plob.META_ABSTRACT), width - 4,
					Theme.FONT_ARTIST);
			y = (height - sa.length * Theme.FONT_ARTIST.getHeight()) / 2;
			y = drawStrings(sa, width, y);

			return;
		} // else: full plob info available

		// //// artist //////

		g.setColor(colorArtist);
		g.setFont(Theme.FONT_ARTIST);
		sa = Theme.splitString(plob.getMeta(Plob.META_ARTIST), width - 4,
				Theme.FONT_ARTIST);
		y = drawStrings(sa, width, y);

		// //// title //////

		g.setColor(colorTitle);
		g.setFont(Theme.FONT_TITLE);
		sa = Theme.splitString(plob.getMeta(Plob.META_TITLE), width - 4,
				Theme.FONT_TITLE);
		y = drawStrings(sa, width, y);

		// //// album //////

		g.setColor(colorAlbum);
		g.setFont(Theme.FONT_ALBUM);
		sa = Theme.splitString(plob.getMeta(Plob.META_ALBUM), width - 4,
				Theme.FONT_TITLE);
		y = drawStrings(sa, width, y);

		// //// genre, length, year and image //////

		yImg = y + Theme.FONT_SMALL.getHeight() / 2;
		yYGL = height - Theme.FONT_SMALL.getHeight() * 2;

		if (yYGL < y)
			return; // no space for year, genre, length (or image)

		g.setColor(colorText);
		g.setFont(Theme.FONT_SMALL);

		y = height - Theme.FONT_SMALL.getHeight();

		x = 0;
		g.drawString(plob.getMeta(Plob.META_YEAR), x, y, Graphics.LEFT
				| Graphics.BASELINE);

		x = width / 2;
		g.drawString(plob.getMeta(Plob.META_GENRE), x, y, Graphics.HCENTER
				| Graphics.BASELINE);

		x = width;
		g.drawString(plob.getLenFormatted(), x, y, Graphics.RIGHT
				| Graphics.BASELINE);

		plobImg = plob.getImg();

		if (plobImg == null) // no image to show
			return;

		maxImgHeight = yYGL - yImg;
		if (maxImgHeight < 30) // not enough space for an image
			return;

		plobImg = Theme.shrinkImageIfNeeded(plobImg, width, maxImgHeight);

		g.drawImage(plobImg, width / 2, yImg + maxImgHeight / 2,
				Graphics.HCENTER | Graphics.VCENTER);

	}

	private int drawStrings(String[] sa, int width, int y) {

		int i, saLen, fontHeight;

		saLen = sa.length;

		fontHeight = g.getFont().getHeight();

		for (i = 0; i < saLen; i++) {
			g.drawString(sa[i], width / 2, y, Graphics.TOP | Graphics.HCENTER);
			y += fontHeight;
		}

		return y;
	}
}
