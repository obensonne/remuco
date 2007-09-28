package remuco;

import remuco.comm.DeviceFinder;
import remuco.controller.CommController;
import remuco.controller.ICCEventListener;
import remuco.player.ICurrentPlobListener;
import remuco.player.IPlaylistListener;
import remuco.player.Player;
import remuco.player.Plob;
import remuco.player.PlobList;
import remuco.util.Log;

public class HostClient implements ICCEventListener, ICurrentPlobListener,
		IPlaylistListener {

	public void event(int type, String msg) {

		System.out.println(msg);

	}

	public void go() {

		DeviceFinder df = new DeviceFinder();
		
		try {
			df.startSearch();
		} catch (UserException e1) {
			e1.printStackTrace();
			return;
		}
		
		Log.ln("started device search");
		
		String[] devices;
		
		devices = df.getRemoteDevices();
		
		for (int i = 0; i < devices.length; i+=2) {
			String addr = devices[i];
			String name = devices[i];
			
			Log.ln("Device: " + name + "(" + addr + ")");
			
		}
		
		if (true) return;
		
		CommController c = new CommController(this);

		Player p = c.getPlayer();
		
		p.registerCurrentPlobListener(this);
		p.registerPlaylistListener(this);
		
		//c.connect("0016CFF19765");
		c.connect("001060D01AF4");
		
		try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {

		new HostClient().go();

	}

	public void currentPlobChanged(Plob cp) {
		
		System.out.println("NEW CURRENT PLOB");
		System.out.println(cp.toString());
		
	}

	public void playlistChanged(PlobList playlist) {
		
		System.out.println("NEW PLAYLIST");
		System.out.println(playlist.toString());
		
		
	}

}
