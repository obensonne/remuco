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
 * Player proxy Rhythmbox.
 * 
 */

///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include <stdlib.h>
#include <string.h>

#include "../../rem-pp.h"
#include "../../rem-dbus.h"
#include "../../rem-log.h"
#include "../../rem-pp-util.h"

///////////////////////////////////////////////////////////////////////////////
//
// macros and constants
//
///////////////////////////////////////////////////////////////////////////////

#define REM_PP_RHYTHMBOX_TAG_ARTIST	"artist"
#define REM_PP_RHYTHMBOX_TAG_TITLE	"title"
#define REM_PP_RHYTHMBOX_TAG_ALBUM	"album"
#define REM_PP_RHYTHMBOX_TAG_GENRE	"genre"
#define REM_PP_RHYTHMBOX_TAG_YEAR	"year"
#define REM_PP_RHYTHMBOX_TAG_RATING	"rating"
#define REM_PP_RHYTHMBOX_TAG_COMMENT	"n.a."
#define REM_PP_RHYTHMBOX_TAG_BITRATE	"bitrate"
#define REM_PP_RHYTHMBOX_TAG_LENGTH	"duration"
#define REM_PP_RHYTHMBOX_TAG_TRACK	"track-number"

///////////////////////////////////////////////////////////////////////////////
//
// private function prototypes
//
///////////////////////////////////////////////////////////////////////////////

static int
rem_pp_rhythmbox_connect(void);

static void
rem_pp_rhythmbox_dbus_playpause(void);

static void
rem_pp_rhythmbox_dbus_next(void);

static void
rem_pp_rhythmbox_dbus_prev(void);

static void
rem_pp_rhythmbox_dbus_jump(int pos);

static void
rem_pp_rhythmbox_dbus_stop(void);

static void
rem_pp_rhythmbox_dbus_restart(void);

static void
rem_pp_rhythmbox_dbus_rate(int val);

static void
rem_pp_rhythmbox_dbus_set_volume(int val);

static int
rem_pp_rhythmbox_dbus_get_state(void);

static int
rem_pp_rhythmbox_dbus_get_volume(void);

static int
rem_pp_rhythmbox_dbus_get_pl(struct rem_pp_ps *ps);

static char*
rem_pp_rhythmbox_dbus_getcurrenturi(void);

static void
rem_pp_rhythmbox_apptag(gpointer hash_key, gpointer hash_value, gpointer ud);

///////////////////////////////////////////////////////////////////////////////
//
// global vars
//
///////////////////////////////////////////////////////////////////////////////

static struct rem_dbus_proxy dbp_player, dbp_shell, dbp_plman;

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

	return rem_pp_rhythmbox_connect();
}

int
rem_pp_get_ps(struct rem_pp_ps *ps)
{	
	LOG_NOISE("called\n");
	
	int			ret;

	if (!(dbp_player.dbp && dbp_shell.dbp && dbp_plman.dbp)) {
		LOG_WARN("dbus connection seems broken\n");
		rem_dbus_disconnect();
		ret = rem_pp_rhythmbox_connect();
		if (ret < 0) {
			return -1;
		}
		LOG_INFO("dbus connection ok again\n");
	}

	// set current music player state
	ps->state = rem_pp_rhythmbox_dbus_get_state();
	ps->volume = rem_pp_rhythmbox_dbus_get_volume();
	ps->pl_repeat = 0;
	ps->pl_shuffle = 0;

	LOG_NOISE("getting playlist\n");
	ret = rem_pp_rhythmbox_dbus_get_pl(ps);
	if (ret < 0) {
		return -1;
	}
	
	return 0;
}


void
rem_pp_free_ps(struct rem_pp_ps *ps)
{
	LOG_NOISE("called\n");

	unsigned int u;

	for (u = 0; u < ps->pl_len; u++) {
 		g_free(ps->pl_sid_list[u].str);
	}
	free(ps->pl_sid_list);
	ps->pl_sid_list = NULL;
}

int
rem_pp_get_song(const union rem_pp_sid *sid, struct rem_pp_song *song)
{
	LOG_NOISE("called\n");

	GError		*g_err;
	gboolean	ret;
	unsigned int	u;
	unsigned long	gt_asv;

	GHashTable	*hash;
	

	// get the glib type code for the returned hastable (a{sv})
	gt_asv = dbus_g_type_get_map ("GHashTable", G_TYPE_STRING, G_TYPE_VALUE);
	
	g_err = NULL;
	ret = dbus_g_proxy_call(dbp_shell.dbp, "getSongProperties", &g_err,
		G_TYPE_STRING, sid->str, G_TYPE_INVALID,
		gt_asv, &hash, G_TYPE_INVALID);
	REM_DBUS_CHECK_RET(ret, g_err);
	if (!ret)
		return -1;

	if (!hash) {
		LOG_ERROR("no data\n");
		return -1;
	}
	u = g_hash_table_size(hash);
	
	LOG_DEBUG("song dict has %u elems\n", u);

	g_hash_table_foreach(hash, rem_pp_rhythmbox_apptag, song);

	g_hash_table_destroy(hash);

	return 0;
}

