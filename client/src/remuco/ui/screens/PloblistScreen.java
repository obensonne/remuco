package remuco.ui.screens;

import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import remuco.player.IPlobRequestor;
import remuco.player.Player;
import remuco.player.Plob;
import remuco.player.PlobList;
import remuco.ui.UI;

public final class PloblistScreen extends List implements CommandListener,
		IPlobRequestor {

	private static final Command CMD_PLOB_INFO = new Command("Info",
			Command.ITEM, 5);

	private final Display display;

	private final Vector itemCommands;

	private final CommandListener parent;

	private PlobList pl;

	private final Player player;

	private final PlobInfoScreen screenPlobInfo;

	private final Alert alertUpdate;
	
	private Command selectCommand;

	/**
	 * Screen to show a plob list.
	 * 
	 * @param display
	 * @param player
	 * @param parent
	 *            the command listener to delegate all commands to which has
	 *            been set previously with {@link #addCommand(Command)} <i>and</i>
	 *            the command {@link IUI#CMD_BACK}
	 */
	public PloblistScreen(CommandListener parent, Display display, Player player) {

		super("PlobList", List.IMPLICIT);

		itemCommands = new Vector();
		itemCommands.addElement(CMD_PLOB_INFO);

		super.setCommandListener(this);

		this.display = display;
		this.parent = parent;
		this.player = player;

		screenPlobInfo = new PlobInfoScreen();
		screenPlobInfo.addCommand(UI.CMD_BACK);
		screenPlobInfo.setCommandListener(this);
		
		alertUpdate = new Alert("Info");
		alertUpdate.setString("Updating list..");
		alertUpdate.setType(AlertType.INFO);

	}

	/**
	 * Add a command as in {@link List#addCommand(Command)} but only if the
	 * ploblist contains elements. Otherwise the command is stored and will be
	 * added once there is a list with some content (i.e. when
	 * {@link #updatePloblist(PlobList)} gets called with an non-empty
	 * ploblist).
	 */
	public void addCommand(Command cmd) {

		if (cmd != null && cmd.getCommandType() == Command.ITEM
				&& !itemCommands.contains(cmd)) {

			itemCommands.addElement(cmd);
			if (size() > 0)
				super.addCommand(cmd);

		} else {
			super.addCommand(cmd);
		}

	}

	public void commandAction(Command c, Displayable d) {

		if (c == CMD_PLOB_INFO) {

			player.reqPlob(pl.getPlobPid(getSelectedIndex()), this);

		} else if (c == UI.CMD_BACK && d == screenPlobInfo) {

			display.setCurrent(this);

		} else {

			parent.commandAction(c, d);

		}
	}

	public void handlePlob(Plob p) {

		/*
		 * Ensure we are shown (may not be the case if the user pressed "back"
		 * right after he requested some info about a plob).
		 */
		if (isShown()) {

			screenPlobInfo.setPlob(p);

			display.setCurrent(screenPlobInfo);
		}

	}

	/**
	 * @see List#removeCommand(Command)
	 * @see #addCommand(Command)
	 */
	public void removeCommand(Command cmd) {

		if (cmd.getCommandType() == Command.ITEM)
			itemCommands.removeElement(cmd);

		super.removeCommand(cmd);

	}

	/**
	 * @see List#setSelectCommand(Command)
	 * @see #addCommand(Command)
	 */
	public void setSelectCommand(Command cmd) {

		selectCommand = cmd;

		if (cmd != null && cmd.getCommandType() == Command.ITEM
				&& !itemCommands.contains(cmd)) {

			itemCommands.addElement(cmd);
			if (size() > 0)
				super.setSelectCommand(cmd);

		} else {
			super.setSelectCommand(cmd);
		}
	}

	public void setSelectedPlob(int nr) {

		if (nr >= 0 && nr < size()) {
			setSelectedIndex(nr, true);
		}
	}

	public void setSelectedPlob(String pid) {

		int len;

		len = pl.getLength();

		for (int i = 0; i < len; i++) {
			if (pl.getPlobPid(i).equals(pid))
				setSelectedIndex(i, true);
		}
	}

	public void updatePloblist(PlobList pl) {

		int len;
		Enumeration enu;

		this.pl = pl;

		if (isShown()) {
			display.setCurrent(alertUpdate, this);
		}
		
		deleteAll();

		setTitle(pl.getName());

		len = pl.getLength();

		// show or hide item dependent commands
		
		enu = itemCommands.elements();
		
		if (len == 0) {
			
			while (enu.hasMoreElements())
				super.removeCommand((Command) enu.nextElement());
			
		} else {

			while (enu.hasMoreElements())
				super.addCommand((Command) enu.nextElement());

			// set select command
			if (selectCommand != null)
				super.setSelectCommand(selectCommand);
			else
				super.setSelectCommand(CMD_PLOB_INFO);
		}

		for (int i = 0; i < len; i++)
			append(pl.getPlobTitle(i), null);
	}

}
