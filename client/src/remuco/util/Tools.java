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
package remuco.util;

/**
 * Misc Tools (expacially for J2ME classes).
 * 
 * @author Christian Buennig
 * 
 */
public abstract class Tools {

    /**
     * This is a very bad random number generator, but it is enough if just a
     * <em>taste</em> of random is needed.
     * 
     * @param upper
     * @return a number <code>y</code> with <code>0 &lt;= y &lt; upper</code>
     */
    public static long random(long upper) {
        return (long) ((double) (System.currentTimeMillis() % 2141)
                / (double) 2141 * upper);
    }

    /**
     * Sleep a while. {@link InterruptedException} gets catched.
     * 
     * @param ms
     */
    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sleep some random time. {@link InterruptedException} gets catched.
     * 
     * @param ms
     *            maximum time to sleep
     * @see #random(long)
     * @see #sleep(long)
     */
    public static void sleepRandom(long ms) {
        sleep(random(ms));
    }

    public static String[] splitString(String s, String splitter) {
        String ret[];
        int first, last, spl, sal;

        spl = splitter.length();

        first = s.indexOf(splitter);
        sal = 1;
        while (first >= 0) {
            sal++;
            first = s.indexOf(splitter, first + spl);
        }
        ret = new String[sal];

        first = 0;
        last = s.indexOf(splitter);
        for (int i = 0; i < sal; i++) {
            if ((last = s.indexOf(splitter, first)) < 0) {
                last = s.length();
            }
            ret[i] = s.substring(first, last);
            first = last + spl;
        }
        return ret;
    }

}
