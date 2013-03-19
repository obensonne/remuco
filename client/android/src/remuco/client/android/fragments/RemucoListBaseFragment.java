package remuco.client.android.fragments;

import remuco.client.android.R;
import remuco.client.android.RemucoLibraryList;
import remuco.client.android.RequestHandlerCallback;
import remuco.client.android.RequesterAdapter;
import remuco.client.common.data.ItemList;
import remuco.client.common.util.Log;
import android.content.Context;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;

/**
 * Base fragment for lists.
 */
public abstract class RemucoListBaseFragment extends BaseFragment implements RequestHandlerCallback {
    
    RemucoLibraryList mylib;
    ListView lv;
    protected RequesterAdapter reqHandler;
    
    protected abstract RemucoLibraryList getLibrary(Context context);
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        
        super.onCreateView(inflater, container, savedInstanceState);
        Log.debug("--- " + this.getClass().getName() + ".onCreateView()");
        
        mylib = getLibrary(getActivity().getApplicationContext());
        View view = inflater.inflate(R.layout.playlist, container, false);
        lv = (ListView) view.findViewById(R.id.library_items);
        lv.setTextFilterEnabled(true);
        lv.setAdapter(mylib.getAdapter());
        registerForContextMenu(lv);
        
        reqHandler = new RequesterAdapter(this);
        return view;
    }
    
    
    @Override
    public void onResume() {
        super.onResume();
        Log.debug("--- " + this.getClass().getName() + ".onResume()");
        player.addHandler(reqHandler);
        getList();
    }
    
    
    @Override
    public void onPause() {
        super.onPause();
        Log.debug("--- " + this.getClass().getName() + ".onPause()");
        player.removeHandler(reqHandler);
    }
    
    
    //
    //---  Context menu methods
    //     In this base class there are no special elements to manage,
    //     let superclass and mylib do the work
     
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        mylib.onCreateContextMenu(menu, v, menuInfo);
    }
    
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if(super.onContextItemSelected(item)) {
            return true;
        }
        return mylib.onContextItemSelected(item);
    }
    
    
    /**
     * List methods
     */
    public void getList() {
        if(player.getPlayer() == null) return;
        mylib.getList();
        
    }
    
    public void clearList() {
        mylib.clearList();
    }
    
    @Override
    public void setList(ItemList l) {
        mylib.setList(l);
    }

}
