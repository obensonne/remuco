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

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.TabHost;

public class RemucoLibraryTab extends TabActivity
    implements TabHost.TabContentFactory, TabHost.OnTabChangeListener{

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.library_tab);

        Resources res = getResources(); // Resource object to get Drawables
        TabHost tabHost = getTabHost();  // The activity TabHost
        TabHost.TabSpec spec;  // Resusable TabSpec for each tab
        Intent intent;  // Reusable Intent for each tab

        // Tab Playlist
        intent = new Intent().setClass(this, RemucoLibraryPlaylist.class);
        spec = tabHost.newTabSpec("playlist").setIndicator("",
                      res.getDrawable(R.drawable.ic_tab_playlists))
            .setContent(intent);
        tabHost.addTab(spec);

        // Tab Queue
        intent = new Intent().setClass(this, RemucoLibraryQueue.class);
        spec = tabHost.newTabSpec("queue").setIndicator("",
                      res.getDrawable(R.drawable.ic_tab_albums))
            .setContent(intent);
        tabHost.addTab(spec);

        // Tab MLib
        intent = new Intent().setClass(this, RemucoLibraryMlib.class);
        spec = tabHost.newTabSpec("mlib").setIndicator("",
                      res.getDrawable(R.drawable.ic_tab_songs))
            .setContent(intent);
        tabHost.addTab(spec);

        // Tab Files
        intent = new Intent().setClass(this, RemucoLibraryFiles.class);
        spec = tabHost.newTabSpec("files").setIndicator("",
                      res.getDrawable(R.drawable.ic_tab_artists))
            .setContent(intent);
        tabHost.addTab(spec);

        // Tab Playing now
        intent = new Intent().setClass(this, LaunchRemuco.class);
        spec = tabHost.newTabSpec("playingnow").setIndicator("",
                      res.getDrawable(R.drawable.ic_tab_playback))
            .setContent(this);
        tabHost.addTab(spec);
        tabHost.setOnTabChangedListener(this);

        tabHost.setCurrentTab(0);
    }

    public View createTabContent(String tag) {
        return new View(this.getApplicationContext());
    }

    public void onTabChanged(String tabId) {
        if ("playingnow".equals(tabId)) {
            this.finish();
        }
    }
}