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
#include <xmmsclient/xmmsclient.h>

#include "rem-pp.h"
#include "rem-pp-util.h"
#include "rem-util.h"
#include "rem-log.h"

///////////////////////////////////////////////////////////////////////////////
//
// macros and constants
//
///////////////////////////////////////////////////////////////////////////////

#define PP_NAME			"xmms2"
#define PP_VERSION		"0.4.0"

///////////////////////////////////////////////////////////////////////////////
//
// global vars
//
///////////////////////////////////////////////////////////////////////////////

static xmmsc_connection_t	*connection;

///////////////////////////////////////////////////////////////////////////////
//
// function prototypes
//
///////////////////////////////////////////////////////////////////////////////

static void
rem_pp_xmms2_disconnect(void *arg);

static void
rem_pp_xmms2_setup_connection(void);

static void
rem_pp_xmms2_add_tag_to_song(struct rem_pp_song *s, xmmsc_result_t *result,
	int res_type, const char *tag_name_mlib, const char *tag_name_rem);

///////////////////////////////////////////////////////////////////////////////
//
// functions public (pp interface)
//
// API documentation to these function can be found in rem-pp.h
//
///////////////////////////////////////////////////////////////////////////////

int rem_pp_init()
{
	LOG_NOISE("called\n");
	
	rem_pp_xmms2_setup_connection();
	
	return 0;
}

int
rem_pp_get_ps(struct rem_pp_ps *ps)
{
	int		ret;
	unsigned int	u, sid, sid_cur;
	u_int32_t	n, m;
	xmmsc_result_t	*result;

	ps->pl_sid_type = REM_PP_SID_TYPE_UINT;

	///// xmms2d connection / state /////
	
	LOG_NOISE("check xmms2d connection\n");
	if (!connection) { 
		rem_pp_xmms2_setup_connection();
		if (!connection) {
			LOG_NOISE("xmms2d is still off\n");
			ps->state = REM_PS_STATE_OFF;
			return 0;
		}
	}
	
	///// state /////
	
	result = xmmsc_playback_status(connection);
	xmmsc_result_wait (result);
	if (xmmsc_result_iserror (result)) {
		LOG_WARN("command failed: %s\n", xmmsc_result_get_error(result));
		xmmsc_result_unref(result);
		rem_pp_xmms2_disconnect("");
		return -1;
	}
	
	ret = xmmsc_result_get_uint (result, &u);
	xmmsc_result_unref(result);
	if (!ret) {
		LOG_ERROR("state: result broken");
		return -1;
	}
	
	switch (u) {
		case XMMS_PLAYBACK_STATUS_PAUSE:
			ps->state = REM_PS_STATE_PAUSE;
			break;
		case XMMS_PLAYBACK_STATUS_PLAY:
			ps->state = REM_PS_STATE_PLAY;
			break;
		case XMMS_PLAYBACK_STATUS_STOP:
			ps->state = REM_PS_STATE_STOP;
			break;
		default:
			LOG_WARN("unknown xmms2d playback status\n");
			ps->state = REM_PS_STATE_PROBLEM;
			break;
	}
	
	///// volume /////
	
	result = xmmsc_playback_volume_get(connection);
	xmmsc_result_wait (result);
	if (xmmsc_result_iserror (result)) {
		LOG_WARN("command failed: %s\n", xmmsc_result_get_error(result));
		xmmsc_result_unref(result);
		rem_pp_xmms2_disconnect("");
		return -1;
	}
	
	n = 50; m = 50;
	ret =	xmmsc_result_get_dict_entry_uint32(result, "left", &n) &&
		xmmsc_result_get_dict_entry_uint32(result, "right", &m);
	xmmsc_result_unref(result);
	if (!ret) {
		LOG_WARN("volume: result broken\n");
		return -1;
	}
	
	ps->volume = n < m ? m : n;
	
	///// pl modes /////

	ps->pl_repeat = 0;
	ps->pl_shuffle = 0;

	///// sid_list, pl_len and pl_pos /////

	// get sid of current track
	LOG_NOISE("get sid of current track\n");
	result = xmmsc_playback_current_id(connection);
	xmmsc_result_wait (result);
	if (xmmsc_result_iserror (result)) {
		LOG_WARN("command failed: %s\n", xmmsc_result_get_error(result));
		return -1;
	}
	ret = xmmsc_result_get_uint(result, &sid_cur);
	xmmsc_result_unref(result);
	if (!ret) {
		LOG_WARN("cid: result broken");
		return -1;
	}
	
	// get playlist
	LOG_NOISE("get playlist\n");
	result = xmmsc_playlist_list (connection);
	xmmsc_result_wait (result);
	if (xmmsc_result_iserror (result)) {
		LOG_WARN("command failed: %s\n", xmmsc_result_get_error(result));
		return -1;
	}

	// get length of playlist
	LOG_NOISE("get length of playlist\n");
	for (u = 0, xmmsc_result_list_first(result);
				xmmsc_result_list_valid (result);
				xmmsc_result_list_next(result), u++);
	ps->pl_len = u;
	
	if (ps->pl_len == 0) {
		return 0;
	}

	// create sid_list and get pl_pos
	LOG_NOISE("create uid_list and get pl_pos\n");
	ps->pl_sid_list = malloc(ps->pl_len * sizeof(union rem_pp_sid));
	if (!ps->pl_sid_list) {
		LOG_ERROR("malloc failed\n");
		return -1;
	}
	for (u = 0, xmmsc_result_list_first(result);
					xmmsc_result_list_valid (result);
					xmmsc_result_list_next(result), u++) {
		ret = xmmsc_result_get_uint (result, &sid);
		if (!ret) {
			LOG_WARN("pl: result broken\n");
			xmmsc_result_unref(result);
			free(ps->pl_sid_list);
			return -1;
		}
		ps->pl_sid_list[u].uint = sid;
		// detect pl_pos
		if (sid == sid_cur) {
			ps->pl_pos = u;
		}		
	}
	xmmsc_result_unref(result);
		
	return 0;	
}

