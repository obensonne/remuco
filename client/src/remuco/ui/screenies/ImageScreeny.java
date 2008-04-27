package remuco.ui.screenies;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import remuco.player.IPlayerInfo;
import remuco.ui.Theme;

public final class ImageScreeny extends Screeny {

	public ImageScreeny(Theme theme, IPlayerInfo player) {
		super(theme, player);
	}

	protected void initRepresentation() throws ScreenyException {

		setImage(Image.createImage(width, height));

		// fill with background color

		g.setColor(theme.getColor(Theme.COLOR_BG));
		g.fillRect(0, 0, width, height);

	}

	protected void updateRepresentation() {

		Image img = (Image) data;

		g.setColor(theme.getColor(Theme.COLOR_BG));
		g.fillRect(0, 0, width, height);

		if (img == null)
			return;

		img = Theme.shrinkImageIfNeeded(img, width, height);

		g.drawImage(img, width / 2, height / 2, Graphics.HCENTER
				| Graphics.VCENTER);

	}

}
