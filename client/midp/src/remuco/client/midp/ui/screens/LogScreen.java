/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2010 by the Remuco team, see AUTHORS.
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
package remuco.client.midp.ui.screens;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;

import remuco.client.common.data.ClientInfo;
import remuco.client.common.util.ILogPrinter;
import remuco.client.common.util.Log;
import remuco.client.midp.Config;
import remuco.client.midp.ui.CMD;

public class LogScreen extends Form implements ILogPrinter, CommandListener {

	/** Command for the log form to run the garbage collector */
	private static final Command CMD_RUNGC = new Command("Run GC",
			Command.SCREEN, 2);

	/** Command for the log form to show memory status */
	private static final Command CMD_SYSINFO = new Command("System",
			Command.SCREEN, 1);

	private static int MAX_LOG_ELEMENTS = 70;

	private final Display display;

	private CommandListener externalCommandListener;

	private int insertPos = 0;

	private final Form sysInfoForm;

	/**
	 * Create a log screen.
	 * 
	 * @param display
	 */
	public LogScreen(Display display) {

		/*
		 * This is called before the configuration and the logging framework is
		 * initialized. Thus in this constructor they should not be used!
		 */

		super("Log");

		this.display = display;

		sysInfoForm = new Form("System Info");
		sysInfoForm.addCommand(CMD_RUNGC);
		sysInfoForm.addCommand(CMD.BACK);
		sysInfoForm.setCommandListener(this);

		addCommand(CMD_SYSINFO);
		setCommandListener(this);
	}

	public void commandAction(Command c, Displayable d) {

		if (c == CMD.BACK && d == sysInfoForm) {

			display.setCurrent(this);

		} else if (c == CMD_SYSINFO) {

			updateSysInfoForm();

			display.setCurrent(sysInfoForm);

		} else if (c == CMD_RUNGC) {

			System.gc();

			updateSysInfoForm();

		} else if (externalCommandListener != null) {

			externalCommandListener.commandAction(c, this);

		} else {

			Log.bug("Aug 18, 2009.16:15:09 AM");
		}
	}

	public void println(String s) {
		checkFormSize();
		insert(insertPos, new StringItem(null, s + "\n"));
		insertPos = 0;
		// f.append(s + "\n");
	}

	public void setCommandListener(CommandListener l) {
		if (l == this) {
			super.setCommandListener(l);
		} else {
			externalCommandListener = l;
		}
	}

	private void checkFormSize() {
		int size = size();
		if (size >= MAX_LOG_ELEMENTS) {
			for (int i = size - 1; i > size - 11; i--) {
				delete(i);
			}
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
		sb.append("Version: ").append(ClientInfo.VERSION);
		sb.append('\n');
		sb.append("UTF-8: ").append(Config.UTF8 ? "yes" : "no");
		sb.append('\n');
		sb.append("Device: ").append(Config.DEVICE_NAME);
		sb.append('\n');
		sb.append("Best list icon size: ");
		sb.append(display.getBestImageHeight(Display.LIST_ELEMENT));
		sb.append('\n');
		sb.append("Time: ").append(System.currentTimeMillis());
		sb.append('\n');

		sysInfoForm.deleteAll();
		sysInfoForm.append(sb.toString());

	}

}
