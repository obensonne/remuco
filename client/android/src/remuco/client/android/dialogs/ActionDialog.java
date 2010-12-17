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
package remuco.client.android.dialogs;

import remuco.client.android.R;
import remuco.client.android.RemucoLibrary;
import remuco.client.common.data.AbstractAction;
import remuco.client.common.data.ActionParam;
import remuco.client.common.data.ItemAction;
import remuco.client.common.data.ItemList;
import remuco.client.common.util.Log;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

public class ActionDialog extends Dialog implements OnClickListener{

	Button cancelButton;

    ListView actionTV;
    RemucoLibrary library;
    ItemList list;
    int listposition;
	
	public ActionDialog(RemucoLibrary library) {
		super(library);
		this.library = library;
	}

    public void setList(ItemList l) {
        list = l;
    }

    public void setListposition(int p) {
        listposition = p;
    }
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		setContentView(R.layout.action_dialog);
		setTitle(R.string.action_dialog_title);
	
		// get references
		cancelButton = (Button) findViewById(R.id.action_dialog_cancel_button);
	
        actionTV = (ListView) findViewById(R.id.action_dialog_act);
        ArrayAdapter<String> mArrayAdapter = new ArrayAdapter<String>(this.getContext(), android.R.layout.simple_list_item_1);

        for (int i = 0; i < list.getActions().size(); i++) {
            AbstractAction act = (AbstractAction) list.getActions().elementAt(i);
            if (act.isItemAction())
                mArrayAdapter.add(act.label);
        }

        // Find and set up the ListView for paired devices
        actionTV.setAdapter(mArrayAdapter);
        actionTV.setTextFilterEnabled(true);

 		// --- set listeners
        actionTV.setOnItemClickListener(new OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    int actionid = ((ItemAction) list.getActions().elementAt(position)).id;
                    String[] itemids = new String[1];
                    itemids[0] = list.getItemID(listposition);
                    int[] itempos = new int[1];
                    itempos[0] = list.getItemPosAbsolute(listposition);
                    Log.debug("Action " + ((ItemAction) list.getActions().elementAt(position)).label + " " + list.getItemID(listposition) + " " + list.getItemPosAbsolute(listposition));
                    ActionParam a = new ActionParam(actionid, itempos, itemids);
                    library.sendAction(a);
                    ActionDialog.this.dismiss();
                }
            });
		cancelButton.setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		
		if(v == cancelButton){
			this.cancel();
		}
	}

}
