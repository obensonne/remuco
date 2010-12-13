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
package remuco.client.android.util;

import android.util.Log;
import remuco.client.common.util.ILogPrinter;

public class AndroidLogPrinter implements ILogPrinter {

	/*
	 * This log printer uses Android's logging framework which is good when
	 * debugging. When running on real devices, it might be better to print
	 * logging output to a screen visible to the user.
	 */

	@Override
	public void println(String s) {
		if(s.startsWith("[DEBUG] ")){
			Log.d("Remuco", s);
			return;
		}
		
		if(s.startsWith("[BUG] ")){
			Log.e("Remuco", s);
		}
		
		Log.i("Remuco", s);
	}
	
	
}
