/*
 * Copyright (C) 2006 Christian Buennig - See COPYING
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 * 
 */
package remuco.data;

import java.util.Hashtable;
import java.util.Vector;

public class Tags {

	private String[] list;

	private boolean listChanged;

	private Vector order;

	private Hashtable tags;

	public Tags() {
		tags = new Hashtable();
		order = new Vector();
		list = new String[0];
		listChanged = true;
	}

	public synchronized void add(String name, String val) {
		if (!tags.containsKey(name)) {
			order.addElement(name);
			listChanged = true;
		}
		tags.put(name, val);
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null)
			return false;
		if (!(o instanceof Tags))
			return false;
		Tags t = (Tags) o;
		int n = order.size();
		if (n != t.order.size()) {
			return false;
		}
		for (int i = 0; i < n; i++) {
			if (!order.elementAt(i).equals(t.order.elementAt(i))) {
				return false;
			}
			if (!((String) tags.get(order.elementAt(i))).equals((String) t.tags
					.get(order.elementAt(i)))) {
				return false;
			}

		}
		return true;
	}

	/**
	 * Read a tag.
	 * @param name
	 *            the tag to get the value from
	 * @return the value of the tag named <code>name</code> or the empty
	 *         string if the tag is not set
	 */
	public String get(String name) {
		Object o = tags.get(name);
		if (o == null) {
			return "";
		} else {
			return (String) o;
		}
	}

	// private int hashCode;
	//
	// public int hashCode() {
	// if (listChanged) {
	// StringBuffer sb = new StringBuffer();
	// int n = order.size();
	// sb.append(n).append("x");
	// for (int i = 0; i < n; i++) {
	// sb.append(order.elementAt(i)).append(i);
	// sb.append((String) tags.get(order.elementAt(i))).append(i);
	// }
	// hashCode = sb.toString().hashCode();
	// }
	// return hashCode;
	// }

	/**
	 * Get a list of all tag names.
	 * 
	 * @return string array with names of all tags
	 */
	public String[] list() {
		if (listChanged) {
			int n = order.size();
			list = new String[n];

			for (int j = 0; j < n; j++) {
				list[j] = (String) order.elementAt(j);
			}
			listChanged = false;
		}
		return list;

	}

}
