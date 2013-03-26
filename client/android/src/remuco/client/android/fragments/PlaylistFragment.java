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
package remuco.client.android.fragments;

import remuco.client.android.RemucoLibraryList;
import remuco.client.common.data.ActionParam;
import android.content.Context;

public class PlaylistFragment extends BaseFragmentRemucoLists {

    @Override
    protected RemucoLibraryList getLibrary(Context context) {
        return new MyRemucoLibraryList(context);
    }

    private class MyRemucoLibraryList extends RemucoLibraryList {

        public MyRemucoLibraryList(Context context) {
            super(context);
        }

        @Override
        public void sendAction(ActionParam action) {
            player.getPlayer().actionPlaylist(action);
        }

        @Override
        public void loadList(int page) {
            player.getPlayer().reqPlaylist(reqHandler, page);
        }
    }

}
