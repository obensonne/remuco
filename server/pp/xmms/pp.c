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
///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include <stdlib.h>
#include <string.h>
#include <xmms/xmmsctrl.h>	// from xmms-devel

#include "../../rem-io.h"
#include "../../rem-tags.h"
#include "../../rem-log.h"
#include "../../rem-pp.h"
#include "../../rem-pp-util.h"

///////////////////////////////////////////////////////////////////////////////
//
// macros and constants
//
///////////////////////////////////////////////////////////////////////////////

#define MAX_XMMS_SESSIONS	16

#define REM_PP_XMMS_TAG_NAMES_COUNT	7

///////////////////////////////////////////////////////////////////////////////
//
// global vars
//
///////////////////////////////////////////////////////////////////////////////

static int			xs;
static const char		**tag_names;

///////////////////////////////////////////////////////////////////////////////
//
// functions - pp interface
//
// API documentation to these function can be found in rem-pp.h
//
///////////////////////////////////////////////////////////////////////////////

int rem_pp_init()
{
	LOG_DEBUG("called\n");
	
	// create a fixed list of tags we will later read from files
	tag_names = malloc(REM_PP_XMMS_TAG_NAMES_COUNT * sizeof(char*));
	if (tag_names == NULL) {
		LOG_ERRNO("malloc failed\n");
		return -1;
	}
	tag_names[0] = REM_TAG_NAME_ARTIST;
	tag_names[1] = REM_TAG_NAME_TITLE;
	tag_names[2] = REM_TAG_NAME_ALBUM;
	tag_names[3] = REM_TAG_NAME_GENRE;
	tag_names[4] = REM_TAG_NAME_COMMENT;
	tag_names[5] = REM_TAG_NAME_LENGTH;
	tag_names[6] = REM_TAG_NAME_YEAR;

	// get xmms session (if there is one)
	for( xs = 0 ; xs < MAX_XMMS_SESSIONS ; xs++ )
		if ( xmms_remote_is_running( xs ) )
			break;
	if (xs >= MAX_XMMS_SESSIONS) {
		xs = 0;
	}
	
	// init ok
	return 0;
}

int
rem_pp_get_ps(struct rem_pp_ps *ps)
{
	LOG_NOISE("called\n");

	unsigned int u, j;

	ps->pl_sid_type = REM_PP_SID_TYPE_STRING;

	///// state on / off /////

	LOG_NOISE("check xmms state (session %i)\n", xs);
	if (!xmms_remote_is_running(xs)) { // no session
		
		for( xs = 0 ; xs < MAX_XMMS_SESSIONS ; xs++ )
			if ( xmms_remote_is_running( xs ) )
				break;

		if (xs < MAX_XMMS_SESSIONS) {
			LOG_INFO("xmms is up\n");
		} else {
			LOG_NOISE("xmms is still off\n");
			ps->state = REM_PS_STATE_OFF;
			return 0;
		}
	} // .. from here, we have a running session
	
	///// state playback /////

	if (xmms_remote_is_playing(xs)) {
		if (xmms_remote_is_paused(xs)) {
			ps->state = REM_PS_STATE_PAUSE;
		} else {
			ps->state = REM_PS_STATE_PLAY;
		}
	} else {
		ps->state = REM_PS_STATE_STOP;
	}

	///// volume /////
	
	ps->volume = xmms_remote_get_main_volume(xs);
	ps->volume = ps->volume > 100 ? 50 : ps->volume;

	///// flags: repeat + shuffle /////
	
	ps->pl_repeat = xmms_remote_is_repeat(xs);
	ps->pl_shuffle = xmms_remote_is_shuffle(xs);

	///// playlist position and length /////

	ps->pl_pos = xmms_remote_get_playlist_pos(xs);
	ps->pl_len = xmms_remote_get_playlist_length(xs);

	if (ps->pl_len == 0) {
		return 0;
	}

	///// song id list /////
	
	ps->pl_sid_list = calloc(ps->pl_len, sizeof(char*));
	if (ps->pl_sid_list == NULL) {
		LOG_ERROR("malloc failed\n");
		return -1;
	}
 	for (u = 0; u < ps->pl_len; u++) {
		ps->pl_sid_list[u].str = xmms_remote_get_playlist_file(xs, u);
 		LOG_NOISE("song %i has sid %s\n", u, ps->pl_sid_list[u].str);
		if (ps->pl_sid_list[u].str == NULL) {
			// pl changed while processing, we stop playlist
			// observation for now and have try on next update
		 	for (j = 0; j < u; j++) {
		 		free(ps->pl_sid_list[u].str);
		 	}
		 	free(ps->pl_sid_list);
			return -1;
		}
	}

	return 0;
}

void
rem_pp_free_ps(struct rem_pp_ps *ps)
{
	unsigned int u;
	for (u = 0; u < ps->pl_len; u++) {
 		free(ps->pl_sid_list[u].str);
	}
	free(ps->pl_sid_list);
	ps->pl_sid_list = NULL;
}

int
rem_pp_get_song(const union rem_pp_sid *sid, struct rem_pp_song *song)
{
	int ret;
	
	ret = rem_song_append_tag(song, REM_TAG_NAME_UID, sid->str);
	if (ret < 0) {
		return -1;
	}
	ret = rem_tags_read(sid->str, tag_names, REM_PP_XMMS_TAG_NAMES_COUNT, song);
	if (ret < 0) {
		ret = rem_song_set_unknown(song);
		if (ret < 0) {
			return -1;
		}
	}
	return 0;
}

int
rem_pp_process_cmd(struct rem_pp_pc *pc)
{
	LOG_DEBUG("command: %hu, param: %hu\n", pc->cmd, pc->param);

	int pl_len;

	// search xmms session
	if (!xmms_remote_is_running(xs)) {
		LOG_WARN("xmms is off -> ignore command\n");
		return 0;
	}
			
	switch (pc->cmd) {
		case REM_PC_CMD_JUMP:
			pl_len = xmms_remote_get_playlist_length(xs);
			if (pc->param < pl_len) {
				xmms_remote_set_playlist_pos(xs, pc->param);
			} else {
				LOG_ERROR("jump position is out of range!\n");
			}
			break;
		case REM_PC_CMD_NEXT:
			xmms_remote_playlist_next(xs);
			break;
		case REM_PC_CMD_PREV:
			xmms_remote_playlist_prev(xs);
			break;
		case REM_PC_CMD_PLAY_PAUSE:
			xmms_remote_play_pause(xs);
			break;
		case REM_PC_CMD_STOP:
			xmms_remote_stop(xs);
			break;
		case REM_PC_CMD_RESTART:
			xmms_remote_stop(xs);
			xmms_remote_set_playlist_pos(xs, 0);
			xmms_remote_play(xs);
			break;
		case REM_PC_CMD_VOLUME:
			if (pc->param <= 100) {
				xmms_remote_set_main_volume(xs, pc->param);
			} else {
				LOG_ERROR("volume param is out of range!\n");
			}
			break;
		default:
			LOG_WARN("ignore command %hu\n", pc->cmd);
			break;
	}

	return 0;
}

void rem_pp_dispose()
{
	free(tag_names);
	LOG_NOISE("called\n");
}

///////////////////////////////////////////////////////////////////////////////
//
// functions - internal
//
///////////////////////////////////////////////////////////////////////////////

