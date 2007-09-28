package remuco.player;

/**
 * For classes, which listen for {@link Library} (a list of all ploblists).
 * 
 * @author Christian Buennig
 * 
 */
public interface ILibraryRequestor {

	public void handleLibrary(Library lib);

}
