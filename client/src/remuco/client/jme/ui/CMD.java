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
package remuco.client.jme.ui;

import javax.microedition.lcdui.Command;

/** Generic commands. */
public class CMD {

	/**
	 * A generic back command. The general contract is that screens should not
	 * add this command to themselves. Parent screens are responsible to add
	 * this command to child screens and handle it when the user activates it.
	 * <p>
	 * Label is "Back" and priority is 80.
	 */
	public static final Command BACK = new Command("Back", Command.BACK, 80);
	/**
	 * A generic command for a selection.
	 * <p>
	 * Label is "Select" and priority is 1.
	 * <p>
	 * This command is an alternative to the default selection command of a list
	 * to prevent the behavior described <a href=
	 * "http://sourceforge.net/forum/forum.php?thread_id=1953173&forum_id=568227"
	 * >in this forum post</a>.
	 */
	public static final Command SELECT = new Command("Select", Command.ITEM, 1);
	/**
	 * A generic exit command. Every screen which wants to offer the user to
	 * Immediately exit the application should add this command to itself.
	 * However, hen this command gets activated by the user, the command action
	 * must be delegated back to each screen's parent screen. The root screen
	 * (which has no parent screen) is responsible for shutting down.
	 * <p>
	 * Label is "Exit" and priority is 100.
	 */
	public static final Command EXIT = new Command("Exit", Command.EXIT, 100);
	/**
	 * A generic command to show some information about the current screen. To
	 * be used by any screen which likes to give information about itself.
	 * <p>
	 * Label is "Info" and priority is 70.
	 */
	public static final Command INFO = new Command("Info", Command.HELP, 70);
	/**
	 * A command which displays the log. Every screen may add this command to
	 * itself. In this case this command's action must be delegated back to each
	 * screen's parent screen. The root screen (which has no parent screen) is
	 * responsible for displaying the log and redisplaying the displayable which
	 * originally recognized this command's action (for this purpose a reference
	 * to this displayable must always be forwarded when forwarding the command
	 * to the root screen).
	 * <p>
	 * Label is "Log" and priority is 90.
	 */
	public static final Command LOG = new Command("Log", Command.SCREEN, 90);
	/**
	 * A generic command for a negative reply.
	 * <p>
	 * Label is "No" and priority is 0.
	 */
	public static final Command NO = new Command("No", Command.CANCEL, 0);
	/**
	 * A generic command for a confirmation.
	 * <p>
	 * Label is "Ok" and priority is 10.
	 */
	public static final Command OK = new Command("Ok", Command.OK, 10);
	/**
	 * A generic command for a positive reply.
	 * <p>
	 * Label is "Yes" and priority is 0.
	 */
	public static final Command YES = new Command("Yes", Command.OK, 0);

}
