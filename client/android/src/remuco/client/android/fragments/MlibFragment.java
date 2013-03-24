package remuco.client.android.fragments;

import remuco.client.android.RemucoLibraryList;
import remuco.client.android.util.LibraryItem;
import remuco.client.common.data.ActionParam;
import remuco.client.common.data.ItemList;
import remuco.client.common.util.Log;
import android.content.Context;

public class MlibFragment extends BaseFragmentRemucoLists {

    private ItemList loadedList;
    private String[] path = null;
    
    @Override
    protected RemucoLibraryList getLibrary(Context context) {
        return new MyRemucoLibraryList(context);
    }
    
    
    @Override
    protected void itemClicked(LibraryItem i) {
        super.itemClicked(i);
        if(!i.nested) {
            return;
        } else if (i.position == -1) {
            path = loadedList.getPathForParent();
        } else {
            path = loadedList.getPathForNested(i.position);
        }
        getList();
    }
    
    
    private class MyRemucoLibraryList extends RemucoLibraryList {

        public MyRemucoLibraryList(Context context) {
            super(context);
        }

        @Override
        public void sendAction(ActionParam action) {
            player.getPlayer().actionMediaLib(action);
        }

        @Override
        public void loadList(int page) {
            Log.debug("Path: "+path);
            player.getPlayer().reqMLib(reqHandler, path, page);
        }
        
        @Override 
        public void setList(ItemList l) {            
            super.setList(l);
            
            if(!l.isRoot()) {
                LibraryItem item = new LibraryItem();
                item.nested = true;
                item.position = -1;
                item.label = "..";
                insert(item, 0);
            }
            
            loadedList = l;
            path = l.getPath();
        }
        
    }

}
