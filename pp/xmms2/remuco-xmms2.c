#include <xmmsclient/xmmsclient.h>
#include <xmmsclient/xmmsclient-glib.h>
#include <remuco.h>
#include <signal.h>

struct _RemPPPriv {
	
	xmmsc_connection_t	*xc;
	GMainLoop			*ml;	
	RemServer			*rs;
	guint				cap_id;	// id of the current active song
};

static RemPPPriv	*priv_global;	// we need a global ref to our private data
									// for the signal handler func priv_sigint()

///////////////////////////////////////////////////////////////////////////////
//
// constants
//
///////////////////////////////////////////////////////////////////////////////

/* Defined here because used more than once. */
#define XMETA_NAME_RATING	"rating"

/** X2 names of the meta informatioin we request from X2 */
static const gchar	*XMETA_NAMES[] = {
		"artist", "album",
		"title", "genre",
		"comment", "tracknr",
		"duration", "bitrate",
		XMETA_NAME_RATING };

/** Remuco names of the meta informatioin we request from X2 */
static const gchar	*RMETA_NAMES[] = {
		REM_PLOB_META_ARTIST, REM_PLOB_META_ALBUM,
		REM_PLOB_META_TITLE, REM_PLOB_META_GENRE,
		REM_PLOB_META_COMMENT, REM_PLOB_META_TRACK,
		REM_PLOB_META_LENGTH, REM_PLOB_META_BITRATE,
		REM_PLOB_META_RATING };

/** X2 value types the meta informatioin we request from X2 */
static const gint	XMETA_TYPES[] = { 
		XMMSC_RESULT_VALUE_TYPE_STRING, XMMSC_RESULT_VALUE_TYPE_STRING,
		XMMSC_RESULT_VALUE_TYPE_STRING, XMMSC_RESULT_VALUE_TYPE_STRING,
		XMMSC_RESULT_VALUE_TYPE_STRING, XMMSC_RESULT_VALUE_TYPE_INT32,
		XMMSC_RESULT_VALUE_TYPE_INT32, XMMSC_RESULT_VALUE_TYPE_INT32,
		XMMSC_RESULT_VALUE_TYPE_INT32 };

/** Count of meta information elements we request from X2 */
#define XMETA_COUNT	9

/** Number of the meta information element 'length' (or duration) */
#define XMETA_NUM_LENGTH	6

static const gchar *XMETA_NAMES_ART[] = {
		"album_front_large", "album_front_small", "album_front_thumbnail"
};

#define XMETA_ART_COUNT 3

///////////////////////////////////////////////////////////////////////////////
//
// macros for synchronous result waiting
//
///////////////////////////////////////////////////////////////////////////////

#define REMX2_RESULT_WAIT(_res) G_STMT_START {			\
	xmmsc_result_wait(_res);							\
	if (xmmsc_result_iserror(_res)) {					\
		LOG_WARN("X2 result error: %s\n", xmmsc_result_get_error(_res));	\
		xmmsc_result_unref(_res);						\
		return;											\
	}													\
} G_STMT_END

#define REMX2_RESULT_WAIT_RET(_res, _ret) G_STMT_START {\
	xmmsc_result_wait(_res);							\
	if (xmmsc_result_iserror(_res)) {					\
		LOG_WARN("X2 result error: %s\n", xmmsc_result_get_error(_res));	\
		xmmsc_result_unref(_res);						\
		return _ret;									\
	}													\
} G_STMT_END

///////////////////////////////////////////////////////////////////////////////
//
// xmms2 callback functions
//
///////////////////////////////////////////////////////////////////////////////

static void
xcb_disconnect(gpointer data)
{
	RemPPPriv	*priv = (RemPPPriv*) data;

	LOG_DEBUG("XMMS2 wants us to disconnect\n");
	
	rem_server_down(priv->rs);
}

///////////////////////////////////////////////////////////////////////////////
//
// misc utility functions
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Catch interrupt signal (SIGINT).
 */
static void
priv_sigint(gint sig)
{
	LOG_DEBUG("received interrupt signal\n");
	
	rem_server_down(priv_global->rs);
}

/**
 * Do the final necessary steps after the playlist position has been changed by
 * a client.
 */
