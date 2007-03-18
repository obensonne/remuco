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
package remuco.proto;


/**
 * Exception to throw if conversion between an data object and its byte
 * representation fails (e.g. if byte array data is malformed).
 * 
 * @author Christian Buennig
 * 
 */
public class TransferDataException extends Exception {

	/**
	 * Indicates a mjor problem. Furhter communication most probably not
	 * posible.
	 */
	public static final int MAJOR = 10;

	/**
	 * Indicates a minor problem. Subsequent communication may work.
	 */
	public static final int MINOR = 20;

	/**
	 * Indicates a problem because of insufficient free memory.
	 */
	public static final int NOMEM = 30;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int type;

	/**
	 * Exception with no message and of type {@link #MINOR}.
	 *
	 */
	public TransferDataException() {
		super();
		type = MINOR;
	}

	/**
	 * Exception with no message and type <code>type</code>.
	 *
	 */
	public TransferDataException(int type) {
		super();
		this.type = type;
	}

	/**
	 * Exception with a message and of type {@link #MINOR}.
	 *
	 */
	public TransferDataException(String message) {
		super(message);
		type = MINOR;
	}

	/**
	 * Exception with a message and type <code>type</code>.
	 *
	 */
	public TransferDataException(String message, int type) {
		super(message);
		this.type = type;
	}

	/**
	 * Get the exception type.
	 * @return one of {@link #MINOR}, {@link #MAJOR} or {@link #NOMEM}
	 */
	public int getType() {
		return type;
	}

}
