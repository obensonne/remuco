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
