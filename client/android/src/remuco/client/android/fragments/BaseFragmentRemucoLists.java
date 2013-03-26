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

import remuco.client.android.R;
import remuco.client.android.RemucoLibraryList;
import remuco.client.android.RequestHandlerCallback;
import remuco.client.android.util.LibraryItem;
import remuco.client.common.data.ItemList;
import remuco.client.common.util.Log;
import android.content.Context;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

/**
 * Base fragment for lists.
 */
public abstract class BaseFragmentRemucoLists extends BaseFragment implements RequestHandlerCallback {
    
    RemucoLibraryList mylib;
    ListView lv;
    LinearLayout navbuttons;
    Button navbuttonleft;
    Button navbuttonright;
    protected RequesterAdapter reqHandler;
    
    protected abstract RemucoLibraryList getLibrary(Context context);
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        
        super.onCreateView(inflater, container, savedInstanceState);
        Log.debug("--- " + this.getClass().getName() + ".onCreateView()");
        
        mylib = getLibrary(getActivity().getApplicationContext());
        View view = inflater.inflate(R.layout.playlist, container, false);
        
        navbuttons = (LinearLayout) view.findViewById(R.id.library_navigatebuttons);
        navbuttonleft = (Button) view.findViewById(R.id.library_prev_button);
        navbuttonleft.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mylib.loadList(mylib.getPage() - 1);
            }
        });
        
        
        navbuttonright = (Button) view.findViewById(R.id.library_next_button);
        navbuttonright.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mylib.loadList(mylib.getPage() + 1);
            }
        });
        
        lv = (ListView) view.findViewById(R.id.library_items);
        lv.setTextFilterEnabled(true);
        lv.setAdapter(mylib.getAdapter());
        lv.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                itemClicked(mylib.getItem(position));                
            }            
        });
        registerForContextMenu(lv);
        
        reqHandler = new RequesterAdapter(this);
        return view;
    }
    
    
    @Override
    public void onResume() {
        super.onResume();
        Log.debug("--- " + this.getClass().getName() + ".onResume()");
        player.addHandler(reqHandler);
        getList();
    }
    
    
    @Override
    public void onPause() {
        super.onPause();
        Log.debug("--- " + this.getClass().getName() + ".onPause()");
        player.removeHandler(reqHandler);
    }
    
    
    //
    //---  Context menu and item click methods
    //     In this base class there are no special elements to manage,
    //     let superclass and mylib do the work
     
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        mylib.onCreateContextMenu(menu, v, menuInfo);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(super.onContextItemSelected(item)) {
            return true;
        }
        return mylib.onContextItemSelected(item);
    }
    
    
    protected void itemClicked(LibraryItem i) {
        //Method can be overwritten to handle LibraryItem clicks.
        Log.debug("--- " + this.getClass().getName() + " list click");
    }
    
    /**
     * List methods
     */
    public void getList() {
        if(player.getPlayer() == null) return;
        mylib.loadList(mylib.getPage());
    }
    
    public void clearList() {
        mylib.clearList();
    }
    
    @Override
    public void setList(ItemList l) {
        mylib.setList(l);
        if(l.getPageMax() > 0) {
            navbuttons.setVisibility(View.VISIBLE);
            navbuttonleft.setEnabled(l.getPage() > 0);
            navbuttonright.setEnabled(l.getPage() < l.getPageMax());
        } else {
            navbuttons.setVisibility(View.GONE);
        }
    }

}
