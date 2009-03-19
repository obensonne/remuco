package remuco.player;

public interface IRequester {

	public void handleFiles(ItemList files);

	public void handleItem(Item item);

	public void handleLibrary(ItemList library);

	public void handlePlaylist(ItemList playlist);

	public void handleQueue(ItemList queue);

}
