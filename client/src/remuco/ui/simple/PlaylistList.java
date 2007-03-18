package remuco.ui.simple;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.List;

import remuco.data.PlayerState;
import remuco.data.Song;

public class PlaylistList extends List {

    private long currentPlID;

    public PlaylistList() {
        super("Playlist", Choice.IMPLICIT);
    }

    public synchronized void setCurrentSong(int nr) {
        if (nr >= 0 && nr < this.size()) {
            this.setSelectedIndex(nr, true);
        }
    }

    public synchronized void setPlaylist(PlayerState ps) {
        synchronized (ps) {
            // only render pl if changed
            if (ps.playlistGetID() != currentPlID) {
                Song s;
                while (this.size() > 0) {
                    this.delete(0);
                }
                currentPlID = ps.playlistGetID();
                int plLen = ps.playlistGetLength();
                if (plLen == 0) {
                    this.setTitle("Empty");
                } else {
                    for (short i = 0; i < plLen; i++) {
                        s = ps.playlistGetSong(i);
                        this.append(s.getTag(Song.TAG_TITLE) + " ("
                                + s.getTag(Song.TAG_ARTIST) + ")", null);
                    }
                }
            }
        }
    }
}
