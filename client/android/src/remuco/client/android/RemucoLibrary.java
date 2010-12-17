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

import android.app.Dialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import remuco.client.android.dialogs.ActionDialog;
import remuco.client.common.data.ActionParam;
import remuco.client.common.data.ItemList;
import remuco.client.common.player.IRequester;
import remuco.client.common.util.Log;

public abstract class RemucoLibrary extends RemucoActivity implements OnClickListener{

	// --- view handler
	protected RequesterAdapter reqHandler;

	// get view handles
	Button prevButton;
	Button nextButton;
    ListView lv;
    ArrayAdapter<String> mArrayAdapter;
    ItemList list;
    ActionDialog actiondialog;

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
		setContentView(R.layout.library);

		// --- get view handles
		getViewHandles();

		// --- create view handler
        reqHandler = new RequesterAdapter(this);
		// --- register view handler at player
		player.addHandler(reqHandler);

        actiondialog = new ActionDialog(this);
		
		// --- set listeners
        lv.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    Log.debug("show Dialog Action " + list.getItemID(position) + " " + list.getItemPosAbsolute(position));
                    actiondialog.setList(list);
                    actiondialog.setListposition(position);
                    showDialog(ACTIONS_DIALOG);
                }
            });
		prevButton.setOnClickListener(this);
        prevButton.setClickable(false);
		nextButton.setOnClickListener(this);
        nextButton.setClickable(false);
    }
	
	private void getViewHandles() {
		// get view handles
		prevButton = (Button) findViewById(R.id.library_prev_button);
		nextButton = (Button) findViewById(R.id.library_next_button);
	
        mArrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1);
        lv = (ListView) findViewById(R.id.library_items);
        lv.setTextFilterEnabled(true);
        lv.setAdapter(mArrayAdapter);
    }

	// --- Options Menu
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.options_library_menu, menu);
		
		return true;
	}
	
    public abstract void sendAction(ActionParam action);
    public abstract void getList();

    public void setList(ItemList l){
        int i = 0;

        list = l;
        pagemax = list.getPageMax();
        activateButtons();

        while (!ItemList.UNKNWON.equals(list.getItemName(i))) {
            mArrayAdapter.add(list.getItemName(i));
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

	// ------------------------
	// --- dialogs
	// ------------------------
	
	@Override
	protected Dialog onCreateDialog(int id) {
        Dialog d = super.onCreateDialog(id);
        if (d != null) return d;

		switch(id){
		// --- action dialog
		case ACTIONS_DIALOG:
			return actiondialog;
		}
		return null;
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