static void
priv_finish_plob_change(RemPPPriv *priv)
{
	xmmsc_result_t	*result;
	gint			ret;
	guint			u;

	result = xmmsc_playback_tickle(priv->xc);
	xmmsc_result_unref(result);
	
	result = xmmsc_playback_status(priv->xc);
	
	REMX2_RESULT_WAIT(result);
	
	ret = xmmsc_result_get_uint(result, &u);
	g_assert(ret);
	
	xmmsc_result_unref(result);
	
	if (u != XMMS_PLAYBACK_STATUS_PLAY) {
		result = xmmsc_playback_start(priv->xc);
		xmmsc_result_unref(result);
	}
}

/**
 * Request a playlist from XMMS2 and convert the integer ID list to a string
 * PID list.
 * This function does not clear the string list @a pl before
 * appending the PIDs!
 * 
 * @param[in]  plid		a PLID
 * @param[out] pl		the string list to append the PIDs to
 */
static void
priv_get_playlist(RemPPPriv *priv, const gchar *plid, RemStringList *pl)
{
	LOG_NOISE("called\n");
	
	guint			id;
	gint			ret;
	GString			*pid;
	xmmsc_result_t	*result;
	
	result = xmmsc_playlist_list_entries(priv->xc, plid);

	REMX2_RESULT_WAIT(result);
	
	ret = xmmsc_result_is_list(result);
	g_assert(ret);

	pid = g_string_new_len("", 255);
	
	LOG_NOISE("playlist (%s): ", plid);
	for (xmmsc_result_list_first(result);
		 xmmsc_result_list_valid(result);
		 xmmsc_result_list_next(result))
	{
		ret = xmmsc_result_get_uint(result, &id);
		g_assert(ret);
		
		#if LOGLEVEL >= LL_NOISE
		LOG("%u ", id);
		#endif
		g_string_printf(pid, "%u", id);
		
		rem_sl_append_const(pl, pid->str);
	}
	#if LOGLEVEL >= LL_NOISE
	LOG("\n");
	#endif
	
	g_string_free(pid, TRUE);
	
	xmmsc_result_unref(result);	
}

/** Connect to XMMS2 */
static void
priv_connect_to_xmms2(RemPPPriv *priv)
{
	LOG_NOISE("called\n");
	
	priv->xc = xmmsc_init("remuco");
	
	g_assert(priv->xc);

	if (xmmsc_connect(priv->xc, g_getenv("XMMS_PATH"))) {
		LOG_INFO("xmms2d is running\n");
		xmmsc_disconnect_callback_set(priv->xc, &xcb_disconnect, priv);
	} else {
		LOG_ERROR("%s\n", xmmsc_get_last_error(priv->xc));
		xmmsc_unref(priv->xc);
		priv->xc = NULL;
	}
}

///////////////////////////////////////////////////////////////////////////////
//
// remuco callback functions
//
///////////////////////////////////////////////////////////////////////////////

