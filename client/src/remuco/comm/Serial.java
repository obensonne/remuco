package remuco.comm;

import remuco.util.Log;

public final class Serial {
	
	public static final String ENCODING = "UTF-8";

	/**
	 * Updates the message's {@link ISerializable} ({@link Message#serializable})
	 * from the messages's binary data ({@link Message#bytes}).
	 * 
	 * @param m
	 *            the message
	 * @throws BinaryDataExecption
	 *             if the message's binary data is somehow malformed (e.g.
	 *             incompatible to the message's {@link ISerializable} atoms)
	 */
	public static void in(ISerializable s, byte[] data)
			throws BinaryDataExecption {

		SerialAtom[] atoms;
		BaIn bis;
		int l;

		atoms = s.getAtoms();

		bis = new BaIn(data);

		l = atoms.length;

		for (int i = 0; i < l; i++) {

			switch (atoms[i].type) {
			case SerialAtom.TYPE_Y:
				atoms[i].y = bis.readY();
				break;
			case SerialAtom.TYPE_B:
				atoms[i].b = bis.readB();
				break;
			case SerialAtom.TYPE_I:
				atoms[i].i = bis.readI();
				break;
			case SerialAtom.TYPE_S:
				atoms[i].s = bis.readS();
				break;
			case SerialAtom.TYPE_AY:
				atoms[i].ay = bis.readAY();
				break;
			case SerialAtom.TYPE_AS:
				atoms[i].as = bis.readAS();
				break;
			default:
				Log.bug("Feb 22, 2009.6:25:57 PM");
				break;
			}
		}

		s.atomsHasBeenUpdated();

		if (bis.available() > 0) {
			throw new BinaryDataExecption(bis.available() + "unused bytes left");
		}

	}

	/**
	 * Converts the message's {@link ISerializable} ({@link Message#serializable})
	 * into binary data ({@link Message#bytes}).
	 * 
	 * @param m
	 *            the message
	 */
	public static byte[] out(ISerializable s) {

		SerialAtom[] atoms;
		BaOut bos;
		int l;

		bos = new BaOut(256); // should be enough for most outgoing messages

		s.updateAtoms();

		atoms = s.getAtoms();

		bos.reset();

		l = atoms.length;

		for (int i = 0; i < l; i++) {

			switch (atoms[i].type) {
			case SerialAtom.TYPE_Y:
				bos.writeY(atoms[i].y);
				break;
			case SerialAtom.TYPE_B:
				bos.writeB(atoms[i].b);
				break;
			case SerialAtom.TYPE_I:
				bos.writeI(atoms[i].i);
				break;
			case SerialAtom.TYPE_S:
				bos.writeS(atoms[i].s);
				break;
			case SerialAtom.TYPE_AY:
				bos.writeAY(atoms[i].ay);
				break;
			case SerialAtom.TYPE_AS:
				bos.writeAS(atoms[i].as);
				break;
			default:
				Log.bug("Feb 22, 2009.6:26:09 PM");
				break;
			}
		}

		return bos.toByteArray();

	}

}
