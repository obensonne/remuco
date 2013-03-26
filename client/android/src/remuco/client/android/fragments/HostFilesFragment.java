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
import remuco.client.android.util.LibraryItem;
import remuco.client.common.data.ActionParam;
import remuco.client.common.data.ItemList;
import remuco.client.common.util.Log;
import android.content.Context;

public class HostFilesFragment extends BaseFragmentRemucoLists {

    private ItemList loadedList;
    private String[] path = null;
    
    @Override
    protected RemucoLibraryList getLibrary(Context context) {
        return new MyRemucoLibraryList(context);
    }
    
    
    @Override
    protected void itemClicked(LibraryItem i) {
        super.itemClicked(i);
        if(!i.nested) {
            return;
        } else if (i.position == -1) {
            path = loadedList.getPathForParent();
        } else {
            path = loadedList.getPathForNested(i.position);
        }
        getList();
    }
    
    
    private class MyRemucoLibraryList extends RemucoLibraryList {

        public MyRemucoLibraryList(Context context) {
            super(context);
        }

        @Override
        public void sendAction(ActionParam action) {
            player.getPlayer().actionFiles(action);
        }

        @Override
        public void loadList(int page) {
            Log.debug("Path: "+path);
            player.getPlayer().reqFiles(reqHandler, path, page);
        }
        
        @Override 
        public void setList(ItemList l) {            
            super.setList(l);
            
            if(!l.isRoot()) {
                LibraryItem item = new LibraryItem();
                item.nested = true;
                item.position = -1;
                item.label = "..";
                insert(item, 0);
            }
            
            loadedList = l;
            path = l.getPath();
        }
        
    }

}
