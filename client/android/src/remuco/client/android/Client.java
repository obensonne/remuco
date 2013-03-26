/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2013 by the Remuco team, see AUTHORS.
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
package remuco.client.android;

import java.util.Hashtable;

import android.view.Display;

import remuco.client.common.data.ClientInfo;
import remuco.client.common.util.Log;

public class Client {
    /**
     * Central utility method to create a client info.
     *
     * @param imgSize
     */
    public static ClientInfo buildClientInfo(Display d) {
        @SuppressWarnings("deprecation") //Deprecated as from API 13, but we support >= 8 
        int imgSize = Math.min(d.getWidth(), d.getHeight());        
        return buildClientInfo(imgSize);
    }
    
    /**
     * Central utility method to create a client info.
     *
     * @param imgSize
     */
    public static ClientInfo buildClientInfo(int imgSize) {
        
        // TODO: this should be configurable by a user
        // create extra info
        Hashtable<String,String> info = new Hashtable<String,String>();
        info.put("name", "Android Client on \"" + android.os.Build.MODEL + "\"");
        Log.ln("running on : " + android.os.Build.MODEL);
        // afaik every android (so far) has a touchscreen and is using unicode
        info.put("touch", "yes");
        info.put("utf8", "yes");
        return new ClientInfo(imgSize, "PNG", 50, info);
    }
    
}