int
rem_pp_process_cmd(struct rem_pp_pc *pc)
{
	LOG_DEBUG("process command %hu with param %hu\n", pc->cmd, pc->param);
	
	switch (pc->cmd) {
		case REM_PC_CMD_JUMP:
			rem_pp_rhythmbox_dbus_jump(pc->param);
			break;
			
		case REM_PC_CMD_NEXT:
			rem_pp_rhythmbox_dbus_next();
			break;
			
		case REM_PC_CMD_PREV:
			rem_pp_rhythmbox_dbus_prev();
			break;
			
		case REM_PC_CMD_PLAY_PAUSE:
			rem_pp_rhythmbox_dbus_playpause();
			break;
			
		case REM_PC_CMD_STOP:
			rem_pp_rhythmbox_dbus_stop();
			break;
			
		case REM_PC_CMD_RESTART:
			rem_pp_rhythmbox_dbus_restart();
			break;
			
		case REM_PC_CMD_VOLUME:
			rem_pp_rhythmbox_dbus_set_volume(pc->param);
			break;
			
		case REM_PC_CMD_RATE:
			rem_pp_rhythmbox_dbus_rate(pc->param);
			break;
			
		default:
			LOG_WARN("ignore command %hu\n", pc->cmd);
			break;
	}

	return 0;
}

void rem_pp_dispose()
{
	LOG_NOISE("called\n");
	
	rem_dbus_disconnect();	
}

///////////////////////////////////////////////////////////////////////////////
//
// functions - private
//
///////////////////////////////////////////////////////////////////////////////

static int
rem_pp_rhythmbox_connect()
{
	int ret;

	ret = rem_dbus_connect();
	if (ret < 0)
		return -1;
	
	dbp_player.service = "org.gnome.Rhythmbox";
	dbp_player.path = "/org/gnome/Rhythmbox/Player";
	dbp_player.iface = "org.gnome.Rhythmbox.Player";
		
	dbp_shell.service = "org.gnome.Rhythmbox";	
	dbp_shell.path = "/org/gnome/Rhythmbox/Shell";
	dbp_shell.iface = "org.gnome.Rhythmbox.Shell";
	
	dbp_plman.service = "org.gnome.Rhythmbox";
	dbp_plman.path = "/org/gnome/Rhythmbox/PlaylistManager";
	dbp_plman.iface = "org.gnome.Rhythmbox.PlaylistManager";

	ret = rem_dbus_get_proxy(&dbp_player);
	if (ret < 0)
		return -1;
	
	ret = rem_dbus_get_proxy(&dbp_shell);
	if (ret < 0)
		return -1;

	ret = rem_dbus_get_proxy(&dbp_plman);
	if (ret < 0)
		return -1;
		
	return 0;
}

/// set ///

static void
rem_pp_rhythmbox_dbus_playpause()
{
	gboolean gb;
	gb = 1;
	dbus_g_proxy_call_no_reply(dbp_player.dbp, "playPause",
					G_TYPE_BOOLEAN, &gb, G_TYPE_INVALID);	
}


static void
rem_pp_rhythmbox_dbus_next()
{
	dbus_g_proxy_call_no_reply(dbp_player.dbp, "next", G_TYPE_INVALID);	
}

static void
rem_pp_rhythmbox_dbus_prev()
{
	dbus_g_proxy_call_no_reply(dbp_player.dbp, "previous", G_TYPE_INVALID);	
}

static void
rem_pp_rhythmbox_dbus_stop()
{
	GError		*g_err;
	gboolean	playing, ret;

	g_err = NULL;
	ret = dbus_g_proxy_call(dbp_player.dbp, "getPlaying", &g_err,
				G_TYPE_INVALID,
				G_TYPE_BOOLEAN, &playing, G_TYPE_INVALID);
	REM_DBUS_CHECK_RET(ret, g_err);
	if (!ret)
		playing = 0;

	if (playing)
		rem_pp_rhythmbox_dbus_playpause();

	dbus_g_proxy_call_no_reply(dbp_player.dbp, "setElapsed",
					G_TYPE_UINT, 0, G_TYPE_INVALID);
	
}

