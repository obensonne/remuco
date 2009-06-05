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
package remuco.ui;

/**
 * An action listener listens for key actions as defined in {@link KeyBindings}.
 */
public interface IActionListener {

	/**
	 * Called if a key or the pointer has been pressed.
	 * 
	 * @param actionCode
	 *            the action key code (see {@link KeyBindings}) which is mapped
	 *            to the pressed key or to the screen area where the pointer has
	 *            been pressed
	 */
	public void handleActionPressed(int actionCode);

	/**
	 * Called if a key or the pointer has been pressed.
	 * 
	 * @param actionCode
	 *            the action key code (see {@link KeyBindings}) which is mapped
	 *            to the released key or to the pointer release event (usually
	 *            depends on where the pointer has been pressed previously)
	 */
	public void handleActionReleased(int actionCode);

}