void
rem_pp_free_ps(struct rem_pp_ps *ps)
{
	free(ps->pl_sid_list);
	ps->pl_sid_list = NULL;
}

int
rem_pp_get_song(const union rem_pp_sid *sid, struct rem_pp_song *song)
{

	int32_t i;
	char str[REM_MAX_STR_LEN];
	const int xrt_str = XMMSC_RESULT_VALUE_TYPE_STRING;
	const int xrt_int = XMMSC_RESULT_VALUE_TYPE_INT32;
	xmmsc_result_t *result;

	// request media data from mlib

	LOG_NOISE("read song %u from mlib\n", sid->uint);
	
	result = xmmsc_medialib_get_info (connection, sid->uint);

	xmmsc_result_wait (result);

	if (xmmsc_result_iserror (result)) {
		LOG_ERROR("medialib get info returns error, %s\n",
				         xmmsc_result_get_error (result));
		return -1;
	}

	// process retreived information and add as remuco tags to the song
	
	rem_pp_xmms2_add_tag_to_song(song, result, xrt_str, "artist",
							REM_TAG_NAME_ARTIST);
	rem_pp_xmms2_add_tag_to_song(song, result, xrt_str, "title",
							REM_TAG_NAME_TITLE);
	rem_pp_xmms2_add_tag_to_song(song, result, xrt_str, "album",
							REM_TAG_NAME_ALBUM);
	rem_pp_xmms2_add_tag_to_song(song, result, xrt_str, "genre",
							REM_TAG_NAME_GENRE);
	rem_pp_xmms2_add_tag_to_song(song, result, xrt_str, "comment",
							REM_TAG_NAME_COMMENT);
	rem_pp_xmms2_add_tag_to_song(song, result, xrt_int, "tracknr",
							REM_TAG_NAME_TRACK);
	rem_pp_xmms2_add_tag_to_song(song, result, xrt_int, "duration",
							REM_TAG_NAME_LENGTH);
	rem_pp_xmms2_add_tag_to_song(song, result, xrt_int, "bitrate",
							REM_TAG_NAME_BITRATE);
	if (!xmmsc_result_get_dict_entry_int32(result, "rating", &i))
		i = 0;
	snprintf(str, REM_MAX_STR_LEN, "%i/5", i);
	rem_song_append_tag(song, REM_TAG_NAME_RATING, str);
	
	xmmsc_result_unref (result);
	
	return 0;
}

