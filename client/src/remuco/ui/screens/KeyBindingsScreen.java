/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2009 by the Remuco team, see AUTHORS.
 *
 *   This file is part of Remuco.
 *
 *   Remuco is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Remuco is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Remuco.  If not, see <http://www.gnu.org/licenses/>.
 *   
 */
package remuco.ui.screens;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import remuco.client.common.util.Log;
import remuco.ui.CMD;
import remuco.ui.IKeyListener;
import remuco.ui.KeyBindings;

/** Screen to configure key bindings. */
public final class KeyBindingsScreen extends List implements CommandListener,
		IKeyListener {

	private static final Command CMD_RESET = new Command("Reset",
			Command.SCREEN, 30);

	/** The current action to bind a key to. */
	private int actionToBind;

	private final Alert alertKeyConflict, alertReset;

	private final Display display;

	private CommandListener externalCommandListener;

	private final KeyBindings keyBindings;

	private final StringBuffer msgKeyConflict = new StringBuffer(60);

	private final KeyBinderScreen screenKeyBinder;

	/**
	 * The key selected to set for {@link #actionToBind}. This field has only a
	 * valid value if the selected key is already in use for another action --
	 * it is used for handling this situation by interacting with the user.
	 */
	private int selectedKey;

	public KeyBindingsScreen(final Display display) {

		super("Key Bindings", IMPLICIT);

		this.display = display;

		keyBindings = KeyBindings.getInstance();

		screenKeyBinder = new KeyBinderScreen(this);

		addCommand(CMD_RESET);
		setCommandListener(this);

		for (int i = 0; i < KeyBindings.actionNames.length; i++)
			append("", null);
		updateList();

		alertKeyConflict = new Alert("Key already in use!");
		alertKeyConflict.setType(AlertType.WARNING);
		alertKeyConflict.setTimeout(Alert.FOREVER);
		alertKeyConflict.addCommand(CMD.NO);
		alertKeyConflict.addCommand(CMD.YES);
		alertKeyConflict.setCommandListener(this);

		alertReset = new Alert("Please confirm:");
		alertReset.setString("Reset to default key bindings?");
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

			keyBindings.resetToDefaults();

			updateList();

			display.setCurrent(this);

		} else if (c == CMD.NO && d == alertReset) {

			display.setCurrent(this);

		} else if (c == List.SELECT_COMMAND) { // an action to set a key for
			// has been chosen

			actionToBind = getSelectedIndex();

			screenKeyBinder.configure(actionToBind);

			display.setCurrent(screenKeyBinder);

		} else if (c == CMD.YES && d == alertKeyConflict) {

			actionOld = keyBindings.release(selectedKey);
			keyBindings.bindKeyToAction(actionToBind, selectedKey);

			updateList(actionToBind);
			if (actionOld >= 0)
				updateList(actionOld);

			display.setCurrent(this);

		} else if (c == CMD.NO && d == alertKeyConflict) {

			display.setCurrent(this);

		} else if (externalCommandListener != null) {

			externalCommandListener.commandAction(c, d);

		} else {

			Log.bug("Aug 21, 2009.8:06:24 PM");
		}

	}

	public void keyPressed(int key) {

		int actionOld;
		String keyName;

		if (key == 0 || key == keyBindings.getKeyForAction(actionToBind)) {

			display.setCurrent(this);

		} else if (keyBindings.isBound(key)) {

			selectedKey = key;

			actionOld = keyBindings.getActionForKey(key);
			keyName = screenKeyBinder.getKeyName(key);

			msgKeyConflict.delete(0, msgKeyConflict.length());
			msgKeyConflict.append("Key ").append(keyName);
			msgKeyConflict.append(" is already in use for '");
			msgKeyConflict.append(KeyBindings.actionNames[actionOld]).append(
				"'.");
			msgKeyConflict.append("\nDo you want to unset it from '");
			msgKeyConflict.append(KeyBindings.actionNames[actionOld]);
			msgKeyConflict.append("' and use it for '");
			msgKeyConflict.append(KeyBindings.actionNames[actionToBind])
					.append("' ?");
			alertKeyConflict.setString(msgKeyConflict.toString());

			display.setCurrent(alertKeyConflict);

		} else { // key is valid and free

			keyBindings.bindKeyToAction(actionToBind, key);

			updateList(actionToBind);

			display.setCurrent(this);
		}
	}

	public void keyReleased(int key) {
		// ignore

	}

	public void setCommandListener(CommandListener l) {
		if (l == this) {
			super.setCommandListener(l);
		} else {
			externalCommandListener = l;
		}
	}

	/**
	 * Update the key name for all actions in the displayed key bindings list.
	 * 
	 */
	private void updateList() {

		for (int action = 0; action < KeyBindings.actionNames.length; action++) {

			updateList(action);

		}

	}

	/**
	 * Update the key name mapped to the given action in the displayed key
	 * bindings list.
	 * 
	 * @param action
	 */
	private void updateList(int action) {

		int key;
		String keyName;

		key = keyBindings.getKeyForAction(action);
		try {
			keyName = (key != 0) ? screenKeyBinder.getKeyName(key) : "";
		} catch (IllegalArgumentException e) {
			// may happen on version change or device firmware update
			keyBindings.release(key);
			keyName = "";
		}
		set(action, KeyBindings.actionNames[action] + ": " + keyName, null);
	}
}
