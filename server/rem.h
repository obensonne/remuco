/*
 * Copyright (C) 2006 Christian Buennig - See COPYING
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

/**
 * This header file specifies Remuco constants and some methods to work on
 * Remuco data.
 */

#ifndef REM_H_
#define REM_H_

//////////////////////////////////////////////////////////////////////////////
//
// includes
//
//////////////////////////////////////////////////////////////////////////////

#include <sys/types.h> // htons()

//////////////////////////////////////////////////////////////////////////////
//
// player state: constants for state and flags
//
//////////////////////////////////////////////////////////////////////////////

#define REM_PS_STATE_STOP		0
#define REM_PS_STATE_PLAY		1
#define REM_PS_STATE_PAUSE		2
#define REM_PS_STATE_PROBLEM		3
#define REM_PS_STATE_OFF		4
#define REM_PS_STATE_ERROR		10
#define REM_PS_STATE_SRVOFF		20

#define REM_PS_FLAG_PL_REPEAT		0x02
#define REM_PS_FLAG_PL_SHUFFLE		0x04

#define REM_PS_FLAG_SET(_f, _v)	_	f |= (u_int8_t) (_v)
#define REM_PS_FLAG_UNSET(_f, _v)	_f &= (u_int8_t) ((_v) ^ 0xFF)
#define REM_PS_FLAG_ISSET(_f, _v)	((_f) & (_v))

//#define REM_PS_PL_POS_NONE		((u_int16_t) -1)
#define REM_PS_PL_POS_NONE		0

//////////////////////////////////////////////////////////////////////////////
//
// player state: constants for songs
//
//////////////////////////////////////////////////////////////////////////////

/** standard tags */
#define REM_TAG_NAME_UID	"UID"
#define REM_TAG_NAME_TITLE	"Title"
#define REM_TAG_NAME_ARTIST	"Artist"
#define REM_TAG_NAME_ALBUM	"Album"
#define REM_TAG_NAME_GENRE	"Genre"
#define REM_TAG_NAME_YEAR	"Year"
#define REM_TAG_NAME_RATING	"Rating"	// format: 'n/m'
						// n: rating (-1: unrated)
						// m: max possible rating
#define REM_TAG_NAME_COMMENT	"Comment"
#define REM_TAG_NAME_BITRATE	"Bitrate"
#define REM_TAG_NAME_LENGTH	"Length"	// in ms
#define REM_TAG_NAME_TRACK	"Track"

#define REM_TAG_VAL_RATING_UNRATED	"-100"

#define REM_MAX_TAGS		20

//////////////////////////////////////////////////////////////////////////////
//
// player control: command codes
//
//////////////////////////////////////////////////////////////////////////////

#define REM_PC_CMD_VOLUME	 1
#define REM_PC_CMD_PLAY_PAUSE	 3
#define REM_PC_CMD_NEXT		 4
#define REM_PC_CMD_PREV		 5
#define REM_PC_CMD_STOP		 6
#define REM_PC_CMD_RESTART	 7
#define REM_PC_CMD_JUMP		 8
#define REM_PC_CMD_RATE		 9
#define REM_PC_CMD_LOGOFF	10
#define REM_PC_CMD_NOOP		99

//////////////////////////////////////////////////////////////////////////////
//
// misc constants
//
//////////////////////////////////////////////////////////////////////////////

#define REM_PP_ENC_DEF		"UTF8"

//////////////////////////////////////////////////////////////////////////////
//
// structs
//
//////////////////////////////////////////////////////////////////////////////

//////////////////////////////////////////////////////////////////////////////
//
// functions
//
//////////////////////////////////////////////////////////////////////////////

#endif /*REM_H_*/
