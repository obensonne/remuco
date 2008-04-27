package remuco.player;

/** A descriptive interface for the player. */
public interface IPlayerInfo {

	/** Get maximum rating or 0 if rating is not supported. */
	public int getMaxRating();

	/**
	 * Get the name of the player.
	 * 
	 * @return the name (never <code>null</code> - returns 'Remuco' when there
	 *         is no name)
	 */
	public String getName();

	public boolean isPlaybackKnown();

	public boolean isVolumeKnown();

}