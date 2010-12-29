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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import remuco.client.android.util.LibraryItem;
import remuco.client.common.data.ActionParam;
import remuco.client.common.data.ItemList;
import remuco.client.common.util.Log;

public class RemucoLibraryMlib extends RemucoLibrary implements OnClickListener{

    String[] path = null;

	// -----------------------------------------------------------------------------
	// --- lifecycle methods
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		// ------
		// android related initialization
		// ------
		
		// --- set listeners
        lv.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    Log.debug("--- " + this.getClass().getName() + " list click");
                    LibraryItem i = mArrayAdapter.getItem(position);
                    if (!i.nested) return;
                    if (i.position == -1) {
                        path = list.getPathForParent();
                    } else {
                        path = list.getPathForNested(i.position);
                    }
                    RemucoLibraryMlib.this.getList();
                }
            });
    }
	
    public void sendAction(ActionParam action) {
        player.getPlayer().actionMediaLib(action);
    }

    public void getList(){
        if (player == null || player.getPlayer() == null) return;

		Log.debug("--- " + this.getClass().getName() + ".getMLib()");

        mArrayAdapter.clear();
        player.getPlayer().reqMLib(reqHandler, path, page);
    }

    @Override
    public void setList(ItemList l){
        super.setList(l);

        path = l.getPath();
    }
}
