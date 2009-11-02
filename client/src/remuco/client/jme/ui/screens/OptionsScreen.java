/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2009 by the Remuco team, see AUTHORS.
 *
 *   This file is part of Remuco.
 *
 *   Remuco is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Remuco is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Remuco.  If not, see <http://www.gnu.org/licenses/>.
 *   
 */
package remuco.client.jme.ui.screens;

import java.util.Hashtable;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.ItemStateListener;
import javax.microedition.lcdui.TextField;

import remuco.Config;
import remuco.OptionDescriptor;
import remuco.client.common.util.Log;
import remuco.client.common.util.Tools;

/**
 * Screen to adjust all options given by {@link Config#OPTION_DESCRIPTORS}.
 */
public class OptionsScreen extends Form {

	/** Interface for classes listening for changes of options. */
	public interface IOptionListener {

		/**
		 * Check if the option listener is a session listener, i.e. it is only
		 * alive as long there is a player connection session.
		 * 
		 * @return <code>true</code> if this is a session only listener,
		 *         <code>false</code> if this is a listener which is alive the
		 *         whole application live time
		 */
		public boolean isSessionOptionListener();

		/**
		 * Notify the change of an option.
		 * 
		 * @param od
		 *            the changed option's descriptor
		 */
		public void optionChanged(OptionDescriptor od);

	}

	/**
	 * Implement item state listener in this class because it is already
	 * implemented privately by {@link Form}.
	 */
	private class OptionWidgetChangeListener implements ItemStateListener {

		public void itemStateChanged(Item ow) {

			optionWidgetChanged(ow);
		}

	}

	private final Config config;

	/** Mapping of option widgets to option descriptors. */
	private final Hashtable ow2od;

	public OptionsScreen() {

		super("Options");

		ow2od = new Hashtable();

		config = Config.getInstance();

		for (int i = 0; i < Config.OPTION_DESCRIPTORS.size(); i++) {

			final OptionDescriptor od;
			final Item ow;

			od = (OptionDescriptor) Config.OPTION_DESCRIPTORS.elementAt(i);
			ow = optionDescriptorToWidget(od);

			append(ow);

			ow2od.put(ow, od);
		}

		setItemStateListener(new OptionWidgetChangeListener());
	}

	private Item optionDescriptorToWidget(OptionDescriptor od) {

		final String val;

		switch (od.type) {

		case OptionDescriptor.TYPE_CHOICE:
			final ChoiceGroup cg = new ChoiceGroup(od.label, Choice.POPUP,
					od.choices, null);
			val = config.getOption(od);
			int index = Tools.getIndex(od.choices, val);
			cg.setSelectedIndex(index < 0 ? 0 : index, true);
			return cg;

		case OptionDescriptor.TYPE_INT:
			val = config.getOption(od);
			return new TextField(od.label, val, 10, TextField.NUMERIC);

		case OptionDescriptor.TYPE_STRING:
			val = config.getOption(od);
			return new TextField(od.label, val, 20, TextField.NON_PREDICTIVE);

		default:
			throw new RuntimeException("bug");
		}
	}

	private void optionWidgetChanged(Item ow) {

		final OptionDescriptor od = (OptionDescriptor) ow2od.get(ow);

		if (ow instanceof ChoiceGroup) {

			final int index = ((ChoiceGroup) ow).getSelectedIndex();
			config.setOption(od, od.choices[index]);

		} else if (ow instanceof TextField) {

			String val = ((TextField) ow).getString();
			if (od.type == OptionDescriptor.TYPE_INT) {
				try {
					int i = Integer.parseInt(val);
					if (i < od.min) {
						val = String.valueOf(od.min);
						((TextField) ow).setString(val);
					} else if (i > od.max) {
						val = String.valueOf(od.max);
						((TextField) ow).setString(val);
					}
				} catch (NumberFormatException e) {
					val = od.def;
					((TextField) ow).setString(val);
				}
			}
			config.setOption(od, val);

		} else {
			Log.bug("Jul 30, 2009.8:26:44 PM");
		}
	}

}
