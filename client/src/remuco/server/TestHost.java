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
package remuco.server;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Form;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;



public class TestHost extends MIDlet {

    public TestHost() {
        super();
    }

    Thread server;

    protected void startApp() throws MIDletStateChangeException {
        Server s;
        s = new Server();
        try {
            s.init();
        } catch (Exception e) {
            e.printStackTrace();
            notifyDestroyed();
            return;
        }
        s.setPlayer(new VirutalPlayer());
        server = new Thread(s);
        server.start();
        Display.getDisplay(this).setCurrent(new Form("Test Host"));
    }

    protected void pauseApp() {

    }

    protected void destroyApp(boolean arg0) throws MIDletStateChangeException {
        server.interrupt();
    }

}
