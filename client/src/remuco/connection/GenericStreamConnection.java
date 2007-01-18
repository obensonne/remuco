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
package remuco.connection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import remuco.util.Log;

/**
 * A stream based connection made up of a DataInput- and DataOuptuStream. This
 * class main purpose is to abstract from j2me specific stream connections.
 * 
 * @author Christian Buennig
 */
public class GenericStreamConnection {

    private DataInputStream in;

    private DataOutputStream out;

    public GenericStreamConnection() {
        setStreams(null, null);
    }

    public GenericStreamConnection(DataInputStream in, DataOutputStream out) {
        setStreams(in, out);
    }

    public void setStreams(DataInputStream in, DataOutputStream out) {
        this.in = in;
        this.out = out;
    }

    public void close() {
        try {
            in.close();
            out.close();
        } catch (IOException e) {
            Log.ln(this, "closing failed");
        } catch (NullPointerException e) {
            // allready closed
        }
        in = null;
        out = null;
    }

    public DataInputStream getIn() {
        return in;
    }

    public DataOutputStream getOut() {
        return out;
    }

    public boolean isOpen() {
        return in != null && out != null;
    }

}
