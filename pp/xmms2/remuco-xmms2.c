#include <xmmsclient/xmmsclient.h>
#include <xmmsclient/xmmsclient-glib.h>
#include <remuco.h>

struct _RemPPPriv {
	
	xmmsc_connection_t	*xc;
	GMainLoop			*ml;
	RemPlayerStatus		*ps;
	gboolean			status_changed;
	gboolean			cap_changed;
	gboolean			playlist_changed;
	RemServer			*rs;
	
};

///////////////////////////////////////////////////////////////////////////////
//
// constants
//
///////////////////////////////////////////////////////////////////////////////

#define XMETA_NAME_RATING	"rating"

static const gchar	*XMETA_NAMES[] = {
		"artist", "album",
		"title", "genre",
		"comment", "tracknr",
		"duration", "bitrate",
		XMETA_NAME_RATING };

static const gchar	*RMETA_NAMES[] = {
		REM_PLOB_META_ARTIST, REM_PLOB_META_ALBUM,
		REM_PLOB_META_TITLE, REM_PLOB_META_GENRE,
		REM_PLOB_META_COMMENT, REM_PLOB_META_TRACK,
		REM_PLOB_META_LENGTH, REM_PLOB_META_BITRATE,
		REM_PLOB_META_RATING };

static const gint	XMETA_TYPES[] = { 
		XMMSC_RESULT_VALUE_TYPE_STRING, XMMSC_RESULT_VALUE_TYPE_STRING,
		XMMSC_RESULT_VALUE_TYPE_STRING, XMMSC_RESULT_VALUE_TYPE_STRING,
		XMMSC_RESULT_VALUE_TYPE_STRING, XMMSC_RESULT_VALUE_TYPE_INT32,
		XMMSC_RESULT_VALUE_TYPE_INT32, XMMSC_RESULT_VALUE_TYPE_INT32,
		XMMSC_RESULT_VALUE_TYPE_INT32 };

#define XMETA_COUNT	9

#define XMETA_NUM_LENGTH	6

///////////////////////////////////////////////////////////////////////////////
//
// xmms2 callback functions - prototypes
//
///////////////////////////////////////////////////////////////////////////////

static void
xcb_disconnect(gpointer data);

static void
xcb_state_changed(xmmsc_result_t *result, gpointer data);

static void
xcb_volume_changed(xmmsc_result_t *result, gpointer data);

static void
xcb_cap_changed(xmmsc_result_t *result, gpointer data);

static void
xcb_cap_plpos_changed(xmmsc_result_t *result, gpointer data);

static void
xcb_playlist_changed(xmmsc_result_t *result, gpointer data);

static void
xcb_playlist_loaded(xmmsc_result_t *result, gpointer data);

///////////////////////////////////////////////////////////////////////////////
//
// misc utility functions
//
///////////////////////////////////////////////////////////////////////////////

static void
priv_get_ploblist(RemPPPriv *priv, const gchar *xplid, RemStringList *pl)
{
	xmmsc_result_t	*result;
	guint			id;
	gint			ret;
	GString			*pid;

	rem_sl_clear(pl);
	
	result = xmmsc_playlist_list_entries(priv->xc, xplid);
	
	xmmsc_result_wait(result);
	
	if (xmmsc_result_iserror(result)) {
		
		LOG_WARN("%s\n", xmmsc_result_get_error(result));
		xmmsc_result_unref(result);
		return;
	}
	
	pid = g_string_new_len("", 255);
	
	for (xmmsc_result_list_first(result);
		 xmmsc_result_list_valid(result);
		 xmmsc_result_list_next(result))
	{
		ret = xmmsc_result_get_uint(result, &id);
		g_assert(ret);
		
		g_string_printf(pid, "%u", id);
		
		rem_sl_append_const(pl, pid->str);
	}
	
	g_string_free(pid, TRUE);
	
	xmmsc_result_unref(result);
	
}

static void
priv_setup_connection(RemPPPriv *priv)
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
// player proxy callback functions
//
///////////////////////////////////////////////////////////////////////////////

