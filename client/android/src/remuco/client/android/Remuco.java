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

import java.util.Locale;


import remuco.client.android.dialogs.RatingDialog;
import remuco.client.android.fragments.PlayerFragment;
import remuco.client.android.fragments.PlaylistFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import remuco.client.common.util.Log;


public class Remuco extends RemucoActivity {
    
    
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
     * will keep every loaded fragment in memory. If this becomes too memory
     * intensive, it may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    
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
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        
        
        /*
        //Old style, set only one fragment 
        setContentView(R.layout.main);
        if (savedInstanceState == null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            //Fragment fragment = new PlayerFragment();
            Fragment fragment = new PlaylistFragment();
            
            fragmentTransaction.add(R.id.fragment_container, fragment);
            fragmentTransaction.commit();
        }
        */
    
    }
    

    // --- Options Menu
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.options_menu, menu);
        
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (super.onOptionsItemSelected(item)) return true;
        
        switch(item.getItemId()){
        
        case R.id.options_menu_library:
            final Intent intent = new Intent(this, RemucoLibraryTab.class);
            startActivity(intent);
            return true;
            
        case R.id.options_menu_rate:
            showRateDialog();
            return true;
        }
        
        return false;
    }
    
    // ------------------------
    // --- dialogs FIXME: Move this code to player
    // ------------------------
    
    private void showRateDialog() {
        FragmentManager fm = getSupportFragmentManager();
        RatingDialog ratingdialog = RatingDialog.newInstance(player);
        ratingdialog.show(fm, "ratingdialog");
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
                return new PlaylistFragment();
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
                return "Playlist".toUpperCase(l);
            case 2:
                return "Collection".toUpperCase(l);
            case 3:
                return "Browse".toUpperCase(l);
            }
            return null;
        }
    }

    
}
