package remuco.ui.screens;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import remuco.player.IPloblistRequestor;
import remuco.player.Info;
import remuco.player.Library;
import remuco.player.Player;
import remuco.player.PlobList;
import remuco.ui.UI;

public final class LibraryScreen extends List implements CommandListener,
		IPloblistRequestor {

	private static final Command CMD_PLAY = new Command("Play", Command.ITEM,
			20);

	private static final Command CMD_SHOW_CONTENT = new Command("Content",
			Command.ITEM, 10);

	private final Display display;

	private Library lib;

	private final CommandListener parent;

	private final Player player;

	private final PloblistScreen screenPloblist;

	public LibraryScreen(CommandListener parent, Display display, Player player) {

		super("Library", IMPLICIT);

		this.display = display;
		this.parent = parent;
		this.player = player;

		setCommandListener(this);

		screenPloblist = new PloblistScreen(this, display, player);
		screenPloblist.addCommand(UI.CMD_BACK);

	}

	public void commandAction(Command c, Displayable d) {

		int i;

		if (c == CMD_PLAY) {

			i = getSelectedIndex();

			player.ctrlPlayPloblist(lib.getPloblistPlid(i));

			parent.commandAction(UI.CMD_BACK, d);

		} else if (c == CMD_SHOW_CONTENT) {

			i = getSelectedIndex();

			player.reqPloblist(lib.getPloblistPlid(i), this);

		} else if (c == UI.CMD_BACK && d == screenPloblist) {
			
			display.setCurrent(this);

		} else {

			parent.commandAction(c, d);

		}

	}

	public void handlePloblist(PlobList pl) {

		/*
		 * Ensure we are shown (may not be the case if the user pressed "back"
		 * right after he requested the content of a ploblist).
		 */
		if (isShown()) {

			screenPloblist.updatePloblist(pl);

			display.setCurrent(screenPloblist);
		}
	}

	public void updateLibrary(Library lib) {

		this.lib = lib;

		updateList();
	}

	private void updateList() {

		int len;

		deleteAll();

		len = (lib == null) ? 0 : lib.getLength();

		if (len == 0) {

			removeCommand(CMD_SHOW_CONTENT);
			removeCommand(CMD_PLAY);

		} else {

			setSelectCommand(CMD_PLAY);

			if (player.info.hasFeature(Info.FEATURE_LIBRARY_PL_CONTENT)) {
				addCommand(CMD_SHOW_CONTENT);
			} else {
				removeCommand(CMD_SHOW_CONTENT);
			}

			for (int i = 0; i < len; i++) {
				append(lib.getPloblistName(i), null);
			}
		}

	}

}
