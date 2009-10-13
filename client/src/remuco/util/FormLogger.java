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
package remuco.util;

import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;

/**
 * A {@link remuco.util.ILogPrinter} implementation which prints out log
 * messages to a {@link Form}. This is useful for inspecting what happens on a
 * mobile device where STDOUT cannot be inspected.
 * 
 * @author Oben Sonne
 * 
 */
public final class FormLogger implements ILogPrinter {

	private final Form f;

	private int insertPos = 0;

	public FormLogger(Form f) {
		this.f = f;
	}

	private static int MAX_LOG_ELEMENTS = 70;

	public void print(String s) {
		checkFormSize();
		f.insert(insertPos, new StringItem(null, s));
		insertPos++;
		// f.append(s);
	}

	public void println(String s) {
		checkFormSize();
		f.insert(insertPos, new StringItem(null, s + "\n"));
		insertPos = 0;
		// f.append(s + "\n");
	}

	public void println() {
		checkFormSize();
		f.insert(insertPos, new StringItem(null, "\n"));
		insertPos = 0;
		// f.append("\n");
	}

	private void checkFormSize() {
		int size = f.size();
		if (size >= MAX_LOG_ELEMENTS) {
			for (int i = size - 1; i > size - 11; i--) {
				f.delete(i);
			}
		}
	}
}
