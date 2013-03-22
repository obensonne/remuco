package remuco.client.android.fragments;

import remuco.client.android.R;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.support.v4.app.FragmentTabHost;


/**
 * Fragment that holds several player lists.
 * These lists can be accessed by pressing the corresponding tab.
 */
public class ListsholderFragment extends BaseFragment {
    private FragmentTabHost mTabHost;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);
        
        Resources res = getResources(); // Resource object to get Drawables

        
        mTabHost = new FragmentTabHost(getActivity());
        mTabHost.setup(getActivity(), getChildFragmentManager(), R.layout.tabsfragment);

        mTabHost.addTab(
                mTabHost.newTabSpec("playlist").setIndicator("", res.getDrawable(R.drawable.ic_tab_playlists)),
                PlaylistFragment.class, null);

        mTabHost.addTab(
                mTabHost.newTabSpec("queue").setIndicator("", res.getDrawable(R.drawable.ic_tab_albums)),
                QueueFragment.class, null);
        
        mTabHost.addTab(
                mTabHost.newTabSpec("mlib").setIndicator("", res.getDrawable(R.drawable.ic_tab_songs)),
                MlibFragment.class, null);
        
        mTabHost.addTab(
                mTabHost.newTabSpec("files").setIndicator("", res.getDrawable(R.drawable.ic_tab_artists)),
                HostFilesFragment.class, null);

        return mTabHost;
        
    }
}