static void
rcb_synchronize(RemPPPriv *priv, RemPlayerStatus *ps)
{
	xmmsc_result_t	*result;
	guint			u, v;
	gint			ret;
	
	////////// playback state //////////
	
	result = xmmsc_playback_status(priv->xc);

	REMX2_RESULT_WAIT(result);
	
	ret = xmmsc_result_get_uint(result, &u);
	g_assert(ret);

	LOG_NOISE("new (xmms2) pbs is %u\n", u);

	switch (u) {
		case XMMS_PLAYBACK_STATUS_PAUSE:
			ps->pbs = REM_PBS_PAUSE;
			break;
		case XMMS_PLAYBACK_STATUS_PLAY:
			ps->pbs = REM_PBS_PLAY;
			break;
		case XMMS_PLAYBACK_STATUS_STOP:
			ps->pbs = REM_PBS_STOP;
			break;
		default:
			LOG_BUG("unknown xmms2 playback status\n");
			break;
	}

	xmmsc_result_unref(result);

	////////// volume //////////
	
	result = xmmsc_playback_volume_get(priv->xc);

	REMX2_RESULT_WAIT(result);
	
	ret = xmmsc_result_get_dict_entry_uint(result, "left", &u);
	ret &= xmmsc_result_get_dict_entry_uint(result, "right", &v);
	g_assert(ret);
	
	ps->volume = u > v ? u : v;

	LOG_NOISE("new volume is %u:%u\n", u, v);

	xmmsc_result_unref(result);

	////////// repeat, shuffle //////////
	
	ps->repeat = REM_REPEAT_MODE_NONE;
	ps->shuffle = REM_SHUFFLE_MODE_OFF;
	
	////////// cap position //////////
	
	result = xmmsc_playlist_current_pos(priv->xc, XMMS_ACTIVE_PLAYLIST);
	
	xmmsc_result_wait(result);
	
	if (xmmsc_result_iserror(result)) {
		
		ps->cap_pos = 0; // song not in playlist
		
	} else {
		
		ret = xmmsc_result_get_uint(result, &u);
		g_assert(ret);
		
		LOG_NOISE("new (XMMS2) cap_pos is %u\n", u);

		ps->cap_pos = (gint) u + 1;
	}
	
	xmmsc_result_unref(result);
	
	////////// cap id //////////
	
	result = xmmsc_playback_current_id(priv->xc);
	
	REMX2_RESULT_WAIT(result);
	
	ret = xmmsc_result_get_uint(result, &u);
	g_assert(ret);
	
	LOG_NOISE("new cap id is %u\n", u);

	if (u == 0)
		g_string_truncate(ps->cap_pid, 0);
	else
		g_string_printf(ps->cap_pid, "%u", u);
	
	xmmsc_result_unref(result);
	
	priv->cap_id = u;	// remember for rating
	
	////////// playlist //////////
	
	rem_sl_clear(ps->playlist);
	
	priv_get_playlist(priv, XMMS_ACTIVE_PLAYLIST, ps->playlist);
	
}

static RemLibrary*
rcb_get_library(RemPPPriv *priv)
{
	RemLibrary		*lib;
	xmmsc_result_t	*result;
	gchar			*plid;
	gint			ret;
	
	lib = rem_library_new();
	
	result = xmmsc_playlist_list(priv->xc);

	REMX2_RESULT_WAIT_RET(result, lib);
	
	ret = xmmsc_result_is_list(result);
	g_assert(ret);
	
	for (xmmsc_result_list_first(result);
		 xmmsc_result_list_valid(result);
		 xmmsc_result_list_next(result))
	{
		ret = xmmsc_result_get_string(result, &plid);
		g_assert(ret);
		
		if (g_str_equal(plid, XMMS_ACTIVE_PLAYLIST)) continue;
		
		rem_library_append_const(lib, plid, plid, REM_PLOBLIST_FLAG_STATIC);
	}
	
	xmmsc_result_unref(result);
	
	return lib;
}

static RemPlob*
rcb_get_plob(RemPPPriv *priv, const gchar *pid)
{
	guint			u, id;
	gint			ret, val_i;
	gchar			*val_s;
	GString			*val_gs;
	RemPlob			*plob;
	xmmsc_result_t	*result;

	id = (guint) g_ascii_strtoull(pid, NULL, 10);
	g_assert(id); // id is 0 on error, and a pid of '0' is also an error
	
	LOG_DEBUG("read song %u from mlib\n", id);
	
	result = xmmsc_medialib_get_info(priv->xc, id);

	REMX2_RESULT_WAIT_RET(result, NULL);

	val_gs = g_string_new("123456");
		
	plob = rem_plob_new(pid);

	for (u = 0; u < XMETA_COUNT; u++) {
		
		switch (XMETA_TYPES[u]) {
		
			case XMMSC_RESULT_VALUE_TYPE_STRING:
				
				ret = xmmsc_result_get_dict_entry_string(
									result, XMETA_NAMES[u], &val_s);
				
				if (!ret || !val_s) break;
				
				rem_plob_meta_add_const(plob, RMETA_NAMES[u], val_s);
				
				break;
				
			case XMMSC_RESULT_VALUE_TYPE_INT32:
				
				ret = xmmsc_result_get_dict_entry_int(
									result, XMETA_NAMES[u], &val_i);
				
				if (!ret) break;
				
				if (u == XMETA_NUM_LENGTH) {
					val_i = val_i / 1000;
				}
				
				g_string_printf(val_gs, "%i", val_i);
					
				rem_plob_meta_add_const(plob, RMETA_NAMES[u], val_gs->str);

				break;
				
			default:
				g_assert_not_reached();
				break;
		}
	}
	
	g_string_free(val_gs, TRUE);

	////////// get album art url (try to get the biggest one) //////////
	
	for (u = 0; u < XMETA_ART_COUNT; u++) {
		
		ret = xmmsc_result_get_dict_entry_string(
							result, XMETA_NAMES_ART[u], &val_s);
		
		if (ret && val_s && val_s[0] != 0) {
			rem_plob_meta_add_const(plob, REM_PLOB_META_ART, val_s);
			break;
		}
		
	}
	
	xmmsc_result_unref(result);
	
	return plob;	
}

