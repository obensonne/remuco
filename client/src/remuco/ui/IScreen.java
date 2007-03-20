package remuco.ui;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;

import remuco.connection.RemotePlayer;
import remuco.data.IPlayerStateObserver;

/**
 * Generic interface for Screens.
 * 
 * @author Christian Buennig
 * 
 */
public interface IScreen extends CommandListener, IPlayerStateObserver {

	public static final String APP_PROP_UI = "remuco-ui";

	/**
	 * Command to delegate to the parent command listener if the screen has done
	 * what ever it wanted to do and the screen may be removed from the display.
	 */
	public static final Command CMD_DISPOSE = new Command("Exit", Command.OK,
			99);

	public static final String UI_CANVAS = "canvas";

	public static final String UI_SIMPLE = "simple";

	public Displayable getDisplayable();

	/**
	 * Inform the screen, if it is currently active, i.e. if it is shown on the
	 * display.
	 * 
	 */
	public void setActive(boolean active);

	/**
	 * Set up the screen.<br>
	 * The screen is expected to act as a command listener for its displayable.
	 * All commands it receives and which it dows not know must get delegated to
	 * the parent command listener. Further, the command {@link #CMD_DISPOSE}
	 * can get delegated to the parent command listener, to inform it, that this
	 * screen's tasks are done for now.
	 * 
	 * @param pcl
	 *            the parent command listener
	 * @param player
	 *            the player to send commands to (if needed) and to get the
	 *            player state from
	 */
	public void setUp(CommandListener pcl, Display d,
			RemotePlayer player);

}
