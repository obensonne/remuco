package remuco.client.android.fragments;

import remuco.client.android.RemucoLibraryList;
import remuco.client.common.data.ActionParam;
import android.content.Context;

public class PlaylistFragment extends RemucoListBaseFragment {

    @Override
    protected RemucoLibraryList getLibrary(Context context) {
        return new MyRemucoLibraryList(context);
    }

    private class MyRemucoLibraryList extends RemucoLibraryList {

        public MyRemucoLibraryList(Context context) {
            super(context);
        }

        @Override
        public void sendAction(ActionParam action) {
            player.getPlayer().actionPlaylist(action);
        }

        @Override
        public void getList() {
            player.getPlayer().reqPlaylist(reqHandler, 0);
        }
    }

}
