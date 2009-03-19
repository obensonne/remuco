package remuco.ui.screenies;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import remuco.player.Feature;
import remuco.player.PlayerInfo;
import remuco.player.Progress;
import remuco.ui.Theme;

/**
 * Sub screeny of an It
 * @author Oben Sonne
 *
 */
public class ProgressScreeny extends Screeny {

	private int colorBG = 0, colorFG;

	private int yOff = 0;

	public ProgressScreeny(PlayerInfo player) {

		super(player);

	}

	protected void initRepresentation() throws ScreenyException {

		if (!player.supports(Feature.KNOWN_PROGRESS)) {
			setImage(INVISIBLE);
			return;
		}

		final int fontHeight = Theme.FONT_PROGRESS.getHeight();

		setImage(Image.createImage(width, fontHeight * 3/2));

		yOff = fontHeight;

		g.setFont(Theme.FONT_PROGRESS);

		colorBG = theme.getColor(Theme.RTC_BG_ITEM);
		colorFG = theme.getColor(Theme.RTC_TEXT_OTHER);

	}

	protected void updateRepresentation() {

		g.setColor(colorBG);
		g.fillRect(0, 0, width, height);

		if (data == null) {
			return;
		}

		final Progress p = (Progress) data;

		final String len;
		if (p.getLength() < 0) {
			len = "???";
		} else {
			len = p.getLengthFormatted();
		}

		final String val;
		if (p.getProgress() < 0) {
			val = "???";
		} else {
			val = p.getProgressFormatted();
		}

		//Log.debug("update porgress values to " + val + "/" + len);
		
		g.setColor(colorFG);

		g.drawString(val, 0, yOff, Graphics.LEFT | Graphics.BASELINE);

		g.drawString(len, width, yOff, Graphics.RIGHT | Graphics.BASELINE);

	}

}
