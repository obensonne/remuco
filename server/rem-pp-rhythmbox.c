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

#include "rem-pp.h"
#include "rem-dbus.h"
#include "rem-log.h"

///////////////////////////////////////////////////////////////////////////////
//
// macros and constants
//
///////////////////////////////////////////////////////////////////////////////

#define REM_PP_RHYTHMBOX_DBUS_SERVICE		"org.gnome.Rhythmbox"
#define REM_PP_RHYTHMBOX_DBUS_PATH_PLAYER	"/org/gnome/Rhythmbox/Player"
#define REM_PP_RHYTHMBOX_DBUS_PATH_SHELL	"/org/gnome/Rhythmbox/Shell"
#define REM_PP_RHYTHMBOX_DBUS_IFACE_PLAYER	"org.gnome.Rhythmbox.Player"
#define REM_PP_RHYTHMBOX_DBUS_IFACE_SHELL	"org.gnome.Rhythmbox.Shell"

///////////////////////////////////////////////////////////////////////////////
//
// private function prototypes
//
///////////////////////////////////////////////////////////////////////////////

static int
rem_pp_rhythmbox_dbus_init(void);

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
rem_pp_rhythmbox_dbus_volume(int val);

///////////////////////////////////////////////////////////////////////////////
//
// global vars
//
///////////////////////////////////////////////////////////////////////////////

static struct rem_dbus_proxy dbp_player, dbp_shell;

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
	
	ret = rem_dbus_get_proxy(&dbp_player);
	if (ret < 0)
		return -1;
	
	ret = rem_dbus_get_proxy(&dbp_shell);
	if (ret < 0)
		return -1;
	
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
	LOG_INFO("process command %hu with param %hu\n", pc->cmd, pc->param);
	
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
			rem_pp_rhythmbox_dbus_volume(pc->param);
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
	
	// This function gets called if the Remuco server shuts down.
	// Do some clean up work if needed.
	
}

///////////////////////////////////////////////////////////////////////////////
//
// functions - internal
//
///////////////////////////////////////////////////////////////////////////////

static int
rem_pp_rhythmbox_dbus_init()
{
	DBusGConnection *g_dbus_conn;
	GError *g_err;
	gboolean ret;
	DBusGProxy	*dbp;
	
	g_type_init();

	LOG_DEBUG("%i\n", __LINE__);

	g_err = NULL;
	g_dbus_conn = dbus_g_bus_get(DBUS_BUS_SESSION, &g_err);
	if (!g_dbus_conn) {
		LOG_ERROR("open dbus-connection failed: %s\n", g_err->message);
		g_error_free(g_err);
		return -1;
	}

	LOG_DEBUG("%i\n", __LINE__);

	// Create a proxy object for the "bus driver"
	dbp = dbus_g_proxy_new_for_name(g_dbus_conn,
					REM_PP_RHYTHMBOX_DBUS_SERVICE,
					REM_PP_RHYTHMBOX_DBUS_PATH_PLAYER,
					REM_PP_RHYTHMBOX_DBUS_IFACE_INTROSPECT);
	g_err = NULL;
	char *str = NULL;
	ret = dbus_g_proxy_call(dbp, "Introspect", &g_err, G_TYPE_INVALID,
					G_TYPE_STRING, &str, G_TYPE_INVALID);
	REM_PP_RHYTHMBOX_CHECK_DBUS_RET(ret, g_err);
	if (!ret) {
		return -1;
	}
	LOG_DEBUG("%s\n", str);
	free(str);

	// Create a proxy object for the "bus driver"
	dbp_player = dbus_g_proxy_new_for_name(g_dbus_conn,
					REM_PP_RHYTHMBOX_DBUS_SERVICE,
					REM_PP_RHYTHMBOX_DBUS_PATH_PLAYER,
					REM_PP_RHYTHMBOX_DBUS_IFACE_PLAYER);

	// Create a proxy object for the "bus driver"
	dbp_shell = dbus_g_proxy_new_for_name(g_dbus_conn,
					REM_PP_RHYTHMBOX_DBUS_SERVICE,
					REM_PP_RHYTHMBOX_DBUS_PATH_SHELL,
					REM_PP_RHYTHMBOX_DBUS_IFACE_SHELL);

	return (dbp_player && dbp_shell) ? 0 : -1;
}


static void
rem_pp_rhythmbox_dbus_playpause()
{
	GError *g_error;
	gboolean ret, gb;
	gb = 1;
	g_error = NULL;
	ret = dbus_g_proxy_call(dbp_player, "playPause", &g_error,
			G_TYPE_BOOLEAN, &gb, G_TYPE_INVALID,
			G_TYPE_INVALID);
	REM_PP_RHYTHMBOX_CHECK_DBUS_RET(ret, g_error);	
	
}


static void
rem_pp_rhythmbox_dbus_next()
{
	GError *g_error;
	gboolean ret;
	
	g_error = NULL;
	ret = dbus_g_proxy_call(dbp_player, "next", &g_error,
			G_TYPE_INVALID,
			G_TYPE_INVALID);
	REM_PP_RHYTHMBOX_CHECK_DBUS_RET(ret, g_error);	
}

static void
rem_pp_rhythmbox_dbus_prev()
{
	GError *g_error;
	gboolean ret;
	
	g_error = NULL;
	ret = dbus_g_proxy_call(dbp_player, "previous", &g_error,
			G_TYPE_INVALID,
			G_TYPE_INVALID);
	REM_PP_RHYTHMBOX_CHECK_DBUS_RET(ret, g_error);
}

static void
rem_pp_rhythmbox_dbus_stop()
{
	GError *g_error;
	gboolean ret;
	unsigned int u = 0;
	
	g_error = NULL;
	ret = dbus_g_proxy_call(dbp_player, "setElapsed", &g_error,
			G_TYPE_UINT, &u, G_TYPE_INVALID,
			G_TYPE_INVALID);
	REM_PP_RHYTHMBOX_CHECK_DBUS_RET(ret, g_error);	
	
	g_error = NULL;
	ret = dbus_g_proxy_call(dbp_player, "playPause", &g_error,
			G_TYPE_BOOLEAN, &gb, G_TYPE_INVALID,
			G_TYPE_INVALID);
	REM_PP_RHYTHMBOX_CHECK_DBUS_RET(ret, g_error);	
}

static void
rem_pp_rhythmbox_dbus_restart()
{
}

static void
rem_pp_rhythmbox_dbus_jump(int pos)
{
}

static void
rem_pp_rhythmbox_dbus_rate(int val)
{
}

static void
rem_pp_rhythmbox_dbus_volume(int val)
{
}

static int
rem_pp_rhythmbox_dbus_get_state()
{
	return 4
}

static int
rem_pp_rhythmbox_dbus_get_volume()
{
	return 50
}

static int
rem_pp_rhythmbox_dbus_get_plpos()
{
	return REM_PS_PL_POS_NONE;
}
