package remuco.ui.screens;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

import remuco.ui.IKeyListener;
import remuco.ui.Keys;
import remuco.ui.Theme;

public final class KeySetScreen extends Canvas {

	/**
	 * Just to block the soft keys which are always used for some menus and
	 * therefore not suited to be used as hot keys.
	 */
	private static final Command CMD_BLOCK1 = new Command("BLOCKED",
			Command.OK, 0);

	/**
	 * Just to block the soft keys which are always used for some menus and
	 * therefore not suited to be used as hot keys.
	 */
	private static final Command CMD_BLOCK2 = new Command("BLOCKED",
			Command.BACK, 0);

	/**
	 * Just to block the soft keys which are always used for some menus and
	 * therefore not suited to be used as hot keys.
	 */
	private static final Command CMD_BLOCK3 = new Command("BLOCKED",
			Command.BACK, 0);

	private static final Font FONT = Theme.FONT_SMALL;

	private int actionCode;

	private final IKeyListener kl;

	private final StringBuffer textHint, textCurrent;

	public KeySetScreen(IKeyListener kl) {

		this.kl = kl;

		textHint = new StringBuffer(70);
		textCurrent = new StringBuffer(70);

		addCommand(CMD_BLOCK1);
		addCommand(CMD_BLOCK2);
		addCommand(CMD_BLOCK3);

	}

	public void configure(int actionCode) {

		this.actionCode = actionCode;

	}

	protected void keyPressed(int keyCode) {
		kl.keyPressed(keyCode);
	}

	protected void paint(Graphics g) {

		int key, y;
		String[] splitted;

		// set up textHint

		textHint.delete(0, textHint.length());
		textHint.append("Please press the key for action '");
		textHint.append(Keys.actionNames[actionCode]).append("'");

		textCurrent.delete(0, textCurrent.length());
		textCurrent.append("Currently ");

		key = Keys.getInstance().getKeyForAction(actionCode);

		if (key == 0)
			textCurrent.append("no key is set.");
		else
			textCurrent.append("key ").append(getKeyName(key)).append(
					" is used.");

		// draw

		g.setColor(0);
		g.fillRect(0, 0, getWidth(), getHeight());

		g.setColor(0xFFCC00);

		splitted = Theme.splitString(textHint.toString(), getWidth(), FONT);
		y = drawStrings(g, splitted, getWidth(), getHeight() / 2
				- FONT.getHeight());
		splitted = Theme.splitString(textCurrent.toString(), getWidth(), FONT);
		y = drawStrings(g, splitted, getWidth(), y);

	}

	private int drawStrings(Graphics g, String[] sa, int width, int y) {

		int i, saLen, fontHeight;

		saLen = sa.length;

		fontHeight = FONT.getHeight();

		g.setFont(FONT);

		for (i = 0; i < saLen; i++) {
			g.drawString(sa[i], width / 2, y, Graphics.TOP | Graphics.HCENTER);
			y += fontHeight;
		}

		return y;
	}

}
