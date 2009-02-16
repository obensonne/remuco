package remuco.ui;

import javax.microedition.lcdui.Canvas;

import remuco.util.Log;

/**
 * Key configuration manager. Manages mappings between actions and key codes.
 * <p>
 * Note: Synchronizing the methods in this class is not needed since they only
 * get called by the MIDlet thread.
 * 
 * 
 * @author Oben Sonne
 * 
 */
public final class Keys {

	public static final int ACTION_NOOP = 100;

	/** Action code. */
	public static final int ACTION_PLAYPAUSE = 0, ACTION_NEXT = 1,
			ACTION_PREV = 2, ACTION_VOLUP = 3, ACTION_VOLDOWN = 4,
			ACTION_VOLMUTE = 5, ACTION_RATEUP = 6, ACTION_RATEDOWN = 7,
			ACTION_EDITTAGS = 8, ACTION_IMAGE = 9, ACTION_COUNT = 10;

	/**
	 * Mapping of action codes to their names.
	 */
	public static final String[] actionNames = new String[] { "Play/Pause",
			"Next", "Previous", "Volume up", "Volume down", "Volume mute",
			"Rate up", "Rate down", "Edit Tags", "Show image" };

	/**
	 * Key configuration. Format described at {@link #defaultConfig}. Initially
	 * it is the same as {@link #defaultConfig}.
	 */
	private static final int[] config = new int[] { Canvas.KEY_NUM5,
			Canvas.KEY_NUM8, Canvas.KEY_NUM2, Canvas.KEY_NUM6, Canvas.KEY_NUM4,
			Canvas.KEY_NUM1, Canvas.KEY_POUND, Canvas.KEY_STAR,
			Canvas.KEY_NUM3, Canvas.KEY_NUM0 };

	/**
	 * Default key configuration.
	 * <p>
	 * Format:<br>
	 * The key code at <code>i</code> is mapped to the action code
	 * <code>i</code>.
	 */
	private static final int[] defaultConfig = new int[] { Canvas.KEY_NUM5,
			Canvas.KEY_NUM8, Canvas.KEY_NUM2, Canvas.KEY_NUM6, Canvas.KEY_NUM4,
			Canvas.KEY_NUM1, Canvas.KEY_POUND, Canvas.KEY_STAR,
			Canvas.KEY_NUM3, Canvas.KEY_NUM0 };

	/**
	 * Set a new key configuration.
	 * 
	 * @param newConfig
	 *            the new configuration
	 * @return <code>false</code> if the new configuration is not valid (the
	 *         current configuration is not changed), <code>true</code>
	 *         otheriwse.
	 */
	public static boolean configure(int[] newConfig) {

		if (!check(newConfig)) {
			Log.ln("[KE] bad config, keep current");
			return false;
		}

		System.arraycopy(newConfig, 0, config, 0, config.length);

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
	public static int getActionForKey(int key) {

		for (int i = 0; i < ACTION_COUNT; i++) {
			if (key == config[i])
				return i;
		}

		return ACTION_NOOP;
	}

	/**
	 * Get the current configuration.
	 * 
	 * @return The current configuration. Do not alter this configuration
	 *         directly, use {@link #setKeyForAction(int, int)},
	 *         {@link #unsetKey(int)}, {@link #unsetKeyForAction(int)} or
	 *         {@link #configure(int[])} to change the config !
	 */
	public static int[] getConfiguration() {

		return config;

	}

	/**
	 * Get an actions key.
	 * 
	 * @param action
	 *            the action code
	 * @return the key code (<code>0</code> if no key is associated with the
	 *         action)
	 */
	public static int getKeyForAction(int action) {

		return config[action];

	}

	/**
	 * Check if a key is already set in the current configuration.
	 * 
	 * @param key
	 *            the key code to check for
	 * @return <code>true</code> if the key is set, <code>false</code>
	 *         otherwise.
	 */
	public static boolean keyIsAlreadySet(int key) {

		for (int i = 0; i < config.length; i++) {
			if (key == config[i]) {
				return true;
			}
		}

		return false;

	}

	public static void resetToDefaults() {
		System.arraycopy(defaultConfig, 0, config, 0, config.length);
	}

	/**
	 * Associate a key code with an action code. If <code>key == 0</code>, the
	 * behaviour is identical to {@link #unsetKeyForAction(int)} with param
	 * <code>action</code>.
	 * 
	 * @param action
	 *            the action code
	 * @param key
	 *            the key code
	 * @return <code>false</code> if the key code is already in use (the
	 *         configuration is not changed in that case), <code>true</code>
	 *         otherwise
	 */
	public static boolean setKeyForAction(int action, int key) {

		if (key != 0)
			if (keyIsAlreadySet(key))
				return false;

		config[action] = key;

		return true;

	}

	/**
	 * Release a key. Dissociates <code>key</code> from the action it is
	 * currently associated with (if any).
	 * 
	 * @param key
	 * @return the action the key has been associated with until now, or -1 if
	 *         the key has been free until now
	 */
	public static int unsetKey(int key) {

		for (int i = 0; i < config.length; i++) {
			if (key == config[i]) {
				config[i] = 0;
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
	public static void unsetKeyForAction(int action) {

		config[action] = 0;

	}

	/**
	 * Validate the current configuration, i.e. check if size of {@link #config}
	 * equals <code>2 * </code>{@link #ACTION_COUNT} and if no key code is used
	 * twice.
	 * 
	 * @return <code>true</code> if the configuration is valid,
	 *         <code>false</code> otherwise.
	 */
	private static boolean check(int[] config) {

		if (config == null) {
			Log.ln("[KE] check: null config");
			return false;
		}

		if (config.length != ACTION_COUNT) {
			Log.ln("[KE] check: wrong mapping length");
			return false;
		}

		for (int i = 0; i < config.length; i++) {
			if (config[i] == 0)
				continue;
			for (int j = i + 1; j < config.length; j++) {
				if (config[i] == config[j]) {
					Log.ln("[KE] check: key " + config[i] + " is set twice");
					return false;
				}
			}
		}

		return true;

	}

}
