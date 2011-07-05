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

import remuco.client.common.data.State;
import remuco.client.common.util.Log;
import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class RemucoPhoneListener extends PhoneStateListener {

    private Context context;
    private boolean pausedPlayer = false;

	// --- the player adapter
	protected PlayerAdapter player = null;

    public RemucoPhoneListener(Context context) {
        this.context = context;
    }

    public void onCallStateChanged(int phoneState, String incomingNumber) {
        if (player == null) {
            player = RemucoActivity.connect(context, 0);
        }

        int playerState = player.getPlayer().state.getPlayback();

        switch(phoneState) {
        case TelephonyManager.CALL_STATE_IDLE:
            Log.debug("Call Finish");
            if (playerState == State.PLAYBACK_PAUSE
                || playerState == State.PLAYBACK_STOP
                || pausedPlayer == true){
                player.getPlayer().ctrlPlayPause();
                pausedPlayer = false;
            }
            break;
        case TelephonyManager.CALL_STATE_OFFHOOK:
            Log.debug("Call Starting");
            if (playerState == State.PLAYBACK_PLAY){
                player.getPlayer().ctrlPlayPause();
                pausedPlayer = true;
            }
            break;
        case TelephonyManager.CALL_STATE_RINGING:
            Log.debug("Call Ringing");
            if (playerState == State.PLAYBACK_PLAY){
                player.getPlayer().ctrlPlayPause();
                pausedPlayer = true;
            }
            break;
        }
    }

}
