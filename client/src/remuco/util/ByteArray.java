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
 * Static methods to read and write (especially integers) from a byte array
 * while taking care of byte ordering.
 * 
 * @author Christian Buennig
 *
 */
public class ByteArray {

    public static byte[] concat(byte[] b1, byte[] b2, int len2) {
        int len1 = b1 == null ? 0 : b1.length;
        int i;
        byte[] ret = new byte[len1 + len2];
        for (i = 0; i < len1; i++)
            ret[i] = b1[i];
        for (i = 0; i < len2; i++)
            ret[len1 + i] = b1[i];
        return ret;
    }

    // public static short getByteUnsigned(byte[] ba, int offset) {
    // throw new IllegalStateException("not yet implemented");
    // }

    public static void copy(byte[] src, int offsetSrc, byte[] dst,
            int offsetDst, int len) {
        for (int i = 0; i < len; i++) {
            dst[offsetDst + i] = src[offsetSrc + i];
        }
    }

    // public static long getIntUnsigned(byte[] ba, int offset, byte bo) {
    // throw new IllegalStateException("not yet implemented");
    // }

    public static boolean equal(byte[] ba1, byte[] ba2) {
        if (ba1 == ba2) {
            return true;
        }
        if (ba1 == null || ba2 == null) {
            return false;
        }
        if (ba1.length != ba2.length) {
            return false;
        }
        for (int i = 0; i < ba2.length; i++) {
            if (ba1[i] != ba2[i]) {
                return false;
            }

        }
        return true;
    }

    /**
     * Reads 4 bytes from <code>ba</code>, starting at <code>offset</code>
     * and interprets them as an int value, according the byte order
     * <code>bo</code>.
     * 
     * @param ba
     *            the byte array to read the value from
     * @param offset
     *            where to start reading in the array
     * @param bo
     *            the byte order
     * @return the int value
     */
    public static int readInt(byte[] ba, int offset, byte bo) {
        switch (bo) {
            case ByteArray.BO_BE:
                return (((ba[offset] & 0xff) << 24)
                        | ((ba[offset + 1] & 0xff) << 16)
                        | ((ba[offset + 2] & 0xff) << 8) | (ba[offset + 3] & 0xff));
            case ByteArray.BO_LE:
                return (((ba[offset + 3] & 0xff) << 24)
                        | ((ba[offset + 2] & 0xff) << 16)
                        | ((ba[offset + 1] & 0xff) << 8) | (ba[offset] & 0xff));
            default:
                throw new IllegalArgumentException("bad byte order type");
        }
    }

    // public static int getShortUnsigned(byte[] ba, int offset, byte

    // bo) {
    // throw new IllegalStateException("not yet implemented");
    // }

    // /**
    // * Convert an array of bytes to an integer.
    // *
    // * @param ba
    // * the byte array
    // * @param offset
    // * from where to read from the array
    // * @param len
    // * how many bytes to use for conversion (valid: 1,2,4)
    // * @param bo
    // * the byte order
    // * @return the integer value
    // * @throws IllegalArgumentException
    // * if len is not valid
    // */
    // private static int byteArrayGetInt(byte[] ba, int offset, int len, byte
    // bo) {
    // if (len != 1 && len != 2 && len != 4 || (ba.length < offset + len))
    // throw new IllegalArgumentException("bad len value");
    // int ret = 0, j = 0;
    // len = (len > 4 || len < 0) ? 4 : len;
    // byte b;
    // switch (bo) {
    // case Remuco.BO_BE:
    // for (int i = 0; i < len; i++) {
    // b = ba[offset + i];
    // j = b < 0 ? (((byte) b ^ 0xFF) * -1) - 1 : b;
    // ret += j << ((len - 1 - i) * 8);
    // }
    // break;
    // case Remuco.BO_LE:
    // for (int i = 0; i < len; i++) {
    // b = ba[offset + i];
    // j = b < 0 ? (((byte) b ^ 0xFF) * -1) - 1 : b;
    // ret += j << (i * 8);
    // }
    // break;
    // }
    // return ret;
    // }

    /**
     * Reads 2 bytes from <code>ba</code>, starting at <code>offset</code>
     * and interprets them as a short value, according the byte order
     * <code>bo</code>.
     * 
     * @param ba
     *            the byte array to read the value from
     * @param offset
     *            where to start reading in the array
     * @param bo
     *            the byte order
     * @return the short value
     */
    public static short readShort(byte[] ba, int offset, byte bo) {
        switch (bo) {
            case ByteArray.BO_BE:
                return (short) (((ba[offset] & 0xff) << 8) | (ba[offset + 1] & 0xff));
            case ByteArray.BO_LE:
                return (short) (((ba[offset + 1] & 0xff) << 8) | (ba[offset] & 0xff));
            default:
                throw new IllegalArgumentException("bad byte order type");
        }
    }

    /**
     * Intepretates <code>b</code> as an unsigned byte and returns the value
     * as an integer.
     * 
     * @param b
     * @return b unsigened
     */
    public static int readByteUnsigned(byte b) {
        return b < 0 ? (((byte) b ^ 0xFF) * -1) - 1 : b;
    }

    /**
     * 
     * @param ba
     * @param from
     *            from index (inclusive)
     * @param len
     *            length of sub char array
     * @return sub char array or null, if ca is null or from and to have
     *         icompatible values
     */
    public static byte[] sub(byte[] ba, int from, int len) {
        if (ba == null || from < 0 || from + len > ba.length) {
            return null;
        }
        byte[] ret = new byte[len];
        for (int i = 0; i < len; i++) {
            ret[i] = ba[from + i];
        }
        return ret;
    }

    public static void writeInt(byte[] ba, int offset, byte bo, int val) {
        switch (bo) {
            case ByteArray.BO_BE:
                ba[offset] = (byte) ((val >> 24) & 0xff);
                ba[offset + 1] = (byte) ((val >> 16) & 0xff);
                ba[offset + 2] = (byte) ((val >> 8) & 0xff);
                ba[offset + 3] = (byte) (val & 0xff);
                break;
            case ByteArray.BO_LE:
                ba[offset + 3] = (byte) ((val >> 24) & 0xff);
                ba[offset + 2] = (byte) ((val >> 16) & 0xff);
                ba[offset + 1] = (byte) ((val >> 8) & 0xff);
                ba[offset] = (byte) (val & 0xff);
                break;
            default:
                throw new IllegalArgumentException("bad byte order");
        }

    }

    public static void writeShort(byte[] ba, int offset, byte bo, short val) {
        switch (bo) {
            case ByteArray.BO_BE:
                ba[offset] = (byte) ((val >> 8) & 0xff);
                ba[offset + 1] = (byte) (val & 0xff);
                break;
            case ByteArray.BO_LE:
                ba[offset + 1] = (byte) ((val >> 8) & 0xff);
                ba[offset] = (byte) (val & 0xff);
                break;
            default:
                throw new IllegalArgumentException("bad byte order");
        }

    }
    
    public static void set(byte[] ba, int offset, byte val, int len) {
        for (int i = offset; i < offset + len; i++) {
            ba[i] = val;
        }
    }

    public static final byte BO_BE = 0;
    public static final byte BO_LE = 1;
    public static final byte BO_NET = BO_BE;
    public static final byte BO_DEFAULT = BO_BE;

}