int rem_pp_process_cmd(struct rem_pp_pc *pc)
{
	
#define REM_PP_XMMS2_WAT_RESULT do {	\
	xmmsc_result_wait (result);	\
	if (xmmsc_result_iserror (result)) {	\
		LOG_ERROR("%s\n", xmmsc_result_get_error(result));	\
		xmmsc_result_unref(result);	\
		rem_pp_xmms2_disconnect("");	\
		break;	\
	}		\
} while(0)

	LOG_DEBUG("command: %hu, param: %hu\n", pc->cmd, pc->param);

	int ret;
	unsigned int u;
	xmmsc_result_t *result;

	// search xmms session
	if (!connection) {
		LOG_WARN("xmms2d is off -> ignore command\n");
		return 0;
	}

			
	switch (pc->cmd) {
		case REM_PC_CMD_JUMP:
			result = xmmsc_playlist_set_next(connection, pc->param);
			REM_PP_XMMS2_WAT_RESULT;
			xmmsc_result_unref(result);
			result = xmmsc_playback_tickle(connection);
			REM_PP_XMMS2_WAT_RESULT;
			xmmsc_result_unref(result);
			break;
			
		case REM_PC_CMD_NEXT:
			result = xmmsc_playlist_set_next_rel(connection, 1);
			REM_PP_XMMS2_WAT_RESULT;
			xmmsc_result_unref(result);
			result = xmmsc_playback_tickle(connection);
			REM_PP_XMMS2_WAT_RESULT;
			xmmsc_result_unref(result);
			break;
			
		case REM_PC_CMD_PREV:
			result = xmmsc_playlist_set_next_rel(connection, -1);
			REM_PP_XMMS2_WAT_RESULT;
			xmmsc_result_unref(result);
			result = xmmsc_playback_tickle(connection);
			REM_PP_XMMS2_WAT_RESULT;
			xmmsc_result_unref(result);
			break;
			
		case REM_PC_CMD_PLAY_PAUSE:
			result = xmmsc_playback_status(connection);
			REM_PP_XMMS2_WAT_RESULT;
			ret = xmmsc_result_get_uint(result, &u);
			xmmsc_result_unref(result);
			if (!ret) {
				LOG_WARN("sid: result broken");
				rem_pp_xmms2_disconnect("");
				break;
			}
			if (u == XMMS_PLAYBACK_STATUS_PLAY) {
				result = xmmsc_playback_pause(connection);
				REM_PP_XMMS2_WAT_RESULT;
				xmmsc_result_unref(result);
			} else {
				result = xmmsc_playback_start(connection);
				REM_PP_XMMS2_WAT_RESULT;
				xmmsc_result_unref(result);
				result = xmmsc_playback_tickle(connection);
				REM_PP_XMMS2_WAT_RESULT;
				xmmsc_result_unref(result);
			}
			break;
		case REM_PC_CMD_STOP:
			result = xmmsc_playback_stop(connection);
			REM_PP_XMMS2_WAT_RESULT;
			xmmsc_result_unref(result);
			break;
			
		case REM_PC_CMD_RESTART:
			result = xmmsc_playback_stop(connection);
			REM_PP_XMMS2_WAT_RESULT;
			xmmsc_result_unref(result);
			
			result = xmmsc_playlist_set_next(connection, 0);
			REM_PP_XMMS2_WAT_RESULT;
			xmmsc_result_unref(result);
			
			result = xmmsc_playback_start(connection);
			REM_PP_XMMS2_WAT_RESULT;
			xmmsc_result_unref(result);
			break;
			
		case REM_PC_CMD_VOLUME:
			result = xmmsc_playback_volume_set(connection, "left",
								pc->param);
			REM_PP_XMMS2_WAT_RESULT;
			xmmsc_result_unref(result);
			result = xmmsc_playback_volume_set(connection, "right",
								pc->param);
			REM_PP_XMMS2_WAT_RESULT;
			xmmsc_result_unref(result);
			break;
			
		case REM_PC_CMD_RATE:
			result = xmmsc_playback_current_id(connection);
			REM_PP_XMMS2_WAT_RESULT;
			ret = xmmsc_result_get_uint(result, &u);
			xmmsc_result_unref(result);
			if (!ret) {
				LOG_WARN("sid: result broken");
				rem_pp_xmms2_disconnect("");
				break;
			}
			result = xmmsc_medialib_entry_property_set_int(
					connection, u, "rating", pc->param);
			REM_PP_XMMS2_WAT_RESULT;
			xmmsc_result_unref(result);
			break;
			
		default:
			LOG_WARN("ignore command %hu\n", pc->cmd);
			break;
	}

	return 0;
}

