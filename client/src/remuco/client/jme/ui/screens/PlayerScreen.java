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
package remuco.client.jme.ui.screens;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

import remuco.client.common.MainLoop;
import remuco.client.common.data.ClientInfo;
import remuco.client.common.data.Item;
import remuco.client.common.io.Message;
import remuco.client.common.player.Feature;
import remuco.client.common.player.IItemListener;
import remuco.client.common.player.IProgressListener;
import remuco.client.common.player.IStateListener;
import remuco.client.common.player.Player;
import remuco.client.common.serial.Serial;
import remuco.client.common.util.Log;
import remuco.client.jme.Config;
import remuco.client.jme.OptionDescriptor;
import remuco.client.jme.ui.CMD;
import remuco.client.jme.ui.CommandList;
import remuco.client.jme.ui.IActionListener;
import remuco.client.jme.ui.KeyBindings;
import remuco.client.jme.ui.RepeatedControl;
import remuco.client.jme.ui.Theme;
import remuco.client.jme.ui.screenies.ButtonBarScreeny;
import remuco.client.jme.ui.screenies.ImageScreeny;
import remuco.client.jme.ui.screenies.ItemScreeny;
import remuco.client.jme.ui.screenies.ProgressScreeny;
import remuco.client.jme.ui.screenies.Screeny;
import remuco.client.jme.ui.screenies.ScreenyException;
import remuco.client.jme.ui.screenies.StateScreeny;
import remuco.client.jme.ui.screenies.TitleScreeny;
import remuco.client.jme.ui.screens.OptionsScreen.IOptionListener;
import remuco.client.jme.util.JMETools;

