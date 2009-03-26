/*   
 *   Remuco - A remote control system for media players.
 *   Copyright (C) 2006-2009 Oben Sonne <obensonne@googlemail.com>
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
package remuco.player;

public abstract class AbstractAction {

	public final String desc;

	public final int id;

	public final String label;

	private String disabledReason;

	private boolean enabled = true;

	public AbstractAction(int id, String label, String desc) {

		this.id = id;
		this.label = label;
		this.desc = desc;
	}

	public void disbale(String reason) {
		enabled = false;
		disabledReason = reason;
	}

	public void enable() {
		enabled = true;
	}

	public String getDisabledReason() {
		return disabledReason;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public abstract boolean isItemAction();

	public abstract boolean isListAction();

}
