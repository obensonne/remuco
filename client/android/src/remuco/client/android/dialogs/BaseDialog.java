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
package remuco.client.android.dialogs;

import remuco.client.android.PlayerAdapter;
import remuco.client.android.PlayerProvider;
import remuco.client.common.util.Log;
import android.support.v4.app.DialogFragment;

/**
 * Custom DialogFragment that ensures that a player variable is accessable
 * after the onResume() command.
 * 
 * The creating context (activity) of this dialog should implement the
 * PlayerProvider interface.
 */
public abstract class BaseDialog extends DialogFragment {

    /**
     * PlayerAdapter which can be used to interact with the player.
     * The player is set after the onResume() method call, and will be set
     * to null on the android onPause() event call.
     */
    protected PlayerAdapter player;
    
    @Override
    public void onResume() {
        super.onResume();
        Log.debug("[RD] onResume() called" );
        
        try {
            PlayerProvider playerprovider = (PlayerProvider) getActivity();
            player = playerprovider.getPlayer();

        } catch(ClassCastException e) {
            Log.bug("-- RemucoDialog gots an unsupported activity type, expected a PlayerProvider.");
            throw new RuntimeException();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        player =  null;
    }
    
}
