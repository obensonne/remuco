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

import android.content.Context;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import remuco.client.android.util.LibraryAdapter;
import remuco.client.android.util.LibraryItem;
import remuco.client.common.data.AbstractAction;
import remuco.client.common.data.ActionParam;
import remuco.client.common.data.ItemAction;
import remuco.client.common.data.ItemList;
import remuco.client.common.data.ListAction;
import remuco.client.common.util.Log;

public abstract class RemucoLibraryList {

    private LibraryAdapter mArrayAdapter;
    private ItemList list;

    public abstract void sendAction(ActionParam a);
    public abstract void loadList(int page);
    
    //paginator
    private int page = 0;
    private int maxpage = 0;
    
    public RemucoLibraryList(Context context) {
        System.out.println(context);
        System.out.println(R.layout.list_item);
        
        this.mArrayAdapter = new LibraryAdapter(context, R.layout.list_item);
    }
    
    public LibraryAdapter getAdapter() {
        return mArrayAdapter;
    }
    
    
    public LibraryItem getItem(int position) {
        return mArrayAdapter.getItem(position);
    }
    
    public void clearList() {
        mArrayAdapter.clear();
    }
    
    public int getPage() {
        return page;
    }
    
    public int getMaxPage() {
        return maxpage;
    }
    
    public void setList(ItemList l) {
        clearList();
        list = l;
        
        page = list.getPage();
        maxpage = list.getPageMax();
        
        for (int j = 0; j < list.getNumNested(); j++) {
            LibraryItem item = new LibraryItem();
            item.nested = true;
            item.position = j;
            item.label = list.getNested(j);
            mArrayAdapter.add(item);
        }

        int i = 0;
        while (!ItemList.UNKNWON.equals(list.getItemName(i))) {
            LibraryItem item = new LibraryItem();
            item.nested = false;
            item.position = i;
            item.label = list.getItemName(i);
            mArrayAdapter.add(item);
            i++;
        }
    }
    

    
    public void insert(LibraryItem item, int index) {
        mArrayAdapter.insert(item, index);
    }
    
    
    /**
     * Builds the context menu for items in our list.
     * The items are retrieved from ReMuCo, and are simply presented to
     * the user.
     */
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.library_items) {
            AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
            LibraryItem item = mArrayAdapter.getItem(info.position);

            if (item.position == -1) return; // no ContextMenu en folder '..'
            if (list.getActions().size() == 0) return;

            if (item.nested) {
                menu.setHeaderTitle(list.getNested(item.position));
            } else {
                menu.setHeaderTitle(list.getItemName(item.position));
            }
            for (int i = 0; i < list.getActions().size(); i++) {
                AbstractAction act = (AbstractAction) list.getActions().elementAt(i);
                if ( act.isItemAction() == item.nested ) 
                    continue;
                menu.add(Menu.NONE, i, i, act.label);
            }
        }
    }
    
    
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        LibraryItem libitem = mArrayAdapter.getItem(info.position);
        int menuItemIndex = item.getItemId();

        AbstractAction act = ((AbstractAction) list.getActions().elementAt(menuItemIndex));

        if (libitem.nested) {
            if (act.isItemAction()) {
                Log.debug("[ERROR] This is an item action, not applicable to lists.");
                return false;
            }
            int actionid = ((ListAction) act).id;
            String[] itemPath = list.getPathForNested(libitem.position);
            Log.debug("List Action " + ((ListAction) act).label + " " + list.getNested(libitem.position));

            ActionParam a = new ActionParam(actionid, itemPath, null, null);
            this.sendAction(a);
            this.loadList(page);
            return true;
        }

        if (!act.isItemAction()) {
            Log.debug("[ERROR] This is a list action, not applicable to items");
            return false;
        }
        int actionid = ((ItemAction) act).id;

        String[] itemids = new String[1];
        itemids[0] = list.getItemID(libitem.position);
        int[] itempos = new int[1];
        itempos[0] = list.getItemPosAbsolute(libitem.position);
        Log.debug("Item Action " + ((ItemAction) act).label + " " + list.getItemID(libitem.position) + " " + list.getItemPosAbsolute(libitem.position));
        //FIXME remove branch, init a correctly.
        ActionParam a;
        if (list.isPlaylist() || list.isQueue()) {
            a = new ActionParam(actionid, itempos, itemids);
        } else {
            a = new ActionParam(actionid, list.getPath(), itempos, itemids);
        }
        this.sendAction(a);
        
        this.loadList(page);
        return true;
    }
    
}
