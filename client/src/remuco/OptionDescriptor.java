/*
 * Remuco - A remote control system for media players. Copyright (C) 2006-2009
 * Oben Sonne <obensonne@googlemail.com>
 * 
 * This file is part of Remuco.
 * 
 * Remuco is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * Remuco is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * Remuco. If not, see <http://www.gnu.org/licenses/>.
 */
package remuco;

import remuco.util.Tools;

/**
 * Description of an option (store ID, label for options screen, default value
 * and value constraints).
 */
public class OptionDescriptor {

	/** Option type. */
	public static final int TYPE_CHOICE = 1, TYPE_INT = 2, TYPE_STRING = 3;

	/** Possible option values for options of type {@link #TYPE_CHOICE}. */
	public String choices[];

	/** Default value. */
	public final String def;

	/** Option ID. */
	public final String id;

	/** Option label. */
	public final String label;

	/** Minimum and maximum value for options of type {@link #TYPE_INT}. */
	public final int min, max;

	/** Option type. */
	public final int type;

	public OptionDescriptor(String id, String label, int def, int min, int max) {
		this(TYPE_INT, id, label, String.valueOf(def), min, max, null);
	}

	public OptionDescriptor(String id, String label, String def) {
		this(TYPE_STRING, id, label, def, 0, 0, null);
	}

	public OptionDescriptor(String id, String label, String def, String choices) {
		this(TYPE_CHOICE, id, label, def, 0, 0, Tools.splitString(choices, ',',
			false));
	}

	public OptionDescriptor(String id, String label, String def,
			String choices[]) {
		this(TYPE_CHOICE, id, label, def, 0, 0, choices);
	}

	private OptionDescriptor(int type, String id, String label, String def,
			int min, int max, String choices[]) {
		
		this.type = type;
		this.id = id;
		this.label = label;
		this.def = def;
		this.min = min;
		this.max = max;
		this.choices = choices;
		
		// validate
		
		if (type != TYPE_CHOICE && type != TYPE_INT && type != TYPE_STRING) {
			throw new IllegalArgumentException();
		}
		if (type == TYPE_INT && min > max) {
			throw new IllegalArgumentException();
		}
		if (type == TYPE_CHOICE && choices == null) {
			throw new IllegalArgumentException();
		}
		if (type == TYPE_CHOICE && Tools.getIndex(choices, def) < 0) {
			throw new IllegalArgumentException();
		}
		if (type != TYPE_CHOICE && choices != null) {
			throw new IllegalArgumentException();
		}
	}

}
