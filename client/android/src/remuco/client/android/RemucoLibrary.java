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

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import remuco.client.android.fragments.RequesterAdapter;
import remuco.client.android.util.LibraryAdapter;
import remuco.client.android.util.LibraryItem;
import remuco.client.common.data.AbstractAction;
import remuco.client.common.data.ActionParam;
import remuco.client.common.data.ListAction;
import remuco.client.common.data.ItemAction;
import remuco.client.common.data.ItemList;
import remuco.client.common.util.Log;

@Deprecated
public abstract class RemucoLibrary extends RemucoActivity implements OnClickListener, RequestHandlerCallback {

    // --- view handler
    protected RequesterAdapter reqHandler;

    // get view handles
    Button prevButton;
    Button nextButton;
    ListView lv;
    LibraryAdapter mArrayAdapter;
    ItemList list;

    int page = 0;
    int pagemax = 0;

    // -----------------------------------------------------------------------------
    // --- lifecycle methods
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ------
        // android related initialization
        // ------
        
        // --- load layout
        setContentView(R.layout.playlist);

        // --- get view handles
        getViewHandles();

        // --- create view handler
        reqHandler = new RequesterAdapter(this);

        // --- set listeners
        prevButton.setOnClickListener(this);
        prevButton.setClickable(false);
        nextButton.setOnClickListener(this);
        nextButton.setClickable(false);
    }
    
    private void getViewHandles() {
        // get view handles
        prevButton = (Button) findViewById(R.id.library_prev_button);
        nextButton = (Button) findViewById(R.id.library_next_button);
    
        mArrayAdapter = new LibraryAdapter(getApplicationContext(), R.layout.list_item);
        lv = (ListView) findViewById(R.id.library_items);
        lv.setTextFilterEnabled(true);
        lv.setAdapter(mArrayAdapter);
        registerForContextMenu(lv);
    }

    
    /**
     * this method gets called after on create
     */
    @Override
    protected void onResume() {
        super.onResume();
        
        Log.debug("--- " + this.getClass().getName() + ".onResume()");

        // --- register view handler at player
        player.addHandler(reqHandler);

        this.getList();
    }

    // --- Options Menu
    
    @Override
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

    @Override
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
            this.getList();
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
        ActionParam a;
        
        //FIXME
        if (list.isPlaylist() || list.isQueue()) {
            a = new ActionParam(actionid, itempos, itemids);
        } else {
            a = new ActionParam(actionid, list.getPath(), itempos, itemids);
        }
        this.sendAction(a);
        this.getList();
        return true;
    }

    public abstract void sendAction(ActionParam action);
    public abstract void getList();

    public void setList(ItemList l){
        int i = 0;

        list = l;
        pagemax = list.getPageMax();
        activateButtons();

        if ((list.isMediaLib() ||
             list.isFiles()) &&
            !list.isRoot()) {
            LibraryItem item = new LibraryItem();
            item.nested = true;
            item.position = -1;
            item.label = "..";
            mArrayAdapter.add(item);
        }

        for (int j = 0; j < list.getNumNested(); j++) {
            LibraryItem item = new LibraryItem();
            item.nested = true;
            item.position = j;
            item.label = list.getNested(j);
            mArrayAdapter.add(item);
        }

        while (!ItemList.UNKNWON.equals(list.getItemName(i))) {
            LibraryItem item = new LibraryItem();
            item.nested = false;
            item.position = i;
            item.label = list.getItemName(i);
            mArrayAdapter.add(item);
            i++;
        }
    }
    
    public void clearList() {
        mArrayAdapter.clear();
    }

    protected void activateButtons() {
        if (page == 0) {
            prevButton.setClickable(false);
        } else {
            prevButton.setClickable(true);
        }
        if (page + 1 > pagemax) {
            nextButton.setClickable(false);
        } else {
            nextButton.setClickable(true);
        }
    }

    @Override
    public void onClick(View v) {
        
        if(v == prevButton){
            page--;
            this.getList();
        }

        if(v == nextButton){
            page++;
            this.getList();
        }
    }
}
