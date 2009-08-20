/*
 * Remuco - A remote control system for media players. Copyright (C) 2006-2009
 * Oben Sonne <obensonne@googlemail.com>
 * 
 * This file is part of Remuco.
 * 
 * Remuco is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Remuco is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Remuco. If not, see <http://www.gnu.org/licenses/>.
 */
package remuco;

/**
 * Interface for classes interested in notifications about changes of options.
 */
public interface IOptionListener {

	/**
	 * Check if the option listener is a session listener, i.e. it is only alive
	 * as long there is a player connection session.
	 * 
	 * @return <code>true</code> if this is a session only listener,
	 *         <code>false</code> if this is a listener which is alive the whole
	 *         application live time
	 */
	public boolean isSessionOptionListener();

	/**
	 * Notify the change of an option.
	 * 
	 * @param od
	 *            the changed option's descriptor
	 */
	public void optionChanged(OptionDescriptor od);

}
