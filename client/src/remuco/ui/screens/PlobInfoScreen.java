package remuco.ui.screens;

import javax.microedition.lcdui.Form;

import remuco.player.Plob;

public final class PlobInfoScreen extends Form {

	private static final String[] TAGS = new String[] { Plob.META_ARTIST,
			Plob.META_TITLE, Plob.META_ALBUM, Plob.META_GENRE, Plob.META_YEAR,
			Plob.META_COMMENT, Plob.META_BITRATE };

	public PlobInfoScreen() {

		super("Info");

	}

	public void setPlob(Plob plob) {

		final String lb = "\n";
		StringBuffer sb = new StringBuffer();

		deleteAll();

		if (plob == null)
			return;

		if (plob.hasAbstract()) { // basic plob info

			sb.append(plob.getMeta(Plob.META_ABSTRACT));

		} else { // full plob info

			for (int i = 0; i < TAGS.length; i++) {
				sb.append(lb);
				sb.append(translateTagName(TAGS[i])).append(": ");
				sb.append(plob.getMeta(TAGS[i]));
			}

			sb.append(lb);
			sb.append("Rating: ").append(plob.getRating());
			sb.append(lb);
			sb.append("Length: ").append(plob.getLenFormatted());
		}

		this.append(sb.toString());

	}

	private String translateTagName(String tagName) {
		return tagName;
	}

}
