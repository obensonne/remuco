/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2009 Oben Sonne <obensonne@googlemail.com>
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
package remuco.ui;

import javax.microedition.lcdui.Canvas;

import remuco.Config;
import remuco.util.Log;

/**
 * Key bindings manager. Manages mappings between actions and key codes.
 * <p>
 * Note: Synchronizing the methods in this class is not needed since they only
 * get called by the MIDlet thread.
 * 
 * 
 * @author Oben Sonne
 * 
 */
public final class KeyBindings {

	private static KeyBindings instance = null;

	public static final int ACTION_NOOP = 100;

	/** Action code. */
	public static final int ACTION_PLAYPAUSE = 0, ACTION_NEXT = 1,
			ACTION_PREV = 2, ACTION_VOLUP = 3, ACTION_VOLDOWN = 4,
			ACTION_VOLMUTE = 5, ACTION_RATEUP = 6, ACTION_RATEDOWN = 7,
			ACTION_EDITTAGS = 8, ACTION_IMAGE = 9, ACTION_REPEAT = 10,
			ACTION_SHUFFLE = 11, ACTION_FULLSCREEN = 12, ACTION_COUNT = 13;

	/**
	 * Mapping of action codes to their names.
	 */
	public static final String[] actionNames = new String[] { "Toggle playing",
			"Next", "Previous", "Volume up", "Volume down", "Volume mute",
			"Rate up", "Rate down", "Edit tags", "Show image", "Toggle repeat",
			"Toggle shuffle", "Toggle fullscreen" };

	/**
	 * Get the key bindings instance. <em>Must not</em> get called from a static
	 * context!
	 * 
	 * @return the key bindings singleton
	 */
	public static KeyBindings getInstance() {

		if (instance == null) {
			instance = new KeyBindings();
		}
		return instance;
	}

	/**
	 * Current key bindings. Format described at {@link #defaultBindings}.
	 */
	private final int[] bindings;

	/**
	 * Default key bindings.
	 * <p>
	 * Format:<br>
	 * The key code at <code>i</code> is mapped to the action code
	 * <code>i</code>.
	 */
	private static final int[] defaultBindings = new int[] { Canvas.KEY_NUM5,
			Canvas.KEY_NUM6, Canvas.KEY_NUM4, Canvas.KEY_NUM2, Canvas.KEY_NUM8,
			Canvas.KEY_NUM0, Canvas.KEY_POUND, Canvas.KEY_STAR,
			0 , Canvas.KEY_NUM1, Canvas.KEY_NUM7, Canvas.KEY_NUM9,
			Canvas.KEY_NUM3 };

	private KeyBindings() {

		final Config config = Config.getInstance();

		int kb[] = config.getKeyBindings();

		if (!validate(kb)) {
			kb = new int[defaultBindings.length];
			System.arraycopy(defaultBindings, 0, kb, 0, kb.length);
			config.setKeyBindings(kb);
		}

		bindings = kb;
	}

	/**
	 * Associate a key code with an action code. If <code>key == 0</code>, the
	 * behavior is identical to {@link #releaseForAction(int)} with param
	 * <code>action</code>.
	 * 
	 * @param action
	 *            the action code
	 * @param key
	 *            the key code
	 * @return <code>false</code> if the key code is already in use (bindings
	 *         are not changed in that case), <code>true</code> otherwise
	 */
	public boolean bindKeyToAction(int action, int key) {

		if (key != 0)
			if (isBound(key))
				return false;

		bindings[action] = key;

		return true;

	}

	/**
	 * Get the action code for a key.
	 * 
	 * @param key
	 *            the key to get the action for (must be one of the constants
	 *            defined in {@link Canvas}, e.g. {@link Canvas#KEY_NUM1} or
	 *            {@link Canvas#DOWN})
	 * @return The action code which has <code>key</code> associated as primary
	 *         or alternative key code. If no action is associated with
	 *         <code>key</code>, {@link #ACTION_NOOP} is returned.
	 */
	public int getActionForKey(int key) {

		for (int i = 0; i < ACTION_COUNT; i++) {
			if (key == bindings[i])
				return i;
		}

		return ACTION_NOOP;
	}

	/**
	 * Get an actions key.
	 * 
	 * @param action
	 *            the action code
	 * @return the key code (<code>0</code> if no key is associated with the
	 *         action)
	 */
	public int getKeyForAction(int action) {

		return bindings[action];

	}

	/**
	 * Check if a key is already bound to an action.
	 * 
	 * @param key
	 *            the key code to check
	 * @return <code>true</code> if the key is bound, <code>false</code>
	 *         otherwise.
	 */
	public boolean isBound(int key) {

		for (int i = 0; i < bindings.length; i++) {
			if (key == bindings[i]) {
				return true;
			}
		}

		return false;

	}

	/**
	 * Release a key. Dissociates <code>key</code> from the action it is
	 * currently associated with (if any).
	 * 
	 * @param key
	 * @return the action the key has been associated with until now, or -1 if
	 *         the key has been free until now
	 */
	public int release(int key) {

		for (int i = 0; i < bindings.length; i++) {
			if (key == bindings[i]) {
				bindings[i] = 0;
				return i;
			}
		}
		return -1;
	}

	/**
	 * Release the key used for an action.
	 * 
	 * @param action
	 */
	public void releaseForAction(int action) {

		bindings[action] = 0;

	}

	public void resetToDefaults() {
		System.arraycopy(defaultBindings, 0, bindings, 0, bindings.length);
	}

	/**
	 * Validate the given key bindings.
	 * 
	 * @param kb
	 *            the bindings to validate
	 * @return <code>true</code> if the bindings are valid, <code>false</code>
	 *         otherwise.
	 */
	private boolean validate(int[] kb) {

		if (kb == null || kb.length != defaultBindings.length) {
			Log.ln("[KB] key bindings malformed");
			return false;
		}

		for (int i = 0; i < kb.length; i++) {
			if (kb[i] == 0)
				continue;
			for (int j = i + 1; j < kb.length; j++) {
				if (kb[i] == kb[j]) {
					Log.ln("[KB] check: key " + kb[i] + " is set twice");
					return false;
				}
			}
		}

		return true;

	}

}
