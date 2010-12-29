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
package remuco.client.android;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import remuco.client.common.data.ActionParam;
import remuco.client.common.util.Log;

public class RemucoLibraryQueue extends RemucoLibrary implements OnClickListener{

	// -----------------------------------------------------------------------------
	// --- lifecycle methods

    public void sendAction(ActionParam action) {
        player.getPlayer().actionQueue(action);
    }

    public void getList(){
        if (player == null || player.getPlayer() == null) return;

		Log.debug("--- " + this.getClass().getName() + ".geQueue()");

        mArrayAdapter.clear();
        player.getPlayer().reqQueue(reqHandler, page);
    }

}