static RemStringList*
rcb_get_ploblist(RemPPPriv *priv, const gchar *plid)
{
	RemStringList	*pl;

	pl = rem_sl_new();
	
	priv_get_playlist(priv, plid, pl);
	
	return pl;
}

static void
rcb_notify(RemPPPriv *priv, RemServerEvent event)
{
	switch (event) {
		case REM_SERVER_EVENT_ERROR:
			LOG_ERROR("server experienced serious error -> shut down server\n");
			rem_server_down(priv->rs);
			break;
		case REM_SERVER_EVENT_DOWN:
			LOG_DEBUG("server shut down finished\n");
			g_main_loop_quit(priv->ml);
			break;
		default:
			g_assert_not_reached();
			break;
	}
}

static void
rcb_play_ploblist(RemPPPriv *priv, const gchar *plid)
{
	xmmsc_result_t	*result;

	result = xmmsc_playlist_load(priv->xc, plid);
	xmmsc_result_unref(result);
}

// FUTUTE FEATURE
//static RemStringList*
//rcb_search(RemPPPriv *priv, const RemPlob *plob);

static void
rcb_simple_control(RemPPPriv *priv, RemSimpleControlCommand cmd, gint param)
{
	xmmsc_result_t	*result;
	guint			u;
	gint			ret;

	LOG_DEBUG("command: %hu, param: %hu\n", cmd, param);

	switch (cmd) {
		case REM_SCTRL_CMD_JUMP:
			
			result = xmmsc_playlist_set_next(priv->xc, param - 1);
			xmmsc_result_unref(result);
			
			priv_finish_plob_change(priv);
			
			break;
			
		case REM_SCTRL_CMD_NEXT:
			
			result = xmmsc_playlist_set_next_rel(priv->xc, 1);
			xmmsc_result_unref(result);
			
			priv_finish_plob_change(priv);
			
			break;
			
		case REM_SCTRL_CMD_PREV:
			
			result = xmmsc_playlist_set_next_rel(priv->xc, -1);
			xmmsc_result_unref(result);
			
			priv_finish_plob_change(priv);
			
			break;
			
		case REM_SCTRL_CMD_PLAYPAUSE:
			
			result = xmmsc_playback_status(priv->xc);
			REMX2_RESULT_WAIT(result);
			ret = xmmsc_result_get_uint(result, &u);
			g_assert(ret);
			xmmsc_result_unref(result);

			if (u == XMMS_PLAYBACK_STATUS_PLAY)
				result = xmmsc_playback_pause(priv->xc);
			else
				result = xmmsc_playback_start(priv->xc);

			xmmsc_result_unref(result);

			break;
			
		case REM_SCTRL_CMD_STOP:
			
			result = xmmsc_playback_stop(priv->xc);
			xmmsc_result_unref(result);
			
			break;
			
		case REM_SCTRL_CMD_RESTART:
			
			result = xmmsc_playback_stop(priv->xc);
			xmmsc_result_unref(result);
			
			result = xmmsc_playlist_set_next(priv->xc, 1);
			xmmsc_result_unref(result);
			
			result = xmmsc_playback_start(priv->xc);
			xmmsc_result_unref(result);
			
			break;
			
		case REM_SCTRL_CMD_VOLUME:
			
			result = xmmsc_playback_volume_set(priv->xc, "left", param);
			xmmsc_result_unref(result);
			
			result = xmmsc_playback_volume_set(priv->xc, "right", param);
			xmmsc_result_unref(result);
			
			break;
			
		case REM_SCTRL_CMD_RATE:
			
			if (!priv->cap_id > 0) break; // no currently active plob
			
			result = xmmsc_medialib_entry_property_set_int(
						priv->xc, priv->cap_id, XMETA_NAME_RATING, param);
			xmmsc_result_unref(result);

			break;
			
		case REM_SCTRL_CMD_REPEAT:
			
			// X2 has no repeat mode
			
			break;
		case REM_SCTRL_CMD_SHUFFLE:
			
			if (param == REM_SHUFFLE_MODE_OFF) break;
			
			// X2 has no shuffle mode but tha playlist can be shuffled:
			result = xmmsc_playlist_shuffle(priv->xc, XMMS_ACTIVE_PLAYLIST);
			xmmsc_result_unref(result);
			
			break;
		default:
			LOG_WARN("ignore command %hu\n", cmd);
			break;
	}
	
}