static void
rcb_synchronize(RemPPPriv *priv, RemPlayerStatus *ps)
{
	if (priv->status_changed) {
		
		priv->status_changed = FALSE;
		
		ps->cap_pos = priv->ps->cap_pos;
		ps->repeat = priv->ps->repeat;
		ps->shuffle = priv->ps->shuffle;
		ps->state = priv->ps->state;
		ps->volume = priv->ps->volume;
	}
	
	if (priv->cap_changed) {
		
		priv->cap_changed = FALSE;
		
		g_string_assign(ps->cap_pid, priv->ps->cap_pid->str);
	}
	
	if (priv->playlist_changed) {
		
		priv->playlist_changed = FALSE;

		priv_get_ploblist(priv, XMMS_ACTIVE_PLAYLIST, ps->playlist);
	}
}

static RemLibrary*
rcb_get_library(RemPPPriv *priv);

static RemPlob*
rcb_get_plob(RemPPPriv *priv, const gchar *pid)
{
	guint			u, id;
	gint			ret, val_i;
	gchar			*val_s;
	GString			*val_gs;
	RemPlob			*plob;
	xmmsc_result_t	*result;

	val_gs = g_string_new("123456");
	
	id = (guint) g_ascii_strtoull(pid, NULL, 10);
	
	g_assert(id);
	
	LOG_DEBUG("read song %u from mlib\n", id);
	
	result = xmmsc_medialib_get_info(priv->xc, id);

	xmmsc_result_wait(result);

	if (xmmsc_result_iserror(result)) {
		LOG_WARN("%s\n", xmmsc_result_get_error (result));
		return NULL;
	}
	
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
	
	xmmsc_result_unref(result);
	
	g_string_free(val_gs, TRUE);
	
	return plob;	
}

static RemStringList*
rcb_get_ploblist(RemPPPriv *priv, const gchar *plid)
{
	RemStringList	*pl;
	
	pl = rem_sl_new();
	
	priv_get_ploblist(priv, plid, pl);
	
	return pl;
}

static void
rcb_notify_error(RemPPPriv *priv, GError *err)
{
	if (err) {
		LOG_ERROR("server error: %s\n", err->message);
		g_error_free(err);
	}
	
	g_main_loop_quit(priv->ml);
}

static void
rcb_play_ploblist(RemPPPriv *priv, const gchar *plid);

static RemStringList*
rcb_search(RemPPPriv *priv, const RemPlob *plob);

static void
rcb_simple_control(RemPPPriv *priv, RemSimpleControlCommand cmd, gint param)
{
	gint			ret;
	guint			u;
	xmmsc_result_t	*result;

#define REMX2_WAT_RESULT									\
	xmmsc_result_wait (result);								\
	if (xmmsc_result_iserror (result)) {					\
		LOG_ERROR("%s\n", xmmsc_result_get_error(result));	\
		xmmsc_result_unref(result);							\
		break;												\
	}

	LOG_DEBUG("command: %hu, param: %hu\n", cmd, param);

	switch (cmd) {
		case REM_SCTRL_CMD_JUMP:
			result = xmmsc_playlist_set_next(priv->xc, param);
			REMX2_WAT_RESULT;
			xmmsc_result_unref(result);
			result = xmmsc_playback_tickle(priv->xc);
			REMX2_WAT_RESULT;
			xmmsc_result_unref(result);
			break;
			
		case REM_SCTRL_CMD_NEXT:
			result = xmmsc_playlist_set_next_rel(priv->xc, 1);
			REMX2_WAT_RESULT;
			xmmsc_result_unref(result);
			result = xmmsc_playback_tickle(priv->xc);
			REMX2_WAT_RESULT;
			xmmsc_result_unref(result);
			break;
			
		case REM_SCTRL_CMD_PREV:
			result = xmmsc_playlist_set_next_rel(priv->xc, -1);
			REMX2_WAT_RESULT;
			xmmsc_result_unref(result);
			result = xmmsc_playback_tickle(priv->xc);
			REMX2_WAT_RESULT;
			xmmsc_result_unref(result);
			break;
			
		case REM_SCTRL_CMD_PLAYPAUSE:
			result = xmmsc_playback_status(priv->xc);
			REMX2_WAT_RESULT;
			ret = xmmsc_result_get_uint(result, &u);
			g_assert(ret);
			xmmsc_result_unref(result);
			if (u == XMMS_PLAYBACK_STATUS_PLAY) {
				result = xmmsc_playback_pause(priv->xc);
				REMX2_WAT_RESULT;
				xmmsc_result_unref(result);
			} else {
				result = xmmsc_playback_start(priv->xc);
				REMX2_WAT_RESULT;
				xmmsc_result_unref(result);
//				if (u == XMMS_PLAYBACK_STATUS_PAUSE) {
//					result = xmmsc_playback_tickle(priv->xc);
//					REMX2_WAT_RESULT;
//					xmmsc_result_unref(result);
//				}
			}
			break;
		case REM_SCTRL_CMD_STOP:
			result = xmmsc_playback_stop(priv->xc);
			REMX2_WAT_RESULT;
			xmmsc_result_unref(result);
			break;
			
		case REM_SCTRL_CMD_RESTART:
			result = xmmsc_playback_stop(priv->xc);
			REMX2_WAT_RESULT;
			xmmsc_result_unref(result);
			
			result = xmmsc_playlist_set_next(priv->xc, 1);
			REMX2_WAT_RESULT;
			xmmsc_result_unref(result);
			
			result = xmmsc_playback_start(priv->xc);
			REMX2_WAT_RESULT;
			xmmsc_result_unref(result);
			break;
			
		case REM_SCTRL_CMD_VOLUME:
			result = xmmsc_playback_volume_set(priv->xc, "left", param);
			REMX2_WAT_RESULT;
			xmmsc_result_unref(result);
			result = xmmsc_playback_volume_set(priv->xc, "right", param);
			REMX2_WAT_RESULT;
			xmmsc_result_unref(result);
			break;
			
		case REM_SCTRL_CMD_RATE:
			result = xmmsc_playback_current_id(priv->xc);
			REMX2_WAT_RESULT;
			ret = xmmsc_result_get_uint(result, &u);
			g_assert(ret);
			xmmsc_result_unref(result);
			result = xmmsc_medialib_entry_property_set_int(
									priv->xc, u, XMETA_NAME_RATING, param);
			REMX2_WAT_RESULT;
			xmmsc_result_unref(result);
			break;
			
		default:
			LOG_WARN("ignore command %hu\n", cmd);
			break;
	}
	
}

