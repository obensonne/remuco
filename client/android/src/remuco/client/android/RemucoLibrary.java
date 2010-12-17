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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

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
		
		// --- set listeners
        lv.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    // When clicked, show a toast with the TextView text
                }
            });
		prevButton.setOnClickListener(this);
        prevButton.setClickable(false);
		nextButton.setOnClickListener(this);
        nextButton.setClickable(false);

		// --- create view handler
        reqHandler = new RequesterAdapter(this);
		// --- register view handler at player
		player.addHandler(reqHandler);
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

    public abstract void getList();
    public abstract void setList(ItemList list);

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
