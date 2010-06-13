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

import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

public class SearchScreen extends Form {

	private final TextField fields[];
	
	private final String mask[];

	public SearchScreen(String mask[]) {
		super("Search");
		fields = new TextField[mask.length];
		for (int i = 0; i < mask.length; i++) {
			fields[i] = new TextField(mask[i], "", 256, TextField.ANY);
			append(fields[i]);
		}
		this.mask = mask;
	}

	public String[] getMask() {
		return mask;
	}

	public String[] getQuery() {

		final String query[] = new String[fields.length];
		for (int i = 0; i < query.length; i++) {
			query[i] = fields[i].getString().trim();
		}
		return query;

	}

}