static void
rcb_update_plob(RemPPPriv *pp_priv, const RemPlob *plob);

static void
rcb_update_ploblist(RemPPPriv *pp_priv,
					const gchar *plid,
					const RemStringList* pids);

///////////////////////////////////////////////////////////////////////////////
//
// xmms2 callback functions
//
///////////////////////////////////////////////////////////////////////////////

static void
xcb_disconnect(gpointer data)
{
	LOG_NOISE("called");
	
	g_main_loop_quit(((RemPPPriv*) data)->ml);
}

static void
xcb_state_changed(xmmsc_result_t *result, gpointer data)
{
	RemPPPriv	*priv = (RemPPPriv*) data;
	guint		st;
	gint		ret;
	
	ret = xmmsc_result_get_uint(result, &st);
	g_assert(ret);

	LOG_DEBUG("new (xmms2) pbs is %u\n", st);

	switch (st) {
		case XMMS_PLAYBACK_STATUS_PAUSE:
			priv->ps->state = REM_PBS_PAUSE;
			break;
		case XMMS_PLAYBACK_STATUS_PLAY:
			priv->ps->state = REM_PBS_PLAY;
			break;
		case XMMS_PLAYBACK_STATUS_STOP:
			priv->ps->state = REM_PBS_STOP;
			break;
		default:
			LOG_BUG("unknown xmms2 playback status\n");
			break;
	}

	priv->status_changed = TRUE;
	
	//rem_server_notify(priv->rs);	
}

static void
xcb_volume_changed(xmmsc_result_t *result, gpointer data)
{
	RemPPPriv	*priv = (RemPPPriv*) data;
	guint		l, r;
	gint		ret;
	
	l = 50; r = 50;
	
	ret = xmmsc_result_get_dict_entry_uint(result, "left", &l);
	ret &= xmmsc_result_get_dict_entry_uint(result, "right", &r);
	g_assert(ret);
	
	priv->ps->volume = l < r ? r : l;

	LOG_DEBUG("new volume is %u:%u\n", l, r);

	priv->status_changed = TRUE;

	//rem_server_notify(priv->rs);
}

static void
xcb_cap_changed(xmmsc_result_t *result, gpointer data)
{
	RemPPPriv	*priv = (RemPPPriv*) data;
	
	guint		id;
	gint		ret;
	
	ret = xmmsc_result_get_uint(result, &id);
	g_assert(ret);
	
	LOG_DEBUG("new cap_pid is %u\n", id);
	
	if (id)
		g_string_printf(priv->ps->cap_pid, "%u", id);
	else
		g_string_truncate(priv->ps->cap_pid, 0);
	
	priv->cap_changed = TRUE;

	//rem_server_notify(priv->rs);
}

