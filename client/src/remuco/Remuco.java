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
package remuco;

import java.util.Timer;
import java.util.TimerTask;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import remuco.comm.Connection;
import remuco.ui.CMD;
import remuco.ui.UI;
import remuco.util.FormLogger;
import remuco.util.Log;

/**
 * MIDlet of the Remuco client.
 * <p>
 * <h1>Emulator Code</h1>
 * Some code is only used while running inside the WTK emulator. All
 * corresponding code is either tagged with <code>emulator</code> in its JavaDoc
 * or is located inside an if-statement block using the condition
 * {@link #EMULATION}.
 * 
 * @author Oben Sonne
 * 
 */
public class Remuco extends MIDlet implements CommandListener {

	/**
	 * @emulator If <code>true</code>, the client runs inside the WTK emulator.
	 */
	public static final boolean EMULATION;

	public static final String VERSION = "@VERSION@";

	/** Command for the log form to run the garbage collector */
	private static final Command CMD_RUNGC = new Command("Run GC",
			Command.SCREEN, 2);

	/** Command for the log form to show memory status */
	private static final Command CMD_SYSINFO = new Command("System",
			Command.SCREEN, 1);

	private static Timer GLOBAL_TIMER = null;

	static {
		EMULATION = "@EMULATION@".equalsIgnoreCase("true") ? true : false;
	}

	/**
	 * Get the global (main loop) timer.
	 * <p>
	 * The global timer is used for
	 * <ul>
	 * <li>handling events from a {@link Connection} (decoupled from the
	 * {@link Connection}'s receiver thread and from the threads calling methods
	 * on a {@link Connection}),
	 * <li>scheduling repetitive tasks,
	 * <li>scheduling delayed tasks and
	 * <li>synchronizing access to objects where critical race conditions may
	 * occur.
	 * </ul>
	 * Do not call {@link Timer#cancel()} on this timer - this is done once on
	 * application shutdown.
	 */
	public static Timer getGlobalTimer() {
		if (GLOBAL_TIMER == null) {
			GLOBAL_TIMER = new Timer();
			// periodically log if the global timer is still alive:
			GLOBAL_TIMER.schedule(new TimerTask() {
				private final long startTime = System.currentTimeMillis();

				public void run() {
					final long now = System.currentTimeMillis();
					final long seconds = (now - startTime) / 1000;
					Log.ln("main loop alive (" + seconds + ")");
				}
			}, 30000, 30000);
		}
		return GLOBAL_TIMER;
	}

	private static void cancelGlobalTimer() {

		if (GLOBAL_TIMER != null) {
			GLOBAL_TIMER.cancel();
			GLOBAL_TIMER = null;
		}

	}

	private final Alert alertLoadConfig;

	private final Alert alertSaveConfig;

	private final Config config;

	private final Display display;

	private Displayable displayableAfterLog;

	private final Form logForm;

	/** Alert displaying current memory status */
	private final Form logSysInfoForm;

	/**
	 * We need to know this since {@link #startApp()} might get called more than
	 * once (e.g. after we have been paused).
	 */
	private boolean startAppFirstTime = true;

	private final UI ui;

	public Remuco() {

		display = Display.getDisplay(this);

		// set up logging

		logForm = new Form("Log");
		logForm.addCommand(CMD.BACK);
		logForm.addCommand(CMD_SYSINFO);
		logForm.setCommandListener(this);
		if (EMULATION) {
			Log.ln("RUNING IN EMULATION MODE ..");
			logForm.append("Emulation -> logging goes to standard out!");
		} else {
			Log.setOut(new FormLogger(logForm));
		}

		// init configuration

		Config.init(this);

		config = Config.getInstance();

		// set up some displayables

		logSysInfoForm = new Form("System Info");
		logSysInfoForm.addCommand(CMD_RUNGC);
		logSysInfoForm.addCommand(CMD.BACK);
		logSysInfoForm.setCommandListener(this);

		alertLoadConfig = new Alert("Error");
		alertLoadConfig.setString("Erros while loading configuration. "
				+ "Please inspect the log for details! Configuration erros "
				+ "are normal, if you installed a new version of the client.");
		alertLoadConfig.setType(AlertType.ERROR);
		alertLoadConfig.setTimeout(Alert.FOREVER);
		alertLoadConfig.setCommandListener(this);

		alertSaveConfig = new Alert("Error");
		alertSaveConfig.setString("Erros while saving configuration."
				+ " Please inspect the log for details!");
		alertSaveConfig.setType(AlertType.ERROR);
		alertSaveConfig.setTimeout(Alert.FOREVER);
		alertSaveConfig.setCommandListener(this);

		// set up the start screen

		ui = new UI(this, display);

	}

