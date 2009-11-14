package remuco.client.android;

import java.util.TimerTask;

import remuco.client.common.MainLoop;
import remuco.client.common.data.Item;
import remuco.client.common.player.IItemListener;
import remuco.client.common.player.IProgressListener;
import remuco.client.common.player.IStateListener;
import remuco.client.common.player.Player;
import remuco.client.common.util.Log;

public class FooBar implements IItemListener, IStateListener, IProgressListener {

	/*
	 * In a real Android app FooBar could be a UI which has some buttons to
	 * control the player and some labels to show the player's state.
	 */

	private final Player player;

	public FooBar(Player player) {

		this.player = player;

		/* Tell the player that we are interested in changes. */
		player.setItemListener(this);
		player.setStateListener(this);
		player.setProgressListener(this);

		/*
		 * For demonstration purpose, set up a timer task which periodically
		 * toggles play / pause on the player. This demonstrates the use of
		 * player and of the MainLoop.
		 */
		MainLoop.schedule(new TimerTask() {
			@Override
			public void run() {
				FooBar.this.player.ctrlPlayPause();
			}
		}, 5000, 5000);
	}

	public Player getPlayer() {
		return player;
	}

	@Override
	public void notifyItemChanged() {
		// log the title of the new item/track
		Log.ln("item changed: " + player.item.getMeta(Item.META_TITLE));
	}

	@Override
	public void notifyProgressChanged() {
		Log.ln("state changed: " + player.progress.getProgress());
	}

	@Override
	public void notifyStateChanged() {
		Log.ln("state changed: " + player.state);

	}

}
