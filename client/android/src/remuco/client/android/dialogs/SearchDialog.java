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

import remuco.client.android.PlayerAdapter;
import remuco.client.android.R;
import remuco.client.android.RemucoLibrarySearch;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class SearchDialog extends Dialog implements OnClickListener{

	PlayerAdapter player;
    String[] mask;

	Button okButton;
	Button cancelButton;

	EditText query[];
	
	public SearchDialog(Context context, PlayerAdapter player) {
		super(context);
		this.player = player;

        if (player == null || player.getPlayer() == null)
            return;

        mask = player.getPlayer().info.getSearchMask();
        query = new EditText[mask.length];
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {

		setContentView(R.layout.search_dialog);
		setTitle(R.string.search_dialog_title);
	
		// get references
		okButton = (Button) findViewById(R.id.search_dialog_ok_button);
		cancelButton = (Button) findViewById(R.id.search_dialog_cancel_button);

        TableLayout table = (TableLayout) findViewById(R.id.search_dialog_root_layout);

        if (mask == null) return;

        for (int i = 0; i < mask.length; i++) {
            final TableRow tr = new TableRow(this.getContext());

            final String field = mask[i];
            final TextView tv = new TextView(this.getContext());
            tv.setText(field);

            query[i] = new EditText(this.getContext());

            tr.addView(tv);
            tr.addView(query[i]);
            table.addView(tr);
        }

		// setup listener
		okButton.setOnClickListener(this);
		cancelButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		
		if(v == okButton){
			this.dismiss();
            if (mask == null) return;
            final Intent intent = new Intent(this.getContext(), RemucoLibrarySearch.class);
            for (int i = 0; i < mask.length; i++) {
                final String field = mask[i];
                intent.putExtra(SearchDialog.class.getName() + "_" + field, query[i].getText().toString());
            }
            this.getContext().startActivity(intent);
		}
		
		if(v == cancelButton){
			this.cancel();
		}
	}
}
