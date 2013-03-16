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

package remuco.client.android.fragments;

import remuco.client.android.PlayerAdapter;
import remuco.client.android.PlayerProvider;
import remuco.client.common.util.Log;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;

/**
 * Base Fragment for Remuco fragments.
 * Holds a protected player object which can be accessed to interact with the
 * player.
 */
public class BaseFragment extends Fragment {
	protected PlayerAdapter player;  //TODO: Find a good way to get the player
	
	@Override
	public void onAttach(Activity a) {
		super.onAttach(a);
		if(PlayerProvider.class.isInstance(a)) {
			this.player = ((PlayerProvider) a).getPlayer();
		} else{
			Log.bug("-- BaseFragment gots an unsupported activity type, expected a PlayerProvider.");
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
}