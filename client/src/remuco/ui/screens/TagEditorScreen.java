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
