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

import java.lang.ref.WeakReference;
import java.util.Locale;

import remuco.client.android.fragments.ListsholderFragment;
import remuco.client.android.fragments.PlayerFragment;
import remuco.client.android.views.LockableViewpager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.Menu;
import android.view.MenuInflater;
import remuco.client.common.util.Log;


public class Remuco extends RemucoActivity {
    

    SectionsPagerAdapter mSectionsPagerAdapter;
    LockableViewpager mViewPager;

    private RemucoConnectionHandler connectionHandler = new RemucoConnectionHandler(this);
    
    // -----------------------------------------------------------------------------
    // --- lifecycle methods
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Android 4 style, compatible with android 2
        // Shows a horizontal pager to select a view. Works nicer on Android 4.
        setContentView(R.layout.main_viewpager);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the app.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (LockableViewpager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setPageMargin(1);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH){
            //FIXME: Bug in Android, the following code does not work on < ICS
            //       All activities will get the separator background color.
            //       See https://plus.google.com/108761828584265913206/posts/2na2KrBqNm5
            //         -> A workaround is also suggested here.
            mViewPager.setPageMarginDrawable(android.R.drawable.screen_background_dark_transparent);
        }
        
    }
    
    
    @Override
    public void onResume() {
        super.onResume();
        
        player.addHandler(connectionHandler);
        if(player.getPlayer() == null || player.getPlayer().getConnection().isClosed()) {
            handleDisconnected();
        } else {
            handleConnected();
        }
    }
    
    @Override 
    public void onPause() {
        super.onPause();
        player.removeHandler(connectionHandler);
    }

    // --- Options Menu
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.options_menu, menu);
        
        return true;
    }
        

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     * 
     * Holds the following sections:
     *   - Player
     *   - Playlist
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Log.debug("--- "+this+" .getItem()");
            
            if(position == 0) {
                return new PlayerFragment();
            } else if(position == 1) {
                return new ListsholderFragment();
            }
            return null; //TODO
            
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
            case 0:
                return "Player".toUpperCase(l);
            case 1:
                return "Library".toUpperCase(l);
            }
            return null;
        }
    }

    
    //is called when the player connection has come up.
    // Enables scrolling to the library pages.
    private void handleConnected() {
        mViewPager.setPagingEnabled(true);
    }
    
    //is called when the player connection was dropped.
    // Disables scrolling to the library pages.
    private void handleDisconnected() {
        mViewPager.setPagingEnabled(false);
        mViewPager.setCurrentItem(0, true);
    }
    
    
    
    //Approach from: http://stackoverflow.com/a/11336822
    static class RemucoConnectionHandler extends Handler {
        WeakReference<Remuco> remuco;
        
        public RemucoConnectionHandler(Remuco callback) {
            remuco = new WeakReference<Remuco>(callback);
        }
        
        @Override
        public void handleMessage(Message msg) {
            Remuco callback = remuco.get();
            if(callback ==  null) {
                return;
            }
            
            switch(msg.what){
            
            case MessageFlag.CONNECTED:
                callback.handleConnected();
                break;
                
            case MessageFlag.DISCONNECTED:
                callback.handleDisconnected();

            }
        }
    }

    
}