public final class PlayerScreen extends Canvas implements IItemListener,
		IStateListener, IProgressListener, CommandListener, IActionListener,
		IOptionListener {

	public static final OptionDescriptor OD_IMG_KEEPFS = new OptionDescriptor(
			"keep-img-fs", "Image fullscreen persistent", "No", "Yes,No");

	/**
	 * A wrapper command for the command {@link CMD#BACK} added externally to
	 * this screen.
	 */
	private static final Command CMD_DISCONNECT = new Command("Disconnect",
			Command.SCREEN, 92);

	private static final Command CMD_KEYS = new Command("Key Bindings",
			Command.SCREEN, 3);

	private static final Command CMD_MEDIA = new Command("Media",
			Command.SCREEN, 1);

	private static final Command CMD_MENU = new Command("Menu", Command.BACK, 2);

	private static final Command CMD_OPTIONS = new Command("Options",
			Command.SCREEN, 2);

	private static final Command CMD_SHUTDOWN_HOST = new Command(
			"Shutdown Host", Command.SCREEN, 95);

	private static final int SEEK_DELAY = 600;

	private final Alert alertFeature;

	private final Config config;

	private final Display display;

	private CommandListener externalCommandListener;

	/** Indicates if the item's image is currently shown in fullscreen mode. */
	private boolean itemImageFullscreenActive;

	/**
	 * Indicates if the item's image (if there is one) is <em>supposed</em> to
	 * be shown in fullscreen mode.
	 */
	private boolean itemImageFullscreenEnabled;

	private final Player player;

	private RepeatedControl recoSeek;

	private RepeatedControl recoVolume = null;

	/** Screen to configure key setup */
	private final KeyBindingsScreen screenKeyConfig;

	/** Screen for browsing the remote player's media */
	private final MediaBrowserScreen screenMediaBrowser;

	private final CommandList screenMenu;

	private final OptionsScreen screenOptions;

	/** Screen to edit the meta information of a item */
	private final TagEditorScreen screenTagEditor;

	private boolean screenTooSmall = false;

	private final ButtonBarScreeny screenyButtons;

	private final ItemScreeny screenyItem;

	private final ImageScreeny screenyItemImageFullscreen;

	private final ProgressScreeny screenyProgress;

	private final StateScreeny screenyState;

	private final Theme theme;

	/**
	 * Create a new player screen.
	 * 
	 * @param display
	 *            display to use
	 * @param conn
	 *            connection to the remote player
	 * @param pinfo
	 *            information about the remote player
	 */
	public PlayerScreen(Display display, Player player) {

		this.display = display;

		theme = Theme.getInstance();

		this.player = player;

		player.setStateListener(this);
		player.setItemListener(this);
		player.setProgressListener(this);

		if (player.info.supportsMediaBrowser()) {
			super.addCommand(CMD_MEDIA);
		}
		super.addCommand(CMD_MENU);
		setCommandListener(this);

		screenMenu = new CommandList("Menu");
		screenMenu.addCommand(CMD_KEYS, theme.licKeys);
		if (player.info.supports(Feature.SHUTDOWN)) {
			screenMenu.addCommand(CMD_SHUTDOWN_HOST, theme.licOff);
		}
		screenMenu.addCommand(CMD.BACK);
		screenMenu.addCommand(CMD_OPTIONS, theme.licThemes);
		screenMenu.setCommandListener(this);

		screenyState = new StateScreeny(player.info);
		screenyItemImageFullscreen = new ImageScreeny(player.info);
		screenyItem = new ItemScreeny(player.info);
		screenyButtons = new ButtonBarScreeny(player.info);
		screenyProgress = new ProgressScreeny(player.info);

		screenKeyConfig = new KeyBindingsScreen(display);
		screenKeyConfig.addCommand(CMD.BACK);
		screenKeyConfig.setCommandListener(this);

		screenOptions = new OptionsScreen();
		screenOptions.addCommand(CMD.BACK);
		screenOptions.setCommandListener(this);

		screenTagEditor = new TagEditorScreen();
		screenTagEditor.addCommand(CMD.BACK);
		screenTagEditor.addCommand(CMD.OK);
		screenTagEditor.setCommandListener(this);

		alertFeature = new Alert("", "", null, AlertType.INFO);

		screenMediaBrowser = new MediaBrowserScreen(display, player);
		screenMediaBrowser.addCommand(CMD.BACK);
		screenMediaBrowser.setCommandListener(this);

		config = Config.getInstance();
		config.addOptionListener(this);

		/*
		 * Some devices do not call sizeChanged() when this screen is shown the
		 * first time. To ensure screenies are initialized properly, we call
		 * this here already.
		 */
		initScreenies();
	}

	/**
	 * All commands added here will be appended as item commands to an option
	 * screen.
	 */
	public void addCommand(Command cmd) {

		if (cmd == CMD.BACK) {

			screenMenu.addCommand(CMD_DISCONNECT, theme.licDisconnect);

		} else if (cmd == CMD.EXIT) {

			screenMenu.addCommand(cmd, theme.licOff);

		} else if (cmd == CMD.LOG) {

			screenMenu.addCommand(cmd, theme.licLog);

		} else {

			screenMenu.addCommand(cmd, theme.licItem);
		}

	}

	public void commandAction(Command c, Displayable d) {

		if (c == CMD_MEDIA) { // MEDIA //

			display.setCurrent(screenMediaBrowser);

		} else if (c == CMD.BACK && d == screenMediaBrowser) {

			display.setCurrent(this);

		} else if (c == CMD_MENU) { // OPTIONS //

			display.setCurrent(screenMenu);

		} else if (c == CMD.BACK && d == screenMenu) {

			display.setCurrent(this);

		} else if (c == CMD_DISCONNECT) {

			if (externalCommandListener != null) {
				externalCommandListener.commandAction(CMD.BACK, this);
			} else {
				Log.bug("Feb 2, 2009.7:30:08 PM");
			}

		} else if (c == CMD_OPTIONS) {

			display.setCurrent(screenOptions);

		} else if (c == CMD.BACK && d == screenOptions) {

			display.setCurrent(this);

		} else if (c == CMD_SHUTDOWN_HOST) {

			player.ctrlShutdownHost();

		} else if (c == CMD_KEYS) {

			display.setCurrent(screenKeyConfig);

		} else if (c == CMD.BACK && d == screenKeyConfig) { // KEYS //

			display.setCurrent(screenMenu);

		} else if (c == CMD.BACK && d == screenTagEditor) { // TAGS //

			display.setCurrent(this);

		} else if (c == CMD.OK && d == screenTagEditor) {

			final String pid = screenTagEditor.getPid();
			final String tags = screenTagEditor.getTags();

			player.ctrlSetTags(pid, tags);

			// if edited item is still current item, update current item
			if (player.item.getId().equals(pid)) {
				player.item.setTags(tags);
			}

			display.setCurrent(this);

		} else if (externalCommandListener != null) {

			externalCommandListener.commandAction(c, d);

		} else {

			Log.bug("Feb 2, 2009.6:46:34 PM (unhandled command)");
		}
	}

	public Player getPlayer() {
		return player;
	}

	public void handleActionPressed(int action) {

		final int rating, ratingMax;

		// Log.debug("[UI] pressed key "
		// + screenMain.getKeyName(key)
		// + " (id "
		// + key
		// + ") -> "
		// + (action != KeyBindings.ACTION_NOOP ?
		// KeyBindings.actionNames[action]
		// : "no action") + " (id " + action + ")");

		switch (action) {
		case KeyBindings.ACTION_VOLUP:

			if (!checkFeature(Feature.CTRL_VOLUME)) {
				break;
			}

			startVolumeControl(1);
			break;

		case KeyBindings.ACTION_VOLDOWN:

			if (!checkFeature(Feature.CTRL_VOLUME)) {
				break;
			}

			startVolumeControl(-1);
			break;

		case KeyBindings.ACTION_PLAYPAUSE:

			if (!checkFeature(Feature.CTRL_PLAYBACK)) {
				break;
			}

			player.ctrlPlayPause();
			break;

		case KeyBindings.ACTION_NEXT:

			startSeek(1);
			break;

		case KeyBindings.ACTION_PREV:

			startSeek(-1);
			break;

		case KeyBindings.ACTION_RATEDOWN:

			if (!checkFeature(Feature.CTRL_RATE)) {
				break;
			}

			ratingMax = player.info.getMaxRating();
			if (player.item.isNone() || ratingMax == 0)
				break;
			rating = player.item.getRating() == 0 ? ratingMax
					: player.item.getRating() - 1;
			player.item.setRating(rating);
			player.ctrlRate(rating);

			notifyItemChanged();

			break;

		case KeyBindings.ACTION_RATEUP:

			if (!checkFeature(Feature.CTRL_RATE)) {
				break;
			}

			ratingMax = player.info.getMaxRating();
			if (player.item.isNone() || ratingMax == 0)
				break;
			rating = player.item.getRating() == ratingMax ? 0
					: player.item.getRating() + 1;
			player.item.setRating(rating);
			player.ctrlRate(rating);

			notifyItemChanged();

			break;

		case KeyBindings.ACTION_VOLMUTE:

			if (!checkFeature(Feature.CTRL_VOLUME)) {
				break;
			}

			player.ctrlVolume(0);
			break;

		case KeyBindings.ACTION_IMAGE:

			final Image itemImage = JMETools.baToImage(player.item.getImg());
			if (itemImage == null) {
				break; // ignore fullscreen toggles when there is no image
			}
			itemImageFullscreenEnabled = !itemImageFullscreenEnabled;
			itemImageFullscreenActive = itemImageFullscreenEnabled;
			repaint(); // repaint all on fullscreen change

			break;

		case KeyBindings.ACTION_EDITTAGS:

			if (!checkFeature(Feature.CTRL_TAG)) {
				break;
			}

			final String currentTags = player.item.getMeta(Item.META_TAGS);

			screenTagEditor.set(player.item.getId(), currentTags);

			display.setCurrent(screenTagEditor);

			break;

		case KeyBindings.ACTION_REPEAT:

			if (!checkFeature(Feature.CTRL_REPEAT)) {
				break;
			}

			player.ctrlToggleRepeat();

			break;

		case KeyBindings.ACTION_SHUFFLE:

			if (!checkFeature(Feature.CTRL_SHUFFLE)) {
				break;
			}

			player.ctrlToggleShuffle();

			break;

		case KeyBindings.ACTION_FULLSCREEN:

			if (!checkFeature(Feature.CTRL_FULLSCREEN)) {
				break;
			}

			player.ctrlToggleFullscreen();

			break;

		case KeyBindings.ACTION_NOOP:

			break;

		default:

			Log.ln("[PLSC] unknown action: " + action);
			break;
		}

	}

	public void handleActionReleased(int action) {

		switch (action) {

		case KeyBindings.ACTION_VOLUP:
			stopVolumeControl();
			break;

		case KeyBindings.ACTION_VOLDOWN:
			stopVolumeControl();
			break;

		case KeyBindings.ACTION_NEXT: {
			final boolean notSeeked = stopSeek();
			if (notSeeked) { // key pressed for a short time -> no seek
				if (checkFeature(Feature.CTRL_NEXT)) {
					player.ctrlNext();
				}
			} else { // progress adjuster did some seeks, we are done already
				checkFeature(Feature.CTRL_SEEK);
			}
			break;
		}
		case KeyBindings.ACTION_PREV:
			final boolean notSeeked = stopSeek();
			if (notSeeked) { // key pressed for a short time -> no seek
				if (checkFeature(Feature.CTRL_PREV)) {
					player.ctrlPrev();
				}
			} else { // progress adjuster did some seeks, we are done already
				checkFeature(Feature.CTRL_SEEK);
			}
			break;

		default:
			break;
		}

	}

	public boolean isSessionOptionListener() {
		return true;
	}

	public void notifyItemChanged() {

		screenyItem.updateData(player.item);
		screenyItemImageFullscreen.updateData(player.item.getImg());

		if (!itemImageFullscreenEnabled) {
			repaint(screenyItem);
		} else {
			itemImageFullscreenEnabled = config.getOption(OD_IMG_KEEPFS)
					.equalsIgnoreCase("yes");
			itemImageFullscreenActive = itemImageFullscreenEnabled
					&& player.item.getImg() != null;
			repaint(screenyItemImageFullscreen);
		}
	}

	public void notifyProgressChanged() {

		screenyProgress.updateData(player.progress);
		repaint(screenyProgress);

	}

	public void notifyStateChanged() {

		screenyState.updateData(player.state);
		repaint(screenyState);
	}

	public void optionChanged(OptionDescriptor od) {

		if (od == Theme.OD_THEME) {
			
			theme.load(config.getOption(Theme.OD_THEME));
			initScreenies();
			
		} else if (od == TitleScreeny.OD_INFO_LEVEL) {
			
			initScreenies();
			
		} else if (od == ClientInfo.OD_IMG_SIZE
				|| od == ClientInfo.OD_PAGE_SIZE
				|| od == ClientInfo.OD_IMG_TYPE) {

			final Message m = new Message();
			m.id = Message.CONN_CINFO;
			m.data = Serial.out(new ClientInfo(false));
			player.getConnection().send(m);

		} else if (od == Config.OD_PING) {
			
			// FIXME: semantically this could be handled elsewhere .. where?
			
			final int interval = Integer.parseInt(config.getOption(Config.OD_PING));
			player.getConnection().setPing(interval);
		}
	}

	public void setCommandListener(CommandListener l) {
		if (l == this) {
			super.setCommandListener(l);
		} else {
			externalCommandListener = l;
		}
	}

	protected void keyPressed(int key) {

		handleActionPressed(KeyBindings.getInstance().getActionForKey(key));
	}

	protected void keyReleased(int key) {

		handleActionReleased(KeyBindings.getInstance().getActionForKey(key));
	}

	/**
	 * This methods finally paints the player screen. Gets called externally -
	 * e.g. as a result of {@link #repaint(Screeny)} or {@link #repaint()}
	 * (which may get called by ourselves).
	 * 
	 * @param g
	 *            the graphics where we can draw our screen into
	 * @see Canvas
	 */
	protected void paint(Graphics g) {

		if (screenTooSmall) {
			drawScreenTooSmallMessage(g);
		} else if (itemImageFullscreenActive) {
			screenyItemImageFullscreen.draw(g);
		} else {
			screenyState.draw(g);
			screenyItem.draw(g);
			screenyProgress.draw(g);
			screenyButtons.draw(g);
		}
	}

	protected void pointerPressed(int x, int y) {

		if (screenTooSmall) { // ignore pointer events
			commandAction(CMD_OPTIONS, this);
		} else if (itemImageFullscreenActive) {
			handleActionPressed(KeyBindings.ACTION_IMAGE);
		} else {
			screenyState.pointerPressed(x, y, this);
			screenyItem.pointerPressed(x, y, this);
			screenyButtons.pointerPressed(x, y, this);
		}
	}

	protected void pointerReleased(int x, int y) {
		if (!itemImageFullscreenActive && !screenTooSmall) {
			screenyState.pointerReleased(x, y, this);
			screenyItem.pointerReleased(x, y, this);
			screenyButtons.pointerReleased(x, y, this);
		}
	}

	protected void sizeChanged(int w, int h) {

		super.sizeChanged(w, h);

		initScreenies();

		repaint(); // guess we need this
	}

	private boolean checkFeature(int feature) {

		final boolean enabled = player.info.supports(feature);

		if (!enabled) {

			final String msg;

			switch (feature) {

			case Feature.CTRL_FULLSCREEN:
				msg = "Sorry, toggling full screen mode is not possible.";
				break;
			case Feature.CTRL_NEXT:
				msg = "Sorry, skipping to the next item is not possible.";
				break;
			case Feature.CTRL_PREV:
				msg = "Sorry, skipping to the previous item is not possible.";
				break;
			case Feature.CTRL_PLAYBACK:
				msg = "Sorry, controlling playback is not possible.";
				break;
			case Feature.CTRL_REPEAT:
				msg = "Sorry, toggling repeat mode is not possible.";
				break;
			case Feature.CTRL_SHUFFLE:
				msg = "Sorry, toggling shuffle mode is not possible.";
				break;
			case Feature.CTRL_SEEK:
				msg = "Sorry, seeking is not possible.";
				break;
			case Feature.CTRL_RATE:
				msg = "Sorry, rating is not possible.";
				break;
			case Feature.CTRL_VOLUME:
				msg = "Sorry, volume control is not possible.";
				break;
			case Feature.CTRL_TAG:
				msg = "Sorry, tagging items is not possible.";
				break;
			default:
				Log.bug("unexpected feature check: " + feature);
				msg = "Sorry, this is not possible.";
				break;
			}

			alertFeature.setString(msg);
			display.setCurrent(alertFeature);
		}

		return enabled;
	}

	private void drawScreenTooSmallMessage(Graphics g) {

		g.setColor(0xFFFFFF);
		g.fillRect(0, 0, getWidth(), getHeight());

		g.setColor(0);
		g.setFont(Theme.FONT_SMALL);

		int y = getHeight() / 2 - Theme.FONT_SMALL.getHeight();
		g.drawString("The screen is too small", getWidth() / 2, y,
			Graphics.HCENTER | Graphics.BASELINE);

		y += Theme.FONT_SMALL.getHeight() * 2;

		g.drawString("for the theme " + config.getOption(Theme.OD_THEME) + "!",
			getWidth() / 2, y, Graphics.HCENTER | Graphics.BASELINE);

	}

	private void initScreenies() {

		int w, h, x, y, anchor;

		if (getWidth() == 0 || getHeight() == 0)
			return;

		try {

			x = 0;
			y = 0;
			w = getWidth();
			h = getHeight();
			anchor = Screeny.TOP_LEFT;
			screenyItemImageFullscreen.initRepresentation(x, y, anchor, w, h);

			// ////// playback, volume, repeat and shuffle ////// //

			anchor = Screeny.TOP_LEFT;
			x = 0;
			w = getWidth();
			h = getHeight() / 3; // max 1/4 for state
			y = 0;
			screenyState.initRepresentation(x, y, anchor, w, h);

			// ////// button bar ////// //

			anchor = Screeny.BOTTOM_LEFT;
			x = 0;
			w = getWidth();
			h = getHeight() / 3; // max 1/4 for button bar
			y = getHeight();
			screenyButtons.initRepresentation(x, y, anchor, w, h);

			// ////// progress ////// //

			anchor = Screeny.BOTTOM_LEFT;
			x = 0;
			w = getWidth();
			h = getHeight() / 4; // max 1/5 for progress
			y = screenyButtons.getPreviousY();
			screenyProgress.initRepresentation(x, y, anchor, w, h);

			// ////// item ////// //

			anchor = Screeny.TOP_LEFT;
			x = 0;
			w = getWidth();
			y = screenyState.getNextY();
			h = screenyProgress.getPreviousY() - y;
			screenyItem.initRepresentation(x, y, anchor, w, h);

			screenTooSmall = false;

		} catch (ScreenyException e) {

			screenTooSmall = true;
			Log.ln("[PD] screen too small", e);
		}
	}

	/** Request a repaint for the region occupied by a screeny. */
	private void repaint(Screeny s) {

		repaint(s.getPreviousX(), s.getPreviousY(), s.getWidth(), s.getHeight());
	}

	private void startSeek(int direction) {

		if (recoSeek != null) {
			recoSeek.cancel();
		}
		recoSeek = new RepeatedControl(RepeatedControl.SEEK, player, direction);
		MainLoop.schedule(recoSeek, SEEK_DELAY, 321);
	}

	private void startVolumeControl(int direction) {

		if (recoVolume != null) {
			recoVolume.cancel();
		}
		recoVolume = new RepeatedControl(RepeatedControl.VOLUME, player,
				direction);
		MainLoop.schedule(recoVolume, 0, 253);
	}

	/**
	 * Stop repeated seek task.
	 * 
	 * @return <code>true</code> if no seek done yet (see
	 *         {@link RepeatedControl#cancel()})
	 */
	private boolean stopSeek() {

		final boolean ret;
		if (recoSeek != null) {
			ret = recoSeek.cancel();
			recoSeek = null;
		} else {
			ret = true;
		}
		return ret;
	}

	private void stopVolumeControl() {

		if (recoVolume != null) {
			recoVolume.cancel();
			recoVolume = null;
		}
	}

}
