///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include <glib.h>
#include <glib-object.h>
#include <dbus/dbus-glib-bindings.h>
#include <signal.h>
#include <string.h> // for memset()

#include "server-glue.h"

///////////////////////////////////////////////////////////////////////////////
//
// macros and constants
//
///////////////////////////////////////////////////////////////////////////////

// TODO: adjust player name
#define REM_PLAYER					"Fooc"

#define REM_SERVER_PP_PROTO_VERSION		1

#define REM_SERVER_ERR_INVALID_DATA		"rem_server_invalid_data"
#define REM_SERVER_ERR_VERSION_MISMATCH	"rem_server_version_mismatch"
#define REM_SERVER_ERR_UNKNOWN_PLAYER	"rem_server_unknown_player"

#define REM_PLAYBACK_STOP			0
#define	REM_PLAYBACK_PAUSE			1
#define REM_PLAYBACK_PLAY			2

#define REM_PLOB_META_ALBUM			"album"
#define REM_PLOB_META_ARTIST		"artist"
#define REM_PLOB_META_BITRATE		"bitrate"
#define REM_PLOB_META_COMMENT		"comment"
#define REM_PLOB_META_GENRE			"genre"
#define REM_PLOB_META_LENGTH		"length"	/** duration in seconds */
#define REM_PLOB_META_TITLE			"title"
#define REM_PLOB_META_TRACK			"track"		/** track number */
#define REM_PLOB_META_YEAR			"year"
#define REM_PLOB_META_RATING		"rating"
#define REM_PLOB_META_TAGS			"tags"
#define REM_PLOB_META_TYPE			"type"		/** song, video, etc */
#define REM_PLOB_META_TYPE_AUDIO	"audio"
#define REM_PLOB_META_TYPE_VIDEO	"video"
#define REM_PLOB_META_TYPE_OTHER	"other"

#define REM_CTL_IGNORE				0
#define REM_CTL_PLAYPAUSE			1
#define REM_CTL_STOP				2
#define REM_CTL_NEXT				3
#define REM_CTL_PREV				4
#define REM_CTL_JUMP				5
#define REM_CTL_SEEK_FWD			6
#define REM_CTL_SEEK_BWD			7
#define REM_CTL_VOLUME				8
#define REM_CTL_RATE				9
#define REM_CTL_PLAYNEXT			10
#define REM_CTL_SETTAGS				12
#define REM_CTL_REPEAT				13
#define REM_CTL_SHUFFLE				14

///////////////////////////////////////////////////////////////////////////////
//
// types and structs
//
///////////////////////////////////////////////////////////////////////////////

typedef struct {
	
	guint					playback;
	guint					volume;
	gboolean				repeat;
	gboolean				shuffle;
	gint					position;
	gchar					*plob_id, *plob_img;
	GHashTable				*plob_meta;
	gchar					**playlist_ids, **playlist_names,
							**queue_ids, **queue_names;

	// TODO: extend as necessary
	
} RemPlayerState;

///////////////////////////////////////////////////////////////////////////////
//
// remuco player proxy type
// (this has to be a GObject for using the DBus GLib bindings)
//
///////////////////////////////////////////////////////////////////////////////

typedef struct {
	
	GObject			parent;
	GMainLoop		*ml;
	gboolean		init_ok;
	DBusGConnection	*dbus_conn;	
	DBusGProxy		*dbus_proxy;	// proxy for communication with server
	
	RemPlayerState	state;
	
	// TODO: extend as necessary
	
} RemPP;

typedef struct
{
	GObjectClass parent_class;
	
} RemPPClass;

G_DEFINE_TYPE(RemPP, rem_pp, G_TYPE_OBJECT);

static void
rem_pp_class_init(RemPPClass *class)
{
	// nothing to do
}

///////////////////////////////////////////////////////////////////////////////
//
// DBUS interface - function prototypes
//
///////////////////////////////////////////////////////////////////////////////

static gboolean
rem_pp_control(RemPP *pp,
			   guint control, gint paramI, gchar *paramS,
			   GError **err);

static gboolean
rem_pp_request_plob(RemPP *rpp,
					gchar *id, GHashTable **meta,
					GError **err);

static gboolean
rem_pp_request_ploblist(RemPP *rpp,
						gchar *id,
						gchar ***nested_ids, gchar ***nested_names,
						gchar ***ids, gchar ***names,
						GError **err);

#include "pp-glue.h"

///////////////////////////////////////////////////////////////////////////////
//
// remuco player proxy type - creation and destroy
//
///////////////////////////////////////////////////////////////////////////////

static void
rem_pp_init(RemPP *pp)
{
	// nothing to do
}

/** Release any resources used by a RemPP. */
static void
pp_destroy(RemPP *pp)
{
	if (!pp)
		return;
	
	if (pp->dbus_proxy)
		g_object_unref(pp->dbus_proxy);
		
	if (pp->dbus_conn)
		dbus_g_connection_unref(pp->dbus_conn);
	
	if (pp->ml)
		g_main_loop_unref(pp->ml);

	g_free(pp->state.plob_id);
	g_free(pp->state.plob_img);
	
	if (pp->state.plob_meta)
		g_hash_table_destroy(pp->state.plob_meta);
	
	g_free(pp->state.playlist_ids);
	g_free(pp->state.playlist_names);
	g_free(pp->state.queue_ids);
	g_free(pp->state.queue_names);
	
	// TODO: extend as necessary with more clean up

	g_object_unref(pp);
}

