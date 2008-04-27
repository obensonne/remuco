package remuco.player;

import remuco.comm.BinaryDataExecption;
import remuco.comm.ISerializable;
import remuco.comm.SerialAtom;
import remuco.util.Log;

public final class PlayerList implements ISerializable {

	private final SerialAtom[] atoms;

	public PlayerList() {

		atoms = new SerialAtom[1];
		atoms[0] = new SerialAtom(SerialAtom.TYPE_AS);

		atoms[0].as = new String[0];
	}

	public void atomsHasBeenUpdated() throws BinaryDataExecption {
		// nothing to do
	}

	public boolean contains(String player) {

		String[] players;

		players = atoms[0].as;

		for (int i = 0; i < players.length; i++)
			if (player.equals(players[i]))
				return true;

		return false;
	}

	public SerialAtom[] getAtoms() {

		return atoms;
	}

	public String[] getNames() {
		return atoms[0].as;
	}

	public void updateAtoms() {
		// not needed
		Log.asssertNotReached(this);

	}

}
