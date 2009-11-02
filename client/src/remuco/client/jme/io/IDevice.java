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
package remuco.client.jme.io;

public interface IDevice {

	/** Char to separate fields in of a flattened device. */
	public static final char FIELD_SEP = '|';

	/** Char to separate devices in a flattened device list. */
	public static final char LIST_SEP = '\n';

	public static final char TYPE_BLUETOOTH = 'B';

	public static final char TYPE_WIFI = 'W';

	/** Get a descriptive name of the device. */
	public String getLabel();

	/**
	 * Get the device type, one of {@link #TYPE_WIFI} or {@link #TYPE_BLUETOOTH}
	 * . This must be the first character in a flattened representation of the
	 * device.
	 */
	public char getType();

}