	public void commandAction(Command c, Displayable d) {

		if (c == CMD.LOG) {

			displayableAfterLog = d;

			display.setCurrent(logForm);

		} else if (c == CMD.BACK && d == logSysInfoForm) {

			display.setCurrent(logForm);

		} else if (c == CMD_RUNGC && d == logSysInfoForm) {

			// run garbage collector

			System.gc();

			updateSysInfoForm();

		} else if (c == CMD_SYSINFO) {

			// show memory usage

			updateSysInfoForm();

			display.setCurrent(logSysInfoForm);

		} else if (c == CMD.BACK && d == logForm) {

			// display the displayable shown before the log

			if (displayableAfterLog != null)
				display.setCurrent(displayableAfterLog);
			else
				Log.ln("A BUG !! Do not know what to show after Log !?");

		} else if (c == CMD.EXIT) {

			if (!config.save()) {
				display.setCurrent(alertSaveConfig);
			} else {
				cancelGlobalTimer();
				notifyDestroyed();
			}

		} else if (c == Alert.DISMISS_COMMAND && d == alertLoadConfig) {

			// continue start up

			ui.show();

		} else if (c == Alert.DISMISS_COMMAND && d == alertSaveConfig) {

			// continue shut down

			cancelGlobalTimer();
			notifyDestroyed();

		} else {

			Log.ln("[ROOT] unexpected command: " + c.getLabel());

		}

	}

	protected void destroyApp(boolean unconditional)
			throws MIDletStateChangeException {

		/*
		 * This method only gets called by the application management (and not
		 * when the MIDLet gets shut down by the user)!
		 */

		config.save();
		cancelGlobalTimer();

	}

	protected void pauseApp() {

		Log.ln("[RM] paused");

	}

	protected void startApp() throws MIDletStateChangeException {

		if (startAppFirstTime) {

			startAppFirstTime = false;

			if (!config.loadedSuccessfully()) {
				display.setCurrent(alertLoadConfig);
				return;
			}

			ui.show();

			Log.ln("[RM] started");

		} else {

			Log.ln("[RM] unpaused");

		}

	}

	private void updateSysInfoForm() {

		final long memTotal = Runtime.getRuntime().totalMemory() / 1024;
		final long memFree = Runtime.getRuntime().freeMemory() / 1024;
		final long memUsed = memTotal - memFree;

		final StringBuffer sb = new StringBuffer(200);

		sb.append("--- Memory --- \n");
		sb.append("Total ").append(memTotal).append(" KB\n");
		sb.append("Used  ").append(memUsed).append(" KB\n");
		sb.append("Free  ").append(memFree).append(" KB\n");
		sb.append("--- Misc --- \n");
		sb.append("Version: ").append(VERSION);
		sb.append('\n');
		sb.append("UTF-8: ").append(Config.UTF8 ? "yes" : "no");
		sb.append('\n');
		sb.append("Device: ").append(Config.DEVICE_NAME);
		sb.append('\n');
		sb.append("Best list icon size: ").append(config.SUGGESTED_LICS);
		sb.append('\n');
		sb.append("Time: ").append(System.currentTimeMillis());
		sb.append('\n');

		logSysInfoForm.deleteAll();
		logSysInfoForm.append(sb.toString());

	}

}
