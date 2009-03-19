package remuco.comm;

public interface ISerializable {

	/** Get the atoms. */
	public SerialAtom[] getAtoms();

	/**
	 * Signals that the atoms has been updated.
	 * 
	 * @throws BinaryDataExecption
	 *             if the data in the atoms is semantically malformed
	 */
	public void notifyAtomsUpdated() throws BinaryDataExecption;

}