void rem_pp_dispose()
{
	LOG_DEBUG("called\n");
	rem_pp_xmms2_disconnect("");
	return;
}

const char* rem_pp_get_name()
{
	LOG_DEBUG("called\n");
	return PP_NAME;
}

const char* rem_pp_get_version()
{
	LOG_NOISE("called\n");
	return PP_VERSION;
}

///////////////////////////////////////////////////////////////////////////////
//
// functions private
//
///////////////////////////////////////////////////////////////////////////////

static void
rem_pp_xmms2_disconnect(void *arg)
{
	LOG_DEBUG("called %s\n", arg ? "intern" : "extern");
	if (!connection) return;
	xmmsc_unref(connection);
	connection = NULL;
}

static void
rem_pp_xmms2_setup_connection()
{
	LOG_NOISE("called\n");
	
	connection = xmmsc_init ("remuco");
	if (!connection) {
		LOG_ERROR("init xmms2d connection failed\n");
		return;
	}

	if (xmmsc_connect(connection, getenv ("XMMS_PATH"))) {
		LOG_INFO("xmms2d is running\n");
		xmmsc_disconnect_callback_set(connection, rem_pp_xmms2_disconnect, NULL);
	} else {
		LOG_INFO("xmms2d is down (%s)\n",
					xmmsc_get_last_error (connection));
		xmmsc_unref(connection);
		connection = NULL;
	}
}


static void
rem_pp_xmms2_add_tag_to_song(struct rem_pp_song *s, xmmsc_result_t *result,
	int res_type, const char *tag_name_mlib, const char *tag_name_rem)
{
	LOG_NOISE("called (%s,%s)\n", tag_name_mlib, tag_name_rem);
	char *val_mlib, val_my[REM_MAX_STR_LEN], *val_final;
	int i;
	switch (res_type) {
		case XMMSC_RESULT_VALUE_TYPE_STRING:
			if (xmmsc_result_get_dict_entry_str(result,
						tag_name_mlib, &val_mlib)) {
				val_final = val_mlib;
			} else {
				val_final = "unknwon";
			}
			break;
		case XMMSC_RESULT_VALUE_TYPE_INT32:
			if (xmmsc_result_get_dict_entry_int32(result,
						tag_name_mlib, &i)) {
				snprintf(val_my, REM_MAX_STR_LEN, "%i", i);
				val_final = val_my;
			} else {
				val_final = "unknwon";
			}
			break;
		default:
			LOG_WARN("unkown xmms2 result type\n");
			val_final = "unknwon";
			break;
	}
	rem_song_append_tag(s, tag_name_rem, val_final);
}
