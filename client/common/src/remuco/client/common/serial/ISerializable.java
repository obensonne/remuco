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
package remuco.client.common.serial;

/**
 * Interface for classes supposed to get (de)serialized, i.e. send to and
 * received from the server.
 */
public interface ISerializable {

	/**
	 * Get the serial atoms of this serializable, i.e. all data fields to
	 * (de)serialize.
	 */
	public SerialAtom[] getAtoms();

	/**
	 * Notifies that the serial atoms has been updated, i.e. some data has been
	 * received and deserialized into the serial atoms of this object.
	 * 
	 * @throws BinaryDataExecption
	 *             if the data in the atoms is semantically malformed
	 */
	public void notifyAtomsUpdated() throws BinaryDataExecption;

}