// FUTUTE FEATURE
//static void
//rcb_update_plob(RemPPPriv *pp_priv, const RemPlob *plob);

// FUTUTE FEATURE
//static void
//rcb_update_ploblist(RemPPPriv *pp_priv,
//					const gchar *plid,
//					const RemStringList* pids);

///////////////////////////////////////////////////////////////////////////////
//
// main
//
///////////////////////////////////////////////////////////////////////////////

int main(int argc, char **argv) {
	
	RemPPPriv		*priv;
	RemPPCallbacks	*rcb;
	RemPPDescriptor	*ppd;
	GError			*err;
	
	rem_log_init(REM_LL_DEBUG);
	
    ////// signal handler //////

    if (signal(SIGINT, priv_sigint) == SIG_ERR) {
            LOG_ERRNO("install sighandler for SIGINT failed");
            return 1;
    }

    ////////// init private data //////////
    
    priv = g_new0(RemPPPriv, 1);
    
	priv_global = priv;
	
	priv_connect_to_xmms2(priv);
	
	if (!priv->xc) return 1;
	
	////////// set callbacks for remuco server //////////

	rcb = g_new(RemPPCallbacks, 1);
	
	rcb->get_library = &rcb_get_library;
	rcb->get_plob = &rcb_get_plob;
	rcb->get_ploblist = &rcb_get_ploblist;
	rcb->notify = &rcb_notify;
	rcb->play_ploblist = &rcb_play_ploblist;
	rcb->search = NULL; // FUTURE FEATURE &rcb_search;
	rcb->simple_ctrl = &rcb_simple_control;
	rcb->synchronize = &rcb_synchronize;
	rcb->update_plob = NULL; // FUTURE FEATURE &rcb_update_plob;
	rcb->update_ploblist = NULL; // FUTURE FEATURE &rcb_update_ploblist;

	////////// set up pp descriptor //////////

	ppd = g_new(RemPPDescriptor, 1);

	ppd->charset = NULL;
	ppd->max_rating_value = 5;
	ppd->player_name = g_strdup("XMMS2");
	ppd->supported_repeat_modes = 0;
	ppd->supported_shuffle_modes = REM_SHUFFLE_MODE_OFF | REM_SHUFFLE_MODE_ON;
	ppd->supports_playlist = TRUE;
	ppd->supports_playlist_jump = TRUE;
	ppd->supports_queue = FALSE;
	ppd->supports_queue_jump = FALSE;
	ppd->supports_seek = FALSE; // TODO TRUE;
	ppd->supports_tags = FALSE; // TODO TRUE;
	
	////////// start remuco server //////////

	err = NULL;
	priv->rs = rem_server_up(ppd, rcb, priv, &err);
	
	if (err) {
		LOG_ERROR("starting server failed: %s\n", err->message);
		g_error_free(err);
	}
	
	if (!priv->rs) {
		g_free(priv);
		return 1;
	}
	
	////////// set up and run main loop (for the default context) //////////
	
	priv->ml = g_main_loop_new(NULL, FALSE);

	rem_server_poll(priv->rs);
	
	LOG_DEBUG("now running main loop\n");
	g_main_loop_run(priv->ml);
	LOG_DEBUG("back from main loop\n");

	////////// shut down //////////

	// rem_server_shutdown() already has been called in priv_sigint() or
	// xcb_disconnect(). So when we are here, then because rcb_notify() has been
	// called by the server.
	
	xmmsc_unref(priv->xc);
	
	g_main_loop_unref(priv->ml);
	
	g_free(priv);
	
	LOG_INFO("bye..\n");
	
	return 0;
}