/** Create a new, initialized RemPP. */
static RemPP*
pp_new()
{
	RemPP			*pp;
	GError			*err;
	DBusGProxy		*proxy;
	guint			ret;
	
	g_type_init();
	
	pp = g_object_new(rem_pp_get_type(), NULL);
	
	////////// connect to dbus //////////
	
	err = NULL;
	pp->dbus_conn = dbus_g_bus_get(DBUS_BUS_SESSION, &err);
	if (err) {
		g_critical("failed to connect to dbus (%s)", err->message);
		g_error_free(err);
		return NULL;
	}
	
	////////// get a dbus proxy to talk with the server //////////
	
	pp->dbus_proxy = dbus_g_proxy_new_for_name(pp->dbus_conn,
											   "net.sf.remuco.Server",
											   "/net/sf/remuco/Server",
											   "net.sf.remuco.Server");
	
	////////// check server-pp protocol version and our name //////////
	
	err = NULL;
	rem_server_check(pp->dbus_proxy, REM_SERVER_PP_PROTO_VERSION, &err);
	
	if (err) {
		g_critical("server check failed (%s)", err->message);
		g_error_free(err);
		return NULL;
	}
	
	////////// register dbus service //////////
	
	// install introspection info for 'RemPP'
	dbus_g_object_type_install_info(rem_pp_get_type(),
									&dbus_glib_rem_pp_object_info);
	
	// register object at path
	dbus_g_connection_register_g_object(pp->dbus_conn, "/net/sf/remuco/"
										REM_PLAYER, G_OBJECT(pp));
	
	// get a temporary proxy for service name request
	proxy = dbus_g_proxy_new_for_name(pp->dbus_conn,
									  DBUS_SERVICE_DBUS,
									  DBUS_PATH_DBUS,
									  DBUS_INTERFACE_DBUS);

	// reuest service name
	err = NULL;
	org_freedesktop_DBus_request_name(proxy, "net.sf.remuco." REM_PLAYER, 0,
									  &ret, &err);
	if (err) {
		g_critical("failed to request dbus service name (%s)", err->message);
		g_error_free(err);
		g_object_unref(proxy);
		return NULL;
	}
	
	// release tmeporary proxy
	g_object_unref(proxy);
	
	////////// get main loop //////////
	
	pp->ml = g_main_loop_new(NULL, FALSE);
	
	////////// other initialization //////////
	
	// TODO: extend as necessary with more initializations
	
	return pp;
}

///////////////////////////////////////////////////////////////////////////////
//
// private functions
//
///////////////////////////////////////////////////////////////////////////////

/** Called periodically to sync player information with the server. */
static gboolean
sync(RemPP *pp)
{
	GError		*err;
	gboolean	ok;
	
	////////// get up-to-date state information from the player //////////
	
	// TODO: get new player state information, i.e. update 'pp->state'
	
	// stupid example:
	
	// release old data
	g_free(pp->state.plob_id);
	if (pp->state.plob_meta)
		g_hash_table_destroy(pp->state.plob_meta);
	
	// set new data
	pp->state.plob_id = g_strdup("ID123");
	pp->state.plob_meta = g_hash_table_new_full(&g_str_hash, &g_str_equal,
												NULL, &g_free);
	g_hash_table_insert(pp->state.plob_meta, REM_PLOB_META_ARTIST,
						g_strdup("Stereo Total"));
	g_hash_table_insert(pp->state.plob_meta, REM_PLOB_META_TITLE,
						g_strdup("Discjockey"));
	g_hash_table_insert(pp->state.plob_meta, REM_PLOB_META_ALBUM,
						g_strdup("My Melody"));
	
	
	////////// sync player state information with server //////////
	
	err = NULL;
	ok = rem_server_update_state(pp->dbus_proxy, REM_PLAYER,
								 pp->state.playback, pp->state.volume,
								 pp->state.repeat, pp->state.shuffle,
								 pp->state.position,
								 &err);
	
	if (!ok) {
		g_critical("failed to update state at server (%s)", err->message);
		g_error_free(err);
		g_main_loop_quit(pp->ml);
		return FALSE;
	}
	
	err = NULL;
	ok = rem_server_update_plob(pp->dbus_proxy, REM_PLAYER,
								 pp->state.plob_id, pp->state.plob_img,
								 pp->state.plob_meta,
								 &err);
	
	if (!ok) {
		g_critical("failed to update plob at server (%s)", err->message);
		g_error_free(err);
		g_main_loop_quit(pp->ml);
		return FALSE;
	}
	
	err = NULL;
	ok = rem_server_update_playlist(pp->dbus_proxy, REM_PLAYER,
									(const gchar**) pp->state.playlist_ids,
									(const gchar**) pp->state.playlist_names,
									&err);
	
	if (!ok) {
		g_critical("failed to update playlist at server (%s)", err->message);
		g_error_free(err);
		g_main_loop_quit(pp->ml);
		return FALSE;
	}
	
	err = NULL;
	ok = rem_server_update_playlist(pp->dbus_proxy, REM_PLAYER,
									(const gchar**) pp->state.queue_ids,
									(const gchar**) pp->state.queue_names,
									&err);
	
	if (!ok) {
		g_critical("failed to update queue at server (%s)", err->message);
		g_error_free(err);
		g_main_loop_quit(pp->ml);
		return FALSE;
	}
	
	return TRUE;
}

