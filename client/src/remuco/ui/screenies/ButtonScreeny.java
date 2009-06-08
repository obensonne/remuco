package remuco.ui.screenies;

import javax.microedition.lcdui.Image;

import remuco.player.PlayerInfo;
import remuco.ui.IActionListener;

public class ButtonScreeny extends Screeny {

	private final int action;

	private Image img;

	private final int imgID;

	private boolean pressed = false;

	public ButtonScreeny(PlayerInfo player, int imgID, int action) {
		super(player);
		this.imgID = imgID;
		this.action = action;
	}

	public void pointerPressed(int px, int py, IActionListener actionListener) {
		if (!isInScreeny(px, py)) {
			return;
		}
		pressed = true;
		actionListener.handleActionPressed(this.action);
	}

	public void pointerReleased(int px, int py, IActionListener actionListener) {
		if (!pressed) {
			return;
		}
		pressed = false;
		actionListener.handleActionReleased(this.action);
	}

	protected void initRepresentation() throws ScreenyException {

		img = theme.getImg(imgID);
		setImage(img);
	}

	protected void updateRepresentation() {
	}

}
