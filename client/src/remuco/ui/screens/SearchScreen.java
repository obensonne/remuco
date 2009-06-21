package remuco.ui.screens;

import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

public class SearchScreen extends Form {

	private final TextField fields[];
	
	private final String mask[];

	public SearchScreen(String mask[]) {
		super("Search");
		fields = new TextField[mask.length];
		for (int i = 0; i < mask.length; i++) {
			fields[i] = new TextField(mask[i], "", 256, TextField.ANY);
			append(fields[i]);
		}
		this.mask = mask;
	}

	public String[] getMask() {
		return mask;
	}

	public String[] getQuery() {

		final String query[] = new String[fields.length];
		for (int i = 0; i < query.length; i++) {
			query[i] = fields[i].getString().trim();
		}
		return query;

	}

}
