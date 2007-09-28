package remuco.ui.screens;

import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import remuco.player.IInfoListener;
import remuco.player.Info;
import remuco.player.Plob;

/**
 * A Form to edit the editable meta information of a {@link Plob}.
 * 
 * @author Christian Buennig
 * 
 */
public final class PlobEditorScreen extends Form implements IInfoListener {

	private static final String[] META_EDITABLE = new String[] {
			Plob.META_ARTIST, Plob.META_TITLE, Plob.META_ALBUM,
			Plob.META_GENRE, Plob.META_YEAR, Plob.META_TRACK,
			Plob.META_COMMENT, Plob.META_TAGS };

	private final Info pinfo;

	/** The current plob to edit */
	private Plob plob;

	private final TextField[] textFields = new TextField[META_EDITABLE.length];

	public PlobEditorScreen(Info pinfo) {

		super("Meta-Info Editor");

		this.pinfo = pinfo;
		
		infoChanged();

	}

	/**
	 * Applies the changes made by the user to the plob previously set with
	 * {@link #setPlob(Plob)} and returns this plob.
	 * 
	 * @return the edited plob
	 */
	public Plob getPlobEdited() {

		for (int i = 0; i < META_EDITABLE.length; i++) {

			if (META_EDITABLE[i].equals(Plob.META_TAGS)) {
				plob.setTags(textFields[i].getString());
			} else {
				plob.setMeta(META_EDITABLE[i], textFields[i].getString());
			}
		}

		return plob;
	}

	public void infoChanged() {

		for (int i = 0; i < textFields.length; i++) {

			textFields[i] = new TextField(META_EDITABLE[i], "", 256,
					TextField.ANY);

			if (META_EDITABLE[i].equals(Plob.META_YEAR))
				textFields[i].setConstraints(TextField.NUMERIC);

			if (META_EDITABLE[i].equals(Plob.META_TAGS)
					&& !pinfo.hasFeature(Info.FEATURE_PLOB_TAGS))
				continue;

			append(textFields[i]);
		}

	}

	/**
	 * Set the plob to be edit with this plob editor screen.
	 * 
	 * @param plob
	 */
	public void setPlob(Plob plob) {

		this.plob = plob;

		for (int i = 0; i < META_EDITABLE.length; i++) {

			textFields[i].setString(plob.getMeta(META_EDITABLE[i]));

		}

	}

}
