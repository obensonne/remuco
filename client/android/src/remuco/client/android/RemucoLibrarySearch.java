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

import remuco.client.android.dialogs.SearchDialog;
import remuco.client.common.data.ActionParam;
import remuco.client.common.util.Log;

public class RemucoLibrarySearch extends RemucoLibrary implements OnClickListener{

    String[] query;
    String[] mask;

	// -----------------------------------------------------------------------------
	// --- lifecycle methods
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mask = player.getPlayer().info.getSearchMask();

        query = new String[mask.length];

        for (int i = 0; i < mask.length; i++) {
            final String field = mask[i];
            final String q = this.getIntent().getStringExtra(SearchDialog.class.getName() + "_" + field);
            query[i] = q.trim();
        }

        this.getList();
    }

    public void sendAction(ActionParam action) {
        player.getPlayer().actionSearch(action);
    }

    public void getList(){
        if (player == null || player.getPlayer() == null) return;

		Log.debug("--- " + this.getClass().getName() + ".getSearch()");

        mArrayAdapter.clear();
        player.getPlayer().reqSearch(reqHandler, query, page);
    }

}
