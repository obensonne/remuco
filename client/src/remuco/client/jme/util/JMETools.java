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
package remuco.client.jme.util;

import javax.microedition.lcdui.Image;

import remuco.util.Log;

/**
 * Java ME related utility methods.
 */
public class JMETools {

	/**
	 * Create an image from its byte array representation.
	 * 
	 * @param ay
	 *            the bate array
	 * @return an image or <code>null</code> if <em>ay</em> is <code>null</code>
	 *         or empty or if <em>ay</em> is malformed
	 */
	public static Image baToImage(byte ay[]) {

		if (ay != null && ay.length > 0) {
			try {
				return Image.createImage(ay, 0, ay.length);
			} catch (Exception e) {
				Log.ln("[IT] creating image failed", e);
				return null;
			}
		} else {
			return null;
		}
	}

}
