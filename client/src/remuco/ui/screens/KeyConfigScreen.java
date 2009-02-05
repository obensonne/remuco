package remuco.ui.screens;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import remuco.ui.CMD;
import remuco.ui.IKeyListener;
import remuco.ui.Keys;

public final class KeyConfigScreen extends List implements CommandListener,
		IKeyListener {

	private static final Command CMD_RESET = new Command("Reset to defaults",
			Command.SCREEN, 30);

	/** The current action to set a key for. */
	private int actionToSet;
	
	private final Alert alertKeyConflict, alertReset;

	private final Display display;

	private final StringBuffer msgKeyConflict = new StringBuffer(60);

	private final CommandListener parent;

	private final KeySetScreen screenKeySet;

	/**
	 * The key selected to set for {@link #actionToSet}. This field has only a
	 * valid value if the selected key is already in use for another action --
	 * it is used for handling this situtation by interacting with the user.
	 */
	private int selectedKey;

	/**
	 * @param display
	 * @param parent
	 * @param player
	 */
	public KeyConfigScreen(final CommandListener parent, final Display display) {

		super("Key Configuration", IMPLICIT);

		this.display = display;
		this.parent = parent;

		screenKeySet = new KeySetScreen(this);

		addCommand(CMD_RESET);
		setCommandListener(this);

		for (int i = 0; i < Keys.actionNames.length; i++)
			append("", null);
		updateList();

		alertKeyConflict = new Alert("Key already in use!");
		alertKeyConflict.setType(AlertType.WARNING);
		alertKeyConflict.setTimeout(Alert.FOREVER);
		alertKeyConflict.addCommand(CMD.NO);
		alertKeyConflict.addCommand(CMD.YES);
		alertKeyConflict.setCommandListener(this);

		alertReset = new Alert("Please confirm:");
		alertReset.setString("Reset to defaults?");
		alertReset.setType(AlertType.WARNING);
		alertReset.addCommand(CMD.NO);
		alertReset.addCommand(CMD.YES);
		alertReset.setCommandListener(this);
	}

	public void commandAction(Command c, Displayable d) {

		int actionOld;
		
		if (c == CMD_RESET) {

			display.setCurrent(alertReset);

		} else if (c == CMD.YES && d == alertReset) {

			Keys.resetToDefaults();

			updateList();

			display.setCurrent(this);

		} else if (c == CMD.NO && d == alertReset) {

			display.setCurrent(this);

		} else if (c == List.SELECT_COMMAND) { // an action to set a key for
			// has been chosen

			actionToSet = getSelectedIndex();

			screenKeySet.configure(actionToSet);

			display.setCurrent(screenKeySet);

		} else if (c == CMD.YES && d == alertKeyConflict) {

			actionOld = Keys.unsetKey(selectedKey);
			Keys.setKeyForAction(actionToSet, selectedKey);

			updateList(actionToSet);
			if (actionOld >= 0)
				updateList(actionOld);

			display.setCurrent(this);

		} else if (c == CMD.NO && d == alertKeyConflict) {

			display.setCurrent(this);

		} else {

			parent.commandAction(c, d);
		}

	}

	public void keyPressed(int key) {

		int actionOld;
		String keyName;

		if (key == 0 || key == Keys.getKeyForAction(actionToSet)) {

			display.setCurrent(this);

		} else if (Keys.keyIsAlreadySet(key)) {

			selectedKey = key;

			actionOld = Keys.getActionForKey(key);
			keyName = screenKeySet.getKeyName(key);

			msgKeyConflict.delete(0, msgKeyConflict.length());
			msgKeyConflict.append("Key ").append(keyName);
			msgKeyConflict.append(" is already in use for '");
			msgKeyConflict.append(Keys.actionNames[actionOld]).append("'.");
			msgKeyConflict.append("\nDo you want to unset it from '");
			msgKeyConflict.append(Keys.actionNames[actionOld]);
			msgKeyConflict.append("' and use it for '");
			msgKeyConflict.append(Keys.actionNames[actionToSet]).append("' ?");
			alertKeyConflict.setString(msgKeyConflict.toString());

			display.setCurrent(alertKeyConflict);

		} else { // key is valid and free

			Keys.setKeyForAction(actionToSet, key);

			updateList(actionToSet);

			display.setCurrent(this);
		}
	}

	public void keyReleased(int key) {
		// ignore

	}

	/**
	 * Update the key name for all actions in the key configuration list.
	 * 
	 */
	private void updateList() {

		for (int action = 0; action < Keys.actionNames.length; action++) {

			updateList(action);

		}

	}

	/**
	 * Update the key name for an action in the key configuration list.
	 * 
	 * @param action
	 */
	private void updateList(int action) {

		int key;
		String keyName;

		key = Keys.getKeyForAction(action);
		keyName = (key != 0) ? screenKeySet.getKeyName(key) : "";
		set(action, Keys.actionNames[action] + ": " + keyName, null);

	}
}