static void
rem_pp_rhythmbox_dbus_restart()
{
	rem_pp_rhythmbox_dbus_stop();
	rem_pp_rhythmbox_dbus_playpause();
}

static void
rem_pp_rhythmbox_dbus_jump(int pos)
{
	rem_pp_rhythmbox_dbus_restart();
}

static void
rem_pp_rhythmbox_dbus_rate(int val)
{
	char		*uri;
	gdouble		rate;
	GValue		g_val;

	rate = (double) val;
	
	memset(&g_val, 0, sizeof(GValue));
	g_value_init(&g_val, G_TYPE_DOUBLE);
	g_value_set_double(&g_val, rate);
	
	uri = rem_pp_rhythmbox_dbus_getcurrenturi();
	if (!uri) {
		LOG_WARN("could not get current song URI\n");
		return;
	}

	dbus_g_proxy_call_no_reply(dbp_shell.dbp, "setSongProperty",
		G_TYPE_STRING, uri, G_TYPE_STRING, REM_PP_RHYTHMBOX_TAG_RATING,
		G_TYPE_VALUE, &g_val, G_TYPE_INVALID);
	
	g_free(uri);
}

static void
rem_pp_rhythmbox_dbus_set_volume(int val)
{
	gdouble		vol;
	
	vol = (double) val / 100;
	dbus_g_proxy_call_no_reply(dbp_player.dbp, "setVolume",
					G_TYPE_DOUBLE, vol, G_TYPE_INVALID);
}

/// get ///

static int
rem_pp_rhythmbox_dbus_get_state()
{
	GError		*g_err;
	gboolean	gb, ret;
	guint		u;
	char		*uri;
	
	uri = rem_pp_rhythmbox_dbus_getcurrenturi();
	if (!uri)
		return REM_PS_STATE_STOP;
	
	g_err = NULL;
	ret = dbus_g_proxy_call(dbp_player.dbp, "getPlaying", &g_err,
					G_TYPE_INVALID,
					G_TYPE_BOOLEAN, &gb, G_TYPE_INVALID);
	REM_DBUS_CHECK_RET(ret, g_err);
	if (!ret) {
		return REM_PS_STATE_ERROR;
	} else if (gb) {
		return REM_PS_STATE_PLAY;
	} else {
		g_err = NULL;
		ret = dbus_g_proxy_call(dbp_player.dbp, "getElapsed", &g_err,
					G_TYPE_INVALID,
					G_TYPE_UINT, &u, G_TYPE_INVALID);
		REM_DBUS_CHECK_RET(ret, g_err);
		if (!ret)
			return REM_PS_STATE_ERROR;
		else if (u)
			return REM_PS_STATE_PAUSE;
		else
			return REM_PS_STATE_STOP;
	}
}

static int
rem_pp_rhythmbox_dbus_get_volume()
{
	GError		*g_err;
	gboolean	ret;
	gdouble		vol;
	
	g_err = NULL;
	ret = dbus_g_proxy_call(dbp_player.dbp, "getVolume", &g_err,
					G_TYPE_INVALID,
					G_TYPE_DOUBLE, &vol, G_TYPE_INVALID);
	REM_DBUS_CHECK_RET(ret, g_err);
	if (!ret)
		return REM_PS_STATE_ERROR;
	else
		return (int) (vol * 100);
}

static int
rem_pp_rhythmbox_dbus_get_pl(struct rem_pp_ps *ps)
{
	char		*uri;

	// since Rhythmbox does not offer methods to read the current
	// playlist (I guess Rhythmbox acutally does not really have something
	// like an allways valid _current_ playlist) we only can offer the
	// current song as the playlist
	
	ps->pl_pos = 0;
	ps->pl_sid_type = REM_PP_SID_TYPE_STRING;

	uri = rem_pp_rhythmbox_dbus_getcurrenturi();
	if (uri) {
		ps->pl_len = 1;
		ps->pl_sid_list = malloc(sizeof(union rem_pp_sid));
		if (!ps->pl_sid_list) {
			LOG_ERROR("malloc failed\n");
			return -1;
		}
		ps->pl_sid_list[0].str = uri;
	} else {
		ps->pl_len = 0;
		ps->pl_sid_list = NULL;
	}

	return 0;
}

static char*
rem_pp_rhythmbox_dbus_getcurrenturi()
{
	GError		*g_err;
	gboolean	ret;
	char		*uri;

	g_err = NULL;
	ret = dbus_g_proxy_call(dbp_player.dbp, "getPlayingUri", &g_err,
				G_TYPE_INVALID,
				G_TYPE_STRING, &uri, G_TYPE_INVALID);
	REM_DBUS_CHECK_RET(ret, g_err);
	LOG_NOISE("current uri: %s\n", uri);
	if (!ret || !uri || strlen(uri) == 0) {
		return NULL;
	} else {
		return uri;
	}
}

