package remuco.ui.screens;

import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

/**
 * A Form to edit the tags of a {@link Item}.
 * 
 * @author Oben Sonne
 * 
 */
public final class TagEditorScreen extends Form {

	private String pid;

	private final TextField tagEditField = new TextField(
			"Attach a comma separted list of tags to the current track.", "",
			256, TextField.ANY);

	public TagEditorScreen() {

		super("Tag Editor");

		append(tagEditField);
	}

	/**
	 * Get the PID of the item this screen edits the tags of (previously set
	 * with {@link #set(String, String)}).
	 * 
	 * @return the PID
	 */
	public final String getPid() {
		return pid;
	}

	/**
	 * Get the edited tags.
	 * 
	 * @return the edited tags
	 */
	public String getTags() {

		return tagEditField.getString();
	}

	/**
	 * Set the PID of the item to be edit the tag of and the tags of this item.
	 * 
	 * @param pid
	 * @param tags
	 */
	public void set(String pid, String tags) {

		this.pid = pid;

		tagEditField.setString(tags);
	}

}
