package remuco.ui.screens;

import java.io.IOException;
import java.util.EmptyStackException;
import java.util.Stack;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

import remuco.player.IPlobRequestor;
import remuco.player.IPloblistRequestor;
import remuco.player.Player;
import remuco.player.Plob;
import remuco.player.PlobList;
import remuco.ui.UI;

public final class LibraryScreen extends List implements CommandListener,
		IPloblistRequestor, IPlobRequestor {

	private static final Command CMD_PLAY = new Command("Play", Command.ITEM,
			20);

	private static final Command CMD_SHOW = new Command("Show", Command.ITEM,
			10);

	private final Display display;

	private final Image iconPlob, iconPloblist;

	private final CommandListener parent;

	private final Player player;

	private final Plob plob;

	private final Stack ploblistHistory;

	private final PlobInfoScreen screenPlobInfo;

	/**
	 * Alert that indicates a request to the server has issued and the client is
	 * waiting for the reply.
	 */
	private final WaitingScreen screenWaiting;

	public LibraryScreen(CommandListener parent, Display display, Player player) {

		super("Library", IMPLICIT);

		Image icon;

		this.display = display;
		this.parent = parent;
		this.player = player;

		setCommandListener(this);

		plob = new Plob();

		screenPlobInfo = new PlobInfoScreen();
		screenPlobInfo.addCommand(UI.CMD_BACK);
		screenPlobInfo.setCommandListener(this);

		screenWaiting = new WaitingScreen();
		screenWaiting.setTitle("Updating");
		screenWaiting.setCommandListener(this);

		ploblistHistory = new Stack();

		try {
			icon = Image.createImage("/plob.png");
		} catch (IOException e) {
			icon = Image.createImage(12, 12);
			icon.getGraphics().setColor(0);
			icon.getGraphics().drawString("P", 2, 2,
					Graphics.TOP | Graphics.LEFT);
		}
		iconPlob = icon;

		try {
			icon = Image.createImage("/ploblist.png");
		} catch (IOException e) {
			icon = Image.createImage(12, 12);
			icon.getGraphics().setColor(0);
			icon.getGraphics().drawString("L", 2, 2,
					Graphics.TOP | Graphics.LEFT);
		}
		iconPloblist = icon;

	}

	public void commandAction(Command c, Displayable d) {

		int i;
		PlobList pl;

		try {
			pl = (PlobList) ploblistHistory.peek();
		} catch (EmptyStackException e) {
			parent.commandAction(UI.CMD_BACK, this);
			return;
		}

		if (c == CMD_PLAY) {

			i = getSelectedIndex();

			if (getImage(i) == iconPloblist) {
				player.ctrlJump(pl.getNestedID(i), 0);
			} else {
				player.ctrlJump(pl.getID(), i);
			}

			parent.commandAction(UI.CMD_BACK, this);

		} else if (c == CMD_SHOW) {

			i = getSelectedIndex();

			if (getImage(i) == iconPloblist) { // show ploblist

				player.reqPloblist(pl.getNestedID(i), new PlobList(), this);
				parent.commandAction(UI.CMD_BACK, this);

			} else { // show plobinfo

				player.reqPlob(pl.getPlobID(i), plob, this);
			}

			display.setCurrent(screenWaiting);

		} else if (c == UI.CMD_BACK && d == screenPlobInfo) {

			display.setCurrent(this);

		} else if (c == UI.CMD_BACK && d == this) {

			ploblistHistory.pop(); // empty stack exc. caught by peek above

			if (ploblistHistory.isEmpty()) {

				parent.commandAction(UI.CMD_BACK, this);

			} else {

				// TODO: need another thread to display a waiting screen
				updateList();
			}

		} else if (c == WaitingScreen.CMD_CANCEL) { // canceled a ploblist/plob
			// request

			// if we have not yet a ploblist, we won't be here .. see peek above
			display.setCurrent(this);

		} else {

			parent.commandAction(c, d);
		}
	}

	public void handlePlob(Plob p) {

		if (!screenWaiting.isShown())
			// user has canceled plob request
			return;

		screenPlobInfo.setPlob(p);

		display.setCurrent(screenPlobInfo);
	}

	public void handlePloblist(PlobList pl) {

		if (!screenWaiting.isShown())
			// user has canceled ploblist request
			return;

		ploblistHistory.push(pl);

		updateList();

		display.setCurrent(this);
	}

	public void showYourself() {

		ploblistHistory.removeAllElements();

		display.setCurrent(screenWaiting);

		player.reqPloblist(null, new PlobList(), this);
	}

	private void updateList() {

		int len_nested, len;

		deleteAll();

		PlobList pl;

		pl = (PlobList) ploblistHistory.peek();

		setTitle(pl.getName());

		len_nested = pl.getNumNested();
		len = pl.getNumPlobs();

		if (len == 0 && len_nested == 0) {

			removeCommand(CMD_SHOW);
			removeCommand(CMD_PLAY);

		} else {

			addCommand(CMD_PLAY);
			addCommand(CMD_SHOW);
			setSelectCommand(CMD_SHOW);

			for (int i = 0; i < len_nested; i++) {
				append(pl.getNestedName(i), iconPloblist);
			}
			for (int i = 0; i < len; i++) {
				append(pl.getPlobName(i), iconPlob);
			}
		}
	}
}
