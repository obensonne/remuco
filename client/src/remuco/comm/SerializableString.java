package remuco.comm;


public final class SerializableString implements ISerializable {

	private final SerialAtom[] atoms;

	private String s;

	public SerializableString() {
		atoms = new SerialAtom[1];
		atoms[0] = new SerialAtom(SerialAtom.TYPE_S);
	}

	public void atomsHasBeenUpdated() {
		s = atoms[0].s;
	}

	public SerialAtom[] getAtoms() {
		return atoms;
	}

	public String get() {
		return s;
	}

	public void set(String s) {
		this.s = s;
	}

	public void updateAtoms() {
		atoms[0].s = s;
	}

}
