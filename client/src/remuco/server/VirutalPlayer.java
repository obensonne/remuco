/*
 * Copyright (C) 2006 Christian Buennig - See COPYING
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 */
package remuco.server;

import remuco.data.ObservablePlayerState;
import remuco.data.PlayerControl;
import remuco.data.PlayerState;
import remuco.data.Song;
import remuco.data.Tags;
import remuco.util.Log;
import remuco.util.Tools;

public class VirutalPlayer {

    private ObservablePlayerState ops;

    public VirutalPlayer() {

        Tags t;

        ops = new ObservablePlayerState();
        ops.setState(PlayerState.ST_STOPPED);

        for (int i = 0; i < 10; i++) {
            t = new Tags();
            t.add(Song.TAG_UID, Integer.toString(i));
            t.add(Song.TAG_TITLE, "S" + i + " XäX");
            t.add(Song.TAG_ARTIST, "A" + i + " XäX");
            t.add(Song.TAG_ALBUM, "B" + i);
            ops.playlistAddSong(new Song(t));
        }

    }

    public synchronized void control(PlayerControl pc) {
        Log.ln(this, "Handle player control " + pc.toString());
        short plLen = ops.playlistGetLength();
        switch (pc.getCmd()) {
            case PlayerControl.CODE_JUMP:
                ops.playlistSetPosition((short) pc.getParam());
                ops.setState(PlayerState.ST_PLAYING);
                break;
            case PlayerControl.CODE_NEXT:
                if (plLen > 1) {
                    if (ops.playlistGetPosition() == plLen - 1) {
                        ops.playlistSetPosition((short) 0);
                    } else {
                        ops.playlistSetPosition((short) (ops
                                .playlistGetPosition() + 1));
                    }
                }
                if (ops.getState() == PlayerState.ST_PAUSED) {
                    ops.setState(PlayerState.ST_PLAYING);
                }
                break;
            case PlayerControl.CODE_PLAY_PAUSE:
                if (ops.getState() == PlayerState.ST_PLAYING) {
                    ops.setState(PlayerState.ST_PAUSED);
                } else if (ops.playlistGetLength() > 0) {
                    ops.setState(PlayerState.ST_PLAYING);
                }
                break;
            case PlayerControl.CODE_PREV:
                if (plLen > 1) {
                    if (ops.playlistGetPosition() == 0) {
                        ops.playlistSetPosition((short) (plLen - 1));
                    } else {
                        ops.playlistSetPosition((short) (ops
                                .playlistGetPosition() - 1));
                    }
                }
                if (ops.getState() == PlayerState.ST_PAUSED) {
                    ops.setState(PlayerState.ST_PLAYING);
                }
                break;
            case PlayerControl.CODE_RESTART:
                ops.playlistSetPosition( (short) 0);
                ops.setState(PlayerState.ST_PLAYING);
                break;
            case PlayerControl.CODE_STOP:
                ops.setState(PlayerState.ST_STOPPED);
                break;
            case PlayerControl.CODE_RATE:
                break;
            default:
                break;
        }
        // simulate time play configuration and change needs
        Tools.sleepRandom(1000);
        ops.changed();
    }

    public ObservablePlayerState getObservablePlayerState() {
        return ops;
    }

    public String getPlayerName() {
        return "Dummy";
    }

}
