package remuco.ui.screens;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

public final class PlayerListScreen extends List implements CommandListener {

	private final static Command CMD_CHOOSE = new Command("Choose",
			Command.ITEM, 1);

	private final CommandListener parent;
	
	public PlayerListScreen(CommandListener parent) {

		super("Players", IMPLICIT);

		this.parent = parent;
		
		setCommandListener(this);
	}

	public void commandAction(Command c, Displayable d) {

		if (c == CMD_CHOOSE) {

			parent.commandAction(List.SELECT_COMMAND, d);

		} else {
			parent.commandAction(c, d);
		}
	}

	public void set(String[] names) {

		deleteAll();

		for (int i = 0; i < names.length; i++) {
			append(names[i], null);
		}

		if (names.length > 0) {
			setSelectCommand(CMD_CHOOSE);
		} else {
			removeCommand(CMD_CHOOSE);
		}

	}

}
