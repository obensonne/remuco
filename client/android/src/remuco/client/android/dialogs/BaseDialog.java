package remuco.client.android.dialogs;

import remuco.client.android.PlayerAdapter;
import remuco.client.android.PlayerProvider;
import remuco.client.common.util.Log;
import android.support.v4.app.DialogFragment;

/**
 * Custom DialogFragment that ensures that a player variable is accessable
 * after the onResume() command.
 * 
 * The creating context (activity) of this dialog should implement the
 * PlayerProvider interface.
 */
public abstract class BaseDialog extends DialogFragment {

    /**
     * PlayerAdapter which can be used to interact with the player.
     * The player is set after the onResume() method call, and will be set
     * to null on the android onPause() event call.
     */
    protected PlayerAdapter player;
    
    @Override
    public void onResume() {
        super.onResume();
        Log.debug("[RD] onResume() called" );
        
        try {
            PlayerProvider playerprovider = (PlayerProvider) getActivity();
            player = playerprovider.getPlayer();

        } catch(ClassCastException e) {
            Log.bug("-- RemucoDialog gots an unsupported activity type, expected a PlayerProvider.");
            throw new RuntimeException();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        player =  null;
    }
    
}