///////////////////////////////////////////////////////////////////////////////
//
// public functions - player interface (from dbus)
//
///////////////////////////////////////////////////////////////////////////////

gboolean
rem_pp_control(RemPP *pp,
			   guint control, gint paramI, gchar *paramS,
			   GError **err)
{
	g_debug("called (%u, %i, '%s')", control, paramI, paramS);
	
	// TODO: control player as specified by 'control', 'paramI' and 'paramS' 
	
	return TRUE;
}

gboolean
rem_pp_request_plob(RemPP *rpp,
					gchar *id,
					GHashTable **meta,
					GError **err)
{
	g_debug("called (%s)", id);

	// TODO: get meta information about the plob (song/video/..) with the ID 'id'
	// from the player and put it as key-value-pairs into 'meta'
	
	// example:
	
	*meta = g_hash_table_new_full(&g_str_hash, &g_str_equal, NULL, &g_free);
	
	g_hash_table_insert(*meta, REM_PLOB_META_ARTIST, g_strdup("Daft Punk"));
	g_hash_table_insert(*meta, REM_PLOB_META_TITLE, g_strdup("Da Funk"));
	g_hash_table_insert(*meta, REM_PLOB_META_ALBUM, g_strdup("Homework"));
	
	return TRUE;
}

gboolean
rem_pp_request_ploblist(RemPP *rpp,
						gchar *id,
						gchar ***nested_ids, gchar ***nested_names,
						gchar ***ids, gchar ***names,
						GError **err) 
{
	guint	len, u;
	
	g_debug("called (%s)", id);

	// TODO: get the content of the ploblist with ID 'id' from the player
	// put IDs of nested ploblists into 'nested_ids'
	// put names of nested ploblists into 'nested_names'
	// put IDs of contained plobs into 'ids'
	// put names of contained plobs into 'names'
	
	// example:
	
	len = 3;
	*nested_ids = g_new0(gchar*, len +1);
	*nested_names = g_new0(gchar*, len +1);
	for (u = 0; u < len; u++) {
		(*nested_ids)[u] = g_strdup_printf("list-%u", u);
		(*nested_names)[u] = g_strdup_printf("Nested List %u", u);
	}
	
	len = 3;
	*ids = g_new0(gchar*, len + 1);
	*names = g_new0(gchar*, len + 1);
	
	for (u = 0; u < len; u++) {
		(*ids)[u] = g_strdup_printf("plob-%u", u);
		(*names)[u] = g_strdup_printf("Plob %u", u);
	}
	
	return TRUE;
}

///////////////////////////////////////////////////////////////////////////////
//
// main
//
///////////////////////////////////////////////////////////////////////////////

static RemPP	*pp = NULL;

static void
sighandler(gint sig)
{
	g_message("received signal %s", g_strsignal(sig));
	if (pp && pp->ml)
		g_main_loop_quit(pp->ml);
}


int main(int argc, char **argv) {
	
	struct sigaction	siga;
	gboolean	ok;
	GError		*err;
	
	////////// log init //////////

	
	
	////////// signal handling //////////
	
	memset(&siga, 0, sizeof(struct sigaction));
	siga.sa_handler = &sighandler;

	ok = sigaction(SIGINT, &siga, NULL) == 0;
	ok &= sigaction(SIGTERM, &siga, NULL) == 0;
	ok &= sigaction(SIGUSR1, &siga, NULL) == 0;
	ok &= sigaction(SIGUSR2, &siga, NULL) == 0;
	
	if (!ok) {
		g_critical("failed to set up signal handler");
		return 1;
	}	
	
	////////// pp init //////////
	
	pp = pp_new();
	if (!pp)
		return 1;
	
	////////// say hello to server //////////
	
	err = NULL;
	rem_server_hello(pp->dbus_proxy, REM_PLAYER, 0, 0, &err);
	if (err) {
		g_critical("failed to say hello to server (%s)", err->message);
		g_error_free(err);
		pp_destroy(pp);
		return 1;
	}

	////////// run the main loop //////////
	
	g_timeout_add(5000, (GSourceFunc) &sync, pp);

	g_message("up and running");
	
	g_main_loop_run(pp->ml);
	
	g_message("going down");

	////////// clean up //////////
	
	if (pp && pp->dbus_proxy)
		rem_server_bye(pp->dbus_proxy, REM_PLAYER, NULL);

	pp_destroy(pp);
	
	return 0;
}