/**
 * Processes an entry from the hash table returned by dbus call
 * 'getSongProperties'. It checks the type of the entry and converts it to
 * an Remuco formated song tag and appends it to the song pointed to by ud.
 * 
 * @param hash_key (char*)
 * 	tag name as used by Rhythmbox
 * @param hash_value (GValue*)
 * 	tag value as used by Rhythmbox
 * @param ud (struct rem_song*)
 * 	song to fill with the song metad data from Rhythmbox
 */
static void
rem_pp_rhythmbox_apptag(gpointer hash_key, gpointer hash_value, gpointer ud)
{
	const char		*tag_name, *tag_value;
	char			gv_str[256];
	struct rem_pp_song	*song;
	GValue			*gv;
	
	tag_name	= (char*) hash_key;
	gv		= (GValue*) hash_value;
	song		= (struct rem_pp_song*) ud;
	
	tag_value	= NULL;

	memset(gv_str, 0, 256);

	LOG_NOISE("process key %s\n", tag_name);
	
	// convert the values to strings

	switch (G_VALUE_TYPE(gv)) {
		case G_TYPE_STRING:
			tag_value = g_value_get_string(gv);		
			break;
		case G_TYPE_INT:
			snprintf(gv_str, 255, "%i", g_value_get_int(gv));
			tag_value = gv_str; 			
			break;
		case G_TYPE_UINT:
			snprintf(gv_str, 255, "%u", g_value_get_uint(gv));
			tag_value = gv_str; 			
			break;
		case G_TYPE_DOUBLE:
			if (strcmp(tag_name, REM_PP_RHYTHMBOX_TAG_RATING) == 0) {
				// it's a rating => format appropriately 
				snprintf(gv_str, 255, "%1.0f/5",
						g_value_get_double(gv)); 
			} else {
				snprintf(gv_str, 255, "%f",
						g_value_get_double(gv));
			}
			tag_value = gv_str; 			
			break;
		case G_TYPE_BOOLEAN:
			if (g_value_get_boolean(gv)) {
				snprintf(gv_str, 255, "yes"); 
			} else {
				snprintf(gv_str, 255, "no"); 
			}			
			tag_value = gv_str; 			
			break;
		default:
			LOG_DEBUG("value type %lu not supported\n",
							G_VALUE_TYPE(gv));
			return;
	}	
	
	// map entries to remuco tag names
	
	if (strcmp(tag_name, REM_PP_RHYTHMBOX_TAG_ARTIST) == 0) {
		tag_name = REM_TAG_NAME_ARTIST;
	} else if (strcmp(tag_name, REM_PP_RHYTHMBOX_TAG_TITLE) == 0) {
		tag_name = REM_TAG_NAME_TITLE;
	} else if (strcmp(tag_name, REM_PP_RHYTHMBOX_TAG_ALBUM) == 0) {
		tag_name = REM_TAG_NAME_ALBUM;
	} else if (strcmp(tag_name, REM_PP_RHYTHMBOX_TAG_GENRE) == 0) {
		tag_name = REM_TAG_NAME_GENRE;
	} else if (strcmp(tag_name, REM_PP_RHYTHMBOX_TAG_YEAR) == 0) {
		tag_name = REM_TAG_NAME_YEAR;
	} else if (strcmp(tag_name, REM_PP_RHYTHMBOX_TAG_RATING) == 0) {
		tag_name = REM_TAG_NAME_RATING;
	} else if (strcmp(tag_name, REM_PP_RHYTHMBOX_TAG_LENGTH) == 0) {
		tag_name = REM_TAG_NAME_LENGTH;
	} else if (strcmp(tag_name, REM_PP_RHYTHMBOX_TAG_TRACK) == 0) {
		tag_name = REM_TAG_NAME_TRACK;
	} else if (strcmp(tag_name, REM_PP_RHYTHMBOX_TAG_BITRATE) == 0) {
		tag_name = REM_TAG_NAME_BITRATE;
	} else if (strcmp(tag_name, REM_PP_RHYTHMBOX_TAG_COMMENT) == 0) {
		tag_name = REM_TAG_NAME_COMMENT;
	} else {
		LOG_NOISE("ignore rhythmbox tag %s\n", tag_name);
		return;
	}
	
	// tag name and value are properly formated for Remuco => now append
	// it to the song

	rem_song_append_tag(song, tag_name, tag_value);
	
} 
