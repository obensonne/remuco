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
 * Player proxy template.
 * 
 * This is a template for developing a player proxy (PP) in C.
 * The documentation in that files is a guideline to implement a specific PP.
 * But see also the functions API documentation in rem-pp.h. You may further
 * have a look in other PPs, e.g. rem-pp-xmms.c or rem-pp-xmms2.c to get an
 * impression how to develop a PP.
 */

///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include <stdlib.h>
#include <string.h>

#include "rem-pp.h"
#include "rem-pp-util.h"	// optional
#include "rem-tags.h"		// optional
#include "rem-log.h"

///////////////////////////////////////////////////////////////////////////////
//
// macros and constants
//
///////////////////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////////////////////////
//
// global vars
//
///////////////////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////////////////////////
//
// functions - pp interface
//
// API documentation to these function can be found in rem-pp.h
//
///////////////////////////////////////////////////////////////////////////////

int rem_pp_init()
{
	LOG_NOISE("called\n");
	
	// This functions gets called first. Do some warm up work, e.g.
	// initially connect to the music player to control.

	return 0;
}

int
rem_pp_get_ps(struct rem_pp_ps *ps)
{	
	LOG_NOISE("called\n");
	
	// This function gets called by the Remuco server to receive the
	// current music player state. This includes playback state, volume
	// etc. as well as the playlist. The playlist is a list of ids of the
	// songs currently in the player's playlist.
	// Below is an example implementation (not realistic, because it
	// allways returns the same values).

	unsigned int		i;

	// set current music player state
	ps->state = REM_PS_STATE_OFF;
	ps->volume = 55;
	ps->pl_repeat = 0;
	ps->pl_shuffle = 0;
	ps->pl_len = 3;
	ps->pl_sid_type = REM_PP_SID_TYPE_STRING;
	
	// Create the list of song ids
	ps->pl_sid_list = malloc(ps->pl_len * sizeof(union rem_pp_sid));
	if (!ps->pl_sid_list) {
		LOG_ERROR("malloc failed\n");
		return -1;
	}
	for (i = 0; i < ps->pl_len; ++i) {
		char	sid[1024];
		sprintf(sid, "ID%i", i);
		ps->pl_sid_list[i].str = strdup(sid);
	}
	
	return 0;
}


void
rem_pp_free_ps(struct rem_pp_ps *ps)
{
	LOG_NOISE("called\n");

	// If the Remuco server has processed a player state returned by
	// rem_pp_get_ps() and does not need the player state anymore,
	// it calls this function, so that you may do some cleanup work.
	// Below is an example implementation which frees the memory occupied
	// by the strings in the player state's song id list (if the song ids
	// are of type string - otherwise nothing has to be done).

	unsigned int u;

	if (ps->pl_sid_type == REM_PP_SID_TYPE_UINT)
		return;	// nothing to do
	
	// Song ids are strings. Free the mem they use (!! the next lines may
	// need to be changed for a specifc PP implemenation !!).
	for (u = 0; u < ps->pl_len; u++) {
 		free(ps->pl_sid_list[u].str);
	}
	free(ps->pl_sid_list);
	ps->pl_sid_list = NULL;
}

int
rem_pp_get_song(const union rem_pp_sid *sid, struct rem_pp_song *song)
{
	LOG_NOISE("called\n");

	// This function gets called by the Remuco server after it has called
	// rem_pp_get_ps() and recognized a change in the playlist. The server
	// iterates the song ids contained in the player state returned by
	// rem_pp_get_ps() and calls this function for every song id in the
	// player state's song id list.
	// You have to fill the structure 'song' with meta data of the song
	// with the song id 'sid'.
	// Below is an example (not realistic, because it returns the same meta
	// data for every song - except the uid).

	int ret;
	
	ret = rem_song_append_tag(song, REM_TAG_NAME_UID, sid->str);
	if (ret < 0) {
		return -1;
	}
	ret = rem_song_append_tag(song, REM_TAG_NAME_ARTIST, "Sade");
	if (ret < 0) {
		return -1;
	}
	ret = rem_song_append_tag(song, REM_TAG_NAME_TITLE, "Smooth Operator");
	if (ret < 0) {
		return -1;
	}
	ret = rem_song_append_tag(song, REM_TAG_NAME_RATING, "4/5");
	if (ret < 0) {
		return -1;
	}
	return 0;
}

int
rem_pp_process_cmd(struct rem_pp_pc *pc)
{
	LOG_NOISE("called\n");
	
	// If the Remuco server receives a player control command from a
	// client (mobile device), it calls this function to forward it
	// to the music player.

	LOG_INFO("process command %hu with param %hu\n", pc->cmd, pc->param);

	return 0;
}

void rem_pp_dispose()
{
	LOG_NOISE("called\n");
	
	// This function gets called if the Remuco server shuts down.
	// Do some clean up work if needed.
	
}

///////////////////////////////////////////////////////////////////////////////
//
// functions - internal
//
///////////////////////////////////////////////////////////////////////////////

