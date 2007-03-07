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
 * This interfaces clones the client side relevant constants from
 * <code>rem.h</code> (can be found in the server source). For description and
 * usage of these constants as well as detailed information about the Remuco 
 * data-transfer protocol have a look into that file.
 * 
 * @author Christian Buennig
 * 
 */
public interface Remuco {

    // ////////////////////////////////////////////////////////////////////////
    //
    // player state
    //
    // ////////////////////////////////////////////////////////////////////////

    public static final int REM_PS_TD_LEN = 12;

    public static final byte REM_PS_STATE_STOP = 0;

    public static final byte REM_PS_STATE_PLAY = 1;

    public static final byte REM_PS_STATE_PAUSE = 2;

    public static final byte REM_PS_STATE_PROBLEM = 3;

    public static final byte REM_PS_STATE_OFF = 4;

    public static final byte REM_PS_STATE_ERROR = 10;

    public static final byte REM_PS_STATE_SRVOFF = 20;

    public static final byte REM_PS_FLAG_PL_REPEAT = 0x02;

    public static final byte REM_PS_FLAG_PL_SHUFFLE = 0x04;

    // ////////////////////////////////////////////////////////////////////////
    //
    // player control
    //
    // ////////////////////////////////////////////////////////////////////////

    public static final int REM_PC_TD_LEN = 4;

    public static final byte REM_PC_CMD_VOLUME = 1;

    public static final byte REM_PC_CMD_PLAY_PAUSE = 3;

    public static final byte REM_PC_CMD_NEXT = 4;

    public static final byte REM_PC_CMD_PREV = 5;

    public static final byte REM_PC_CMD_STOP = 6;

    public static final byte REM_PC_CMD_RESTART = 7;

    public static final byte REM_PC_CMD_JUMP = 8;

    public static final byte REM_PC_CMD_RATE = 9;

    public static final byte REM_PC_CMD_LOGOFF = 10;

    public static final byte REM_PC_CMD_NOOP = 99;

    // ////////////////////////////////////////////////////////////////////////
    //
    // misc
    //
    // ////////////////////////////////////////////////////////////////////////

    public static final int REM_TD_HDR_LEN = 6;

    public static final byte REM_PROTO_VERSION = 0x02;

    public static final byte REM_DATA_TYPE_PLAYER_CTRL = 0x01;

    public static final byte REM_DATA_TYPE_PLAYER_STATE = 0x02;

    public static final byte REM_DATA_TYPE_CLIENT_INFO = 0x03;

    public static final byte REM_DATA_TYPE_NULL = 0x10;

    public static final byte REM_DATA_TYPE_UNKNOWN = (byte) 0xFF;

    // ////////////////////////////////////////////////////////////////////////
    //
    // song (tags)
    //
    // ////////////////////////////////////////////////////////////////////////

    public static final String REM_TAG_NAME_UID = "UID";

    public static final String REM_TAG_NAME_TITLE = "Title";

    public static final String REM_TAG_NAME_ARTIST = "Artist";

    public static final String REM_TAG_NAME_ALBUM = "Album";

    public static final String REM_TAG_NAME_GENRE = "Genre";

    public static final String REM_TAG_NAME_YEAR = "Year";

    public static final String REM_TAG_NAME_RATING = "Rating";

    public static final String REM_TAG_NAME_COMMENT = "Comment";

    public static final String REM_TAG_NAME_BITRATE = "Bitrate";

    public static final String REM_TAG_NAME_LENGTH = "Length";

    public static final String REM_TAG_NAME_TRACK = "Track";

    public static final String REM_TAG_VAL_RATING_UNRATED = "-100";

    // ////////////////////////////////////////////////////////////////////////
    //
    // client info
    //
    // ////////////////////////////////////////////////////////////////////////

    public static final int REM_CI_ENCSTR_LEN = 256;

    public static final int REM_CI_TD_LEN = (REM_CI_ENCSTR_LEN + 2);

}
