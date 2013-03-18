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

import remuco.client.android.dialogs.RatingDialog;
import remuco.client.android.fragments.PlayerFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class Remuco extends RemucoActivity {
	
	// -----------------------------------------------------------------------------
	// --- lifecycle methods
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// ------
		// android related initialization
		// ------
		setContentView(R.layout.main);
	    if (savedInstanceState == null) {
	        FragmentManager fragmentManager = getSupportFragmentManager();
	        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
	        PlayerFragment fragment = new PlayerFragment();
	        fragmentTransaction.add(R.id.fragment_container, fragment);
	        fragmentTransaction.commit();
	    }
	
	}
	

	// --- Options Menu
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.options_menu, menu);
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        if (super.onOptionsItemSelected(item)) return true;
		
		switch(item.getItemId()){
		
		case R.id.options_menu_library:
            final Intent intent = new Intent(this, RemucoLibraryTab.class);
            startActivity(intent);
			return true;
			
		case R.id.options_menu_rate:
			showRateDialog();
			return true;
		}
		
		return false;
	}
	
	// ------------------------
	// --- dialogs
	// ------------------------
	
	private void showRateDialog() {
	    FragmentManager fm = getSupportFragmentManager();
	    RatingDialog ratingdialog = RatingDialog.newInstance(player);
	    ratingdialog.show(fm, "ratingdialog");
	}
	
}
