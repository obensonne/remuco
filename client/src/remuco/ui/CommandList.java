package remuco.ui;

import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

/**
 * A screen to organize commands as a list.
 * <p>
 * To add commands as list items, call {@link #addCommand(Command, Image)} or
 * {@link #addCommand(Command, String, Image)}. To add commands traditionally,
 * use {@link #addCommand(Command)}. Both command types can be removed as usual
 * with {@link #removeCommand(Command)}.
 * 
 * @author Oben Sonne
 * 
 */
public class CommandList extends List implements CommandListener {

	/**
	 * External command listener to pass commands to, except the select command.
	 */
	private CommandListener externalCommandListener = null;

	private final Vector itemCommands, itemLabels, itemIcons;

	private final Command select;

	/**
	 * Create a new command list with a default select command.
	 * 
	 * @param title
	 *            list title
	 */
	public CommandList(String title) {
		this(title, null);
	}

	/**
	 * Create new command list.
	 * 
	 * @param title
	 *            list title
	 * @param select
	 *            the command to use as the select command (if <code>null</code>
	 *            then {@link CMD#CMD_SELECT} will be used)
	 */
	public CommandList(String title, Command select) {

		super(title, List.IMPLICIT);

		itemCommands = new Vector();
		itemLabels = new Vector();
		itemIcons = new Vector();

		this.select = select == null ? CMD.CMD_SELECT : select;

		addCommand(this.select);
		setSelectCommand(this.select);

		super.setCommandListener(this);

	}

	/**
	 * Add a command as a list item.
	 * 
	 * @param cmd
	 *            the command (which will be passed to the command listener if
	 *            this commands's list item has been selected)
	 * @param icon
	 *            an optional item image
	 */
	public void addCommand(Command cmd, Image icon) {
		addCommand(cmd, null, icon);
	}

	/**
	 * Add a command as a list item.
	 * 
	 * @param cmd
	 *            the command (which will be passed to the command listener if
	 *            this commands's list item has been selected)
	 * @param altLabel
	 *            if not <code>null</code>, this will be used as the item label
	 *            instead of <i>cmd</i>'s label
	 * @param icon
	 *            an optional item image
	 */
	public void addCommand(Command cmd, String altLabel, Image icon) {

		if (itemCommands.contains(cmd)) {
			return;
		}

		final int itemCount = itemCommands.size();

		int position;
		for (position = 0; position < itemCount; position++) {

			final Command cmdExistent = (Command) itemCommands
					.elementAt(position);

			if (cmd.getPriority() < cmdExistent.getPriority()) {
				break;
			}

		} // if 'cmd' has lowest priority then 'position' equals 'itemCount'

		final String label = altLabel != null ? altLabel : cmd.getLabel();

		itemCommands.insertElementAt(cmd, position);
		itemLabels.insertElementAt(label, position);
		itemIcons.insertElementAt(icon, position);

		update();
	}

	public void commandAction(Command c, Displayable d) {

		if (c == select) {

			final int index = getSelectedIndex();

			if (index < 0) {
				return;
			}

			Command cmdSelected = (Command) itemCommands.elementAt(index);

			if (externalCommandListener != null) {
				externalCommandListener.commandAction(cmdSelected, this);
			}

		} else {

			externalCommandListener.commandAction(c, d);
		}

	}

	/**
	 * Remove a command (also valid for commands added as items). If <i>cmd</i>
	 * has been added both, as an item and as a traditional command, then both
	 * will be removed.
	 */
	public void removeCommand(Command cmd) {

		final int index = itemCommands.indexOf(cmd);

		if (index >= 0) {
			itemCommands.removeElementAt(index);
			itemLabels.removeElementAt(index);
			itemIcons.removeElementAt(index);
			update();
		}

		super.removeCommand(cmd); // must not be in 'else' branch
	}

	public void setCommandListener(CommandListener l) {
		externalCommandListener = l;
	}

	/**
	 * Update the list to show all item commands in priority dependent order.
	 */
	private void update() {

		deleteAll();

		final int itemCount = itemCommands.size();

		for (int i = 0; i < itemCount; i++) {

			final String label = (String) itemLabels.elementAt(i);
			final Image icon = (Image) itemIcons.elementAt(i);
			append(label, icon);

		}

	}

}
