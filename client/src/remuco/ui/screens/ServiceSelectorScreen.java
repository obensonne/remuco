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
package remuco.ui.screens;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.lcdui.List;

public class ServiceSelectorScreen extends List {

	private final Vector urls;

	public ServiceSelectorScreen() {

		super("Media Players", List.IMPLICIT);

		urls = new Vector();

	}

	/**
	 * Get the selected service.
	 * 
	 * @return the service URL
	 */
	public String getSelectedService() {

		final int index = getSelectedIndex();

		return (String) urls.elementAt(index);

	}

	public void setServices(Hashtable services) {

		deleteAll();

		urls.removeAllElements();

		final Enumeration e = services.keys();

		while (e.hasMoreElements()) {
			final String name = (String) e.nextElement();
			append(name, null);
			urls.addElement(services.get(name));
		}

	}

}
