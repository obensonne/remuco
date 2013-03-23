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
import android.support.v4.app.Fragment;

/**
 * Base Fragment for Remuco fragments.
 * Holds a protected player object which can be accessed to interact with the
 * player. The user of this fragment can assume player != null.
 */
public class BaseFragment extends Fragment {
    protected PlayerAdapter player;
    
    @Override
    public void onResume() {
        super.onResume();
        
        try {
            PlayerProvider a = (PlayerProvider) getActivity();
            this.player = a.getPlayer();
            
        } catch(ClassCastException e) {
            Log.bug("-- BaseFragment gots an unsupported activity type, expected a PlayerProvider.");
            throw e;
        }
    }
}