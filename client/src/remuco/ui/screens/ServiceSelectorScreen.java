package remuco.ui.screens;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.microedition.lcdui.List;

public class ServiceSelectorScreen extends List {

	private final Vector urls;

	public ServiceSelectorScreen() {

		super("Media Players", List.IMPLICIT);

		urls = new Vector();

	}

	/**
	 * Get the selected service.
	 * 
	 * @return the service URL
	 */
	public String getSelectedService() {

		final int index = getSelectedIndex();

		return (String) urls.elementAt(index);

	}

	public void setServices(Hashtable services) {

		deleteAll();

		urls.removeAllElements();

		final Enumeration e = services.keys();

		while (e.hasMoreElements()) {
			final String name = (String) e.nextElement();
			append(name, null);
			urls.addElement(services.get(name));
		}

	}

}