static void
xcb_cap_plpos_changed(xmmsc_result_t *result, gpointer data)
{
	RemPPPriv	*priv = (RemPPPriv*) data;
	guint		pos;
	gint		ret;
	
	ret = xmmsc_result_get_uint(result, &pos);
	g_assert(ret);
	
	LOG_DEBUG("new cap_pos is %u\n", pos);

	priv->ps->cap_pos = (gint) pos; // xmms2 has 1 as first position and 0 for
									// no position .. like remuco
	
	priv->status_changed = TRUE;

	//rem_server_notify(priv->rs);
}

static void
xcb_playlist_changed(xmmsc_result_t *result, gpointer data)
{
	((RemPPPriv*) data)->playlist_changed = TRUE;

	LOG_DEBUG("playlist changed\n");

	//rem_server_notify(priv->rs);
}

static void
xcb_playlist_loaded(xmmsc_result_t *result, gpointer data)
{
	((RemPPPriv*) data)->playlist_changed = TRUE;

	LOG_DEBUG("playlist loaded\n");
	
	//rem_server_notify(priv->rs);
}

///////////////////////////////////////////////////////////////////////////////
//
// main
//
///////////////////////////////////////////////////////////////////////////////

int main(int argc, char **argv) {
	
	RemPPPriv		*priv;
	RemPPCallbacks	*rcb;
	GError			*err;
	
	priv = g_slice_new0(RemPPPriv);
	
	priv_setup_connection(priv);
	
	if (!priv->xc) return 1;
	
	priv->ps = rem_player_status_new();
	
	////////// set callbacks for player changes //////////

	XMMS_CALLBACK_SET(priv->xc, xmmsc_broadcast_playback_current_id,
					  &xcb_cap_changed, priv);
	XMMS_CALLBACK_SET(priv->xc, xmmsc_broadcast_playback_status,
					  &xcb_state_changed, priv);
	XMMS_CALLBACK_SET(priv->xc, xmmsc_broadcast_playback_volume_changed,
					  &xcb_volume_changed, priv);
	XMMS_CALLBACK_SET(priv->xc, xmmsc_broadcast_playlist_current_pos,
					  &xcb_cap_plpos_changed, priv);
	XMMS_CALLBACK_SET(priv->xc, xmmsc_broadcast_playlist_changed,
					  &xcb_playlist_changed, priv);
	XMMS_CALLBACK_SET(priv->xc, xmmsc_broadcast_playlist_loaded,
					  &xcb_playlist_loaded, priv);
	
	////////// set callbacks for remuco server //////////

	rcb = g_slice_new0(RemPPCallbacks);
	
	// TODO rcb->get_library = &rcb_get_library;
	rcb->get_plob = &rcb_get_plob;
	rcb->get_ploblist = &rcb_get_ploblist;
	rcb->notify_error = &rcb_notify_error;
	// TODO rcb->play_ploblist = &rcb_play_ploblist;
	// FUTURE FEATURE rcb->search = &rcb_search;
	rcb->simple_ctrl = &rcb_simple_control;
	rcb->synchronize = &rcb_synchronize;
	// FUTURE FEATURE rcb->update_plob = &rcb_update_plob;
	// FUTURE FEATURE rcb->update_ploblist = &rcb_update_ploblist;

	// TODO desc erstellen
	
	////////// start remuco server //////////

	err = NULL;
	priv->rs = rem_server_up(NULL, rcb, priv, &err);
	
	g_slice_free(RemPPCallbacks, rcb);
	
	if (err) {
		LOG_ERROR("starting server failed: %s\n", err->message);
		g_error_free(err);
	}
	
	if (!priv->rs) {
		rem_player_status_destroy(priv->ps);
		g_slice_free(RemPPPriv, priv);
		return 1;
	}
	
	////////// set up and run main loop //////////
	
	priv->ml = g_main_loop_new (NULL, FALSE);

	xmmsc_mainloop_gmain_init(priv->xc);

	g_main_loop_run(priv->ml);

	////////// .. relax .. //////////

	xmmsc_unref(priv->xc);
	
	g_main_loop_unref(priv->ml);
	
	rem_player_status_destroy(priv->ps);
	
	g_slice_free(RemPPPriv, priv);
	
	LOG_INFO("bye..\n");
	
	return 0;
}
