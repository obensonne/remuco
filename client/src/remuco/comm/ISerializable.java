package remuco.comm;

public interface ISerializable {

	/**
	 * Get the atoms. To read the atoms, call {@link #updateAtoms()} before. To
	 * alter the atoms, call {@link #atomsHasBeenUpdated()} after altering the
	 * atoms.
	 * <p>
	 * <em>Note:</em> Implementing classes should only touch the atoms within
	 * {@link #updateAtoms()} and {@link #atomsHasBeenUpdated()} !
	 */
	public SerialAtom[] getAtoms();

	/** Requests to update the atom vector <code>atoms</code>. */
	public void updateAtoms();

	/**
	 * Signals that the atom vector <code>atoms</code> has been updated.
	 * 
	 * @throws BinaryDataExecption
	 *             if the data in the atoms is semantically malformed
	 */
	public void atomsHasBeenUpdated() throws BinaryDataExecption;

}
