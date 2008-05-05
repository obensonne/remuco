///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include <signal.h>	// for sigaction() etc.

#include "common.h"

#include "config.h"

#include "net.h"
#include "serial.h"
#include "dbus.h"
#include "dbus-pp-glue.h"
#include "util.h"
#include "img.h"

///////////////////////////////////////////////////////////////////////////////
//
// macros and constants
//
///////////////////////////////////////////////////////////////////////////////

#define G_IO_INERRHUPNVAL	(G_IO_IN | G_IO_ERR | G_IO_HUP | G_IO_NVAL)

#define REM_IMG_NONE				"none"

////////// message ids ///////////

#define REM_MSG_ID_IGNORE			0
#define REM_MSG_ID_SYN_PLIST		1
#define REM_MSG_ID_IFS_PINFO		2
#define REM_MSG_ID_SYN_STATE		3
#define REM_MSG_ID_SYN_PLOB			4	/** currently active plob */
#define REM_MSG_ID_SYN_PLAYLIST		5
#define REM_MSG_ID_SYN_QUEUE		6
#define REM_MSG_ID_IFS_SRVDOWN		7
#define REM_MSG_ID_IFC_CINFO		8
#define REM_MSG_ID_SEL_PLAYER		9
#define REM_MSG_ID_CTL				10
#define REM_MSG_ID_REQ_PLOB			11
#define REM_MSG_ID_REQ_PLOBLIST		12

////////// plob meta information accessed by the server ///////////

#define REM_PLOB_META_ID			"__id__"

////////// well known ploblist IDs and names //////////

#define REM_PLAYLIST_ID				"__PLAYLIST__"
#define REM_PLAYLIST_NAME			"Playlist"
#define REM_QUEUE_ID				"__QUEUE__"
#define REM_QUEUE_NAME				"Queue"

////////// shutdown command code //////////

#define REM_CTL_IGNORE				0
#define REM_CTL_SHUTDOWN			0x0100

////////// dbus related constants //////////

/** The Remuco DBUS interface version. */
#define REM_DBUS_IF_VERSION				1

#define REM_SERVER_ERR_DOMAIN			g_quark_from_string("rem_server_error")

#define REM_SERVER_ERR_INVALID_DATA		"rem_server_invalid_data"
#define REM_SERVER_ERR_VERSION_MISMATCH	"rem_server_version_mismatch"
#define REM_SERVER_ERR_UNKNOWN_PLAYER	"rem_server_unknown_player"

///////////////////////////////////////////////////////////////////////////////
//
// types and structs
//
///////////////////////////////////////////////////////////////////////////////

typedef struct {
	
	RemNetConfig	net;
	gchar			*cmd_shutdown;
	gchar			*img;
	guint			list_limit;
	
} RemServerConfig;

typedef struct _RemServer RemServer;
typedef struct _RemPP RemPP;
typedef struct _RemClient RemClient;

struct _RemClient {
	
	RemNetClient	*net;
	
	guint			screen_w, screen_h;
	gchar			*encoding;
	
	RemPP			*pp;		// the pp a client is connected to
	
	RemServer		*server;	// back reference for access in callbacks
	
};

typedef enum {
	
	REM_STATE_STARTING = 0,
	REM_STATE_RUNNING,
	REM_STATE_SHUTTING_DOWN
	
} RemState;

typedef struct {
	
	guint					playback;
	guint					volume;
	gboolean				repeat;
	gboolean				shuffle;
	gint					position;
	gboolean				queue;
	gchar					*plob_id, **plob_meta, *plob_img;
	gchar					**playlist_ids, **playlist_names;
	gchar					**queue_ids, **queue_names;
	
	gboolean				triggered_sync_state, triggered_sync_playlist,
							triggered_sync_queue, triggered_sync_plob;
	
} RemPlayerState;

typedef struct {
	
	gchar		*name;
	guint		flags;
	guint		rating;
	
} RemPlayerDesc;

struct _RemPP {
	
	DBusGProxy		*dbus;
	RemPlayerState	state;
	GHashTable		*ploblists;
	gboolean		client_sync_triggered;
	
	RemPlayerDesc	*desc;
	GSList			*clients;
	
	RemServer		*server;	// back reference for access in callbacks
	
};

typedef struct {
	
	RemServer	*server;
	gchar		*player;
	GIOChannel	*client_chan;
	gchar		*id;
	
} RemPPRequest;

#define rem_pp_request_new() g_slice_new0(RemPPRequest)

#define rem_pp_request_destroy(_ppr) G_STMT_START {	\
	if (_ppr) {										\
		g_free(_ppr->player);						\
		g_free(_ppr->id);							\
		g_slice_free(RemPPRequest, _ppr);			\
		_ppr = NULL;								\
	}												\
} G_STMT_END

///////////////////////////////////////////////////////////////////////////////
//
// server type
//
///////////////////////////////////////////////////////////////////////////////

struct _RemServer {

	GObject			parent;
	
	DBusGConnection	*bus;
	
	GMainLoop		*ml;
	RemState		state;
	GHashTable		*sht, *cht, *pht;
	gchar			**plist;
	gboolean		plist_sync_triggered;
	
	RemServerConfig	*config;
	
	RemImg			*ri;
	RemNetMsg		msg;
	
};

typedef struct
{
	GObjectClass parent_class;
	
} RemServerClass;

G_DEFINE_TYPE(RemServer, rem_server, G_TYPE_OBJECT);

static void
rem_server_class_init(RemServerClass *class) {
	// nothing to do
}

static void
rem_server_init(RemServer *server) {
}

///////////////////////////////////////////////////////////////////////////////
//
// DBUS interface - function prototypes
//
///////////////////////////////////////////////////////////////////////////////

static gboolean
rem_server_check(RemServer *server, guint version, GError **error);

static gboolean
rem_server_hello(RemServer *server, gchar *player,
				 guint flags, guint rating,
				 GError **error);

static gboolean
rem_server_update_state(RemServer *server, gchar *player,
						guint playback, guint volume, gboolean repeat,
						gboolean shuffle, guint position, gboolean queue,
						GError **err);

static gboolean
rem_server_update_plob(RemServer *server, gchar *player,
					   gchar *id, gchar *img, GHashTable *meta,
					   GError **err);

static gboolean
rem_server_update_playlist(RemServer *server, gchar *player,
						   gchar **ids, gchar **names,
						   GError **err);

static gboolean
rem_server_update_queue(RemServer *server, gchar *player,
						gchar **ids, gchar **names,
						GError **err);

static gboolean
rem_server_bye(RemServer *server, gchar *player,
			   GError **err);


#include "dbus-server-glue.h"

///////////////////////////////////////////////////////////////////////////////
//
// private functions - shutdown procedure
//
///////////////////////////////////////////////////////////////////////////////

static gboolean
shutdown_finish(RemServer *server)
{
	rem_img_down(server->ri);
	
	g_hash_table_destroy(server->cht);

	g_free(server->plist);
	
	g_byte_array_free(server->msg.ba, TRUE);
	
	g_main_loop_quit(server->ml);
	
	return FALSE;
}

static gboolean
shutdown_cleanup(RemServer *server)
{
	GList		*l, *el;
	RemClient	*client;
	gboolean	ok;
	
	g_hash_table_destroy(server->sht);
	g_hash_table_destroy(server->pht);
	
	////////// say bye to the clients //////////
	
	server->msg.id = REM_MSG_ID_IFS_SRVDOWN;
	server->msg.ba->len = 0;

	l = g_hash_table_get_values(server->cht);
	
	for (el = l; el; el = el->next) {
		
		client = (RemClient*) el->data;
		
		ok = rem_net_tx(client->net, &server->msg);
	}
	
	////////// do the rest //////////
	
	if (l)
		// wait a bit until real shutdown, to ensure clients receive bye message
		g_timeout_add(1000, (GSourceFunc) &shutdown_finish, server);
	else
		shutdown_finish(server);

	g_list_free(l);
	
	
	return FALSE;
}

/** Initiates shutdown (real shutdown happens in main loop callback funcs). */
static void
shutdown(RemServer *server)
{
	if (server->state == REM_STATE_SHUTTING_DOWN)
		return;
	
	g_assert(server->state == REM_STATE_RUNNING); // don't call me when starting
	
	server->state = REM_STATE_SHUTTING_DOWN;
	
	g_idle_add_full(G_PRIORITY_HIGH, (GSourceFunc) &shutdown_cleanup,
					server, NULL);
}

static void
shutdown_sys(RemServer *server)
{
	GError		*err;
	gchar		*stdout, *stderr;
	gint		exit;
	
	if (server->state == REM_STATE_SHUTTING_DOWN)
		return;

	if (!server->config->cmd_shutdown) {
		LOG_INFO("system shutdown disabled");
		return;
	}
	
	//shutdown(server);

	LOG_INFO("system shutdown: '%s'", server->config->cmd_shutdown);
	
	err = NULL; stdout = NULL; stderr = NULL; exit = 0;
	g_spawn_command_line_sync(server->config->cmd_shutdown, &stdout, &stderr,
							  &exit, &err);
	
	if (err) {
		LOG_ERROR_GERR(err, "failed to execute shutdown command");
		exit = 0;
	}
	
	if (exit) {
		LOG_WARN("shutdown command failed:\n%s%s", stdout, stderr);
		LOG_WARN("maybe the remuco process is not allowed to shut down");
	}
	
	g_free(stdout);
	g_free(stderr);
}

///////////////////////////////////////////////////////////////////////////////
//
// private functions - pp replys
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Analyze and handle a DBus message call/reply error (if there is one).
 * 
 * @return
 * 		@p TRUE if no error has occured (success), @p FALSE if there was an
 * 		error
 */
static gboolean
pp_reply_handle_error(GError *err, RemPPRequest *req)
{
	if (err && err->domain == DBUS_GERROR) {
		
		if (err->code == DBUS_GERROR_NO_REPLY) {
			LOG_WARN_GERR(err, "no reply from pp %s, probably it is busy",
						  req->player);
		} else if (err->code == DBUS_GERROR_SERVICE_UNKNOWN) {
			LOG_WARN_GERR(err, "pp %s is down, but did not say bye",
						  req->player);
			g_hash_table_remove(req->server->pht, req->player);
		} else {
			LOG_ERROR_GERR(err, "failed to talk to pp %s", req->player);
			g_hash_table_remove(req->server->pht, req->player);
		}
		
		return FALSE;
	
	} else if (err) {
		
		LOG_ERROR_GERR(err, "error in reply from pp %s", req->player);
		return FALSE;
		
	} else {
		
		return TRUE;
	}
}

static void
pp_reply_control(DBusGProxy *proxy, GError *err, RemPPRequest *req)
{
	pp_reply_handle_error(err, req);
	
	rem_pp_request_destroy(req);
}

static void
pp_reply_request_plob(DBusGProxy *proxy, GHashTable *meta, GError *err,
					  RemPPRequest *req)
{
	RemClient	*client;
	RemPP		*pp;
	gchar		**as;
	gboolean	ok;

	////////// error handling //////////
	
	ok = pp_reply_handle_error(err, req);

	if (!ok) {
		rem_pp_request_destroy(req);
		// freeing 'meta' would cause segfault
		return;
	}

	if (req->server->state == REM_STATE_SHUTTING_DOWN) {
		LOG_DEBUG("shutting down -> ignore");
		if (meta) g_hash_table_destroy(meta);
		return;
	}
	
	if (!req->client_chan) { // this was just a ping
		LOG_INFO("pong from %s", req->player);
		if (meta) g_hash_table_destroy(meta);
		rem_pp_request_destroy(req);
		return;
	}
	
	////////// check if client and pp are still present //////////
	
	client = g_hash_table_lookup(req->server->cht, req->client_chan);
	pp = g_hash_table_lookup(req->server->pht, req->player);
	
	// theoretically, client or pp may be away now or client may have another pp
	if (!client || !pp || client->pp != pp) {
		LOG_DEBUG("discard reply (%p, %p)", client, pp);
		if (meta) g_hash_table_destroy(meta);
		rem_pp_request_destroy(req);
		return;
	}

	////////// serialize plob//////////

	if (meta) {
		g_hash_table_insert(meta, g_strdup(REM_PLOB_META_ID), req->id);
		req->id = NULL; // prevent a free by rem_pp_request_destroy()
		as = rem_util_ht2sv(meta, FALSE);	// flat copy
	} else {
		as = NULL;
	}
	
	client->server->msg.id = REM_MSG_ID_REQ_PLOB;
	rem_serial_out(req->server->msg.ba, client->encoding, "as ay", as, NULL);
	
	g_free(as);	// 'as' is just a flat copy of 'meta'
	if (meta) g_hash_table_destroy(meta);	// also frees original 'req->id'
	
	////////// send plob to client //////////
	
	ok = rem_net_tx(client->net, &req->server->msg);
	if (!ok)
		g_hash_table_remove(req->server->cht, req->client_chan);

	rem_pp_request_destroy(req);
}

static void
pp_reply_request_ploblist(DBusGProxy *proxy, gchar **nested_ids,
						  gchar **nested_names, gchar **ids, gchar **names,
						  GError *err, RemPPRequest *req)
{
	RemClient	*client;
	RemPP		*pp;
	gboolean	ok;
	gchar		*name;	// ploblist name
	guint		u, list_limit;

	////////// error handling //////////
	
	ok = pp_reply_handle_error(err, req);

	if (!ok) {
		rem_pp_request_destroy(req);
		// freeing arg data would cause segfault
		return;
	}

	if (req->server->state == REM_STATE_SHUTTING_DOWN) {
		LOG_DEBUG("shutting down -> ignore");
		return;
	}
	
	////////// check if client and pp are still present //////////
	
	client = g_hash_table_lookup(req->server->cht, req->client_chan);
	pp = g_hash_table_lookup(req->server->pht, req->player);
	
	// theoretically, client or pp may be away now or client may have another pp
	if (!client || !pp || client->pp != pp) {
		LOG_DEBUG("discard reply (%p, %p)", client, pp);
		g_strfreev(nested_ids);
		g_strfreev(nested_names);
		g_strfreev(ids);
		g_strfreev(names);
		rem_pp_request_destroy(req);
		return;
	}
	
	////////// check data //////////
	
	if (!rem_util_strv_equal_length(nested_ids, nested_names) ||
		!rem_util_strv_equal_length(ids, names))	{
		
		LOG_WARN("pp %s send malformed data (%s), this looks like a pp bug",
				 req->player, "ids and names of entries or nested lists in "
				 "ploblist differ in length");
		g_hash_table_remove(req->server->pht, req->player);
	}
	
	////////// serialize ploblist //////////

	if (req->id && strlen(req->id)) {
		name = (gchar*) g_hash_table_lookup(pp->ploblists, req->id);
		name = name ? name : "???NAME???";
	} else { // root playlist -> library
		name = "Library";
	}
	
	list_limit = req->server->config->list_limit;
	
	nested_ids = rem_util_strv_trunc(nested_ids, list_limit, FALSE);
	nested_names = rem_util_strv_trunc(nested_names, list_limit,  FALSE);
	ids = rem_util_strv_trunc(ids, list_limit,  FALSE);
	names = rem_util_strv_trunc(names, list_limit, FALSE);
	
	req->server->msg.id = REM_MSG_ID_REQ_PLOBLIST;
	rem_serial_out(req->server->msg.ba, client->encoding,
				   "s s as as as as", req->id, name,
				   nested_ids, nested_names, ids, names);
	
	////////// remember plid <-> name mapping //////////
	
	for (u = 0; nested_ids[u]; u++)
		g_hash_table_insert(pp->ploblists, nested_ids[u], nested_names[u]);
	
	// don't free name (comes from ht), don't free id (happens in req-destroy)
	g_free(nested_ids);		// keep strings, which are in ht now
	g_free(nested_names);	// keep strings, which are in ht now
	g_strfreev(ids);
	g_strfreev(names);

	////////// send ploblist //////////
	
	ok = rem_net_tx(client->net, &req->server->msg);
	if (!ok)
		g_hash_table_remove(req->server->cht, req->client_chan);

	rem_pp_request_destroy(req);
}

///////////////////////////////////////////////////////////////////////////////
//
// private functions - client interaction
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Send the current playlist or queue to a client.
 * Client automatically gets removed on error.
 */
static gboolean
client_send_playlist(RemClient *client, gboolean send_queue)
{
	gchar		*pl_id, *pl_name, **ids, **names;
	gboolean	ok;
	
	if (send_queue) {
		client->server->msg.id = REM_MSG_ID_SYN_QUEUE;
		pl_id = REM_QUEUE_ID;
		pl_name = REM_QUEUE_NAME;
		ids = client->pp->state.queue_ids;
		names = client->pp->state.queue_names;
	} else {
		client->server->msg.id = REM_MSG_ID_SYN_PLAYLIST;
		pl_id = REM_PLAYLIST_ID;
		pl_name = REM_PLAYLIST_NAME;
		ids = client->pp->state.playlist_ids;
		names = client->pp->state.playlist_names;		
	}
	
	rem_serial_out(client->server->msg.ba, client->encoding,
				   "s s as as as as", pl_id, pl_name, NULL, NULL, ids, names);
	
	ok = rem_net_tx(client->net, &client->server->msg);
	if (!ok)
		g_hash_table_remove(client->server->cht, client->net->chan);
	
	return ok;
}

/**
 * Idle callback function. Gets called whenever a player gets added/removed
 * to the player hash table.
 * Clients automatically get removed on error.
 */
static gboolean
client_sync_plist(RemServer *server)
{
	GList		*l, *el;
	RemClient	*client;
	gboolean	ok;
	
	LOG_INFO("sync player list with clients");
	
	g_free(server->plist);			// strings are borrowed from pp descriptors
	
	server->plist = rem_util_htk2sv(server->pht, FALSE);	// _flat_ copy

	rem_util_dump_sv("new player list", server->plist);
	
	server->msg.id = REM_MSG_ID_SYN_PLIST;

	l = g_hash_table_get_values(server->cht);
	
	el = l;
	while(el) {
		
		client = (RemClient*) el->data;
		el = el->next;
		
		if (!client->encoding)	// skip clients which did not send client info
			continue;
		
		rem_serial_out(server->msg.ba, client->encoding, "as", server->plist);
		
		ok = rem_net_tx(client->net, &server->msg);
		if (!ok )
			g_hash_table_remove(server->cht, client->net->chan);
	}
	
	g_list_free(l);
	
	server->plist_sync_triggered = FALSE;
	
	return FALSE;
}


static gboolean
client_sync_state(RemPP *pp)
{
	GSList		*l;
	RemClient	*client;
	gboolean	ok;
	
	pp->state.triggered_sync_state = FALSE;
	
	pp->server->msg.id = REM_MSG_ID_SYN_STATE;
	
	l = pp->clients;
	while (l) {

		client = (RemClient*) l->data;
		l = l->next;
		
		g_assert(client->encoding);

		rem_serial_out(pp->server->msg.ba, client->encoding, "i i b b i b",
					   pp->state.playback, pp->state.volume,
					   pp->state.repeat, pp->state.shuffle,
					   pp->state.position, pp->state.queue);
		
		ok = rem_net_tx(client->net, &pp->server->msg);
		
		if (!ok)
			g_hash_table_remove(pp->server->cht, client->net->chan);
	}
	
	return FALSE;
}

static gboolean
client_sync_plob(RemPP *pp)
{
	GSList		*l;
	RemClient	*client;
	gboolean	ok;
	GByteArray	*plob_img_data;
	
	pp->state.triggered_sync_plob = FALSE;
	
	pp->server->msg.id = REM_MSG_ID_SYN_PLOB;

	l = pp->clients;
	while (l) {

		client = (RemClient*) l->data;
		l = l->next;
		
		g_assert(client->encoding);

		if (pp->server->config->img && pp->state.plob_img)
			plob_img_data = rem_img_get(pp->server->ri, pp->state.plob_img,
										client->screen_w, client->screen_h);
		else
			plob_img_data = NULL;
		
		rem_serial_out(pp->server->msg.ba, client->encoding, "as ay",
					   pp->state.plob_meta, plob_img_data);
		
		if (plob_img_data)
			g_byte_array_free(plob_img_data, TRUE);
		
		ok = rem_net_tx(client->net, &pp->server->msg);
		if (!ok)
			g_hash_table_remove(pp->server->cht, client->net->chan);
		
	}
	
	return FALSE;
}

static gboolean
client_sync_playlist(RemPP *pp)
{
	GSList		*l;
	RemClient	*client;
	
	pp->state.triggered_sync_playlist = FALSE;
	
	l = pp->clients;
	while (l) {

		client = (RemClient*) l->data;
		l = l->next; // do this now, because the next may free current 'c'
		
		g_assert(client->encoding); 
		
		client_send_playlist(client, FALSE);
	}
	
	return FALSE;
}

static gboolean
client_sync_queue(RemPP *pp)
{
	GSList		*l;
	RemClient	*client;
	
	pp->state.triggered_sync_queue = FALSE;
	
	l = pp->clients;
	while (l) {

		client = (RemClient*) l->data;
		l = l->next; // do this now, because the next may free current 'c'
		
		g_assert(client->encoding); 
		
		client_send_playlist(client, TRUE);
	}
	
	return FALSE;
}

/** Called by io_client() to handle a client info message. */
static gboolean
client_welcome(RemClient *client)
{
	gboolean	ok;
	
	////////// rx client info //////////
	
	LOG_INFO("welcome client %s", client->net->addr);
	
	if (client->encoding) {
		LOG_WARN("client %s sent info twice", client->net->addr);
		g_hash_table_remove(client->server->cht, client->net->chan);
		return FALSE;
	}
	
	ok = rem_serial_in(client->server->msg.ba, NULL, "i i s",
					   &client->screen_w, &client->screen_h,
					   &client->encoding);

	////////// check data //////////
	
	if (!ok) {
		LOG_WARN("client %s sent malformed data", client->net->addr);
		g_hash_table_remove(client->server->cht, client->net->chan);
		return FALSE;
	}
	
	if (!client->encoding || !client->encoding[0]) {
		LOG_WARN("client %s sent no encoding", client->net->addr);
		g_hash_table_remove(client->server->cht, client->net->chan);
		return FALSE;
	}
	
	if (client->screen_w + client->screen_h > 4000) {
		LOG_WARN("client %s sent bad screen size", client->net->addr);
		g_hash_table_remove(client->server->cht, client->net->chan);
		return FALSE;
	}

	LOG_DEBUG("client encoding: %s", client->encoding);
	
	////////// send player list //////////
	
	LOG_DEBUG("send player list to client %s", client->net->addr);
	
	rem_serial_out(client->server->msg.ba, client->encoding, "as",
				   client->server->plist);
	
	client->server->msg.id = REM_MSG_ID_SYN_PLIST;
	
	ok = rem_net_tx(client->net, &client->server->msg);
	if (!ok ) {
		g_hash_table_remove(client->server->cht, client->net->chan);
		return FALSE;
	}
	
	return TRUE;	
}

/** Called by io_client() to handle a player selection message. */
static gboolean
client_give_player(RemClient *client)
{
	gboolean		ok;
	gchar			*player;
	GByteArray		*plob_img_data;

	player = NULL;
	ok = rem_serial_in(client->server->msg.ba, client->encoding, "s", &player);
	
	if (!ok) {
		LOG_WARN("client %s sent malformed data", client->net->addr);
		rem_serial_reset("s", &player);
		g_hash_table_remove(client->server->cht, client->net->chan);
		return FALSE;
	}
	
	if (client->pp) {	// remove from current pp's client list
		client->pp->clients = g_slist_remove(client->pp->clients, client);
		client->pp = NULL;
	}
	
	client->pp = g_hash_table_lookup(client->server->pht, player);
	if (!client->pp) {
		// player no more present -> ignore (client should get new
		// player list soon, i.e. plist_sync_triggered should be TRUE)
		LOG_WARN("player %s not known", player);
		g_free(player);
		return TRUE;
	}
	
	// add the client to the pp's client list
	client->pp->clients = g_slist_append(client->pp->clients, client);
	
	LOG_INFO("client %s choosed player %s", client->net->addr, player);
	
	g_free(player);
	
	////////// send player info //////////
	
	client->server->msg.id = REM_MSG_ID_IFS_PINFO;
	
	rem_serial_out(client->server->msg.ba, client->encoding, "s i i",
				   client->pp->desc->name, client->pp->desc->flags,
				   client->pp->desc->rating);
	
	ok = rem_net_tx(client->net, &client->server->msg);
	if (!ok) {
		g_hash_table_remove(client->server->cht, client->net->chan);
		return FALSE;
	}
	
	////////// send player state //////////
	
	client->server->msg.id = REM_MSG_ID_SYN_STATE;
	
	rem_serial_out(client->server->msg.ba, client->encoding, "i i b b i b",
				   client->pp->state.playback, client->pp->state.volume,
				   client->pp->state.repeat, client->pp->state.shuffle,
				   client->pp->state.position, client->pp->state.queue);
	
	ok = rem_net_tx(client->net, &client->server->msg);
	if (!ok) {
		g_hash_table_remove(client->server->cht, client->net->chan);
		return FALSE;
	}
	
	////////// send current plob //////////
	
	client->server->msg.id = REM_MSG_ID_SYN_PLOB;
	
	if (client->server->config->img && client->pp->state.plob_img) {
		plob_img_data = rem_img_get(client->server->ri,
									client->pp->state.plob_img,
									client->screen_w, client->screen_h);
	} else {
		plob_img_data = NULL;
	}
	
	rem_serial_out(client->server->msg.ba, client->encoding, "as ay",
				   client->pp->state.plob_meta, plob_img_data);
	
	if (plob_img_data)
		g_byte_array_free(plob_img_data, TRUE);
	
	ok = rem_net_tx(client->net, &client->server->msg);
	if (!ok) {
		g_hash_table_remove(client->server->cht, client->net->chan);
		return FALSE;
	}
	
	////////// send playlist //////////
	
	ok = client_send_playlist(client, FALSE);
	if (!ok)
		return FALSE;
	
	////////// send queue //////////

	ok = client_send_playlist(client, TRUE);
	if (!ok)
		return FALSE;
	
	return TRUE;
	
}

/** Called by io_client() to handle messages from a fully connected client. */
static gboolean
client_handle_message(RemClient *client)
{
	gboolean		ok;
	gchar			*param, *id;
	guint			u;
	gint			i;
	RemPPRequest	*req;

	if (!client->encoding) {
		LOG_WARN("client %s sent invalid message (%u)",
				 client->net->addr, client->server->msg.id);
		g_hash_table_remove(client->server->cht, client->net->chan);
		return FALSE;			
	}
	
	if (!client->pp) {
		LOG_WARN("client %s must choose a player", client->net->addr);
		// ignore, the client should receive the new player list soon
		return TRUE;
	}
	
	switch (client->server->msg.id) {
		case REM_MSG_ID_CTL:
		
			////////// do a player control //////////
			
			param = NULL;
			ok = rem_serial_in(client->server->msg.ba, client->encoding,
							   "i i s", &u, &i, &param);
			if (!ok) {
				LOG_WARN("client %s sent malformed data", client->net->addr);
				rem_serial_reset("i i s", &u, &i, &param);
				g_hash_table_remove(client->server->cht, client->net->chan);
				return FALSE;
			}
			
			if (u == REM_CTL_SHUTDOWN) {
				g_free(param);
				shutdown_sys(client->server);
				return TRUE;
			}
			
			req = rem_pp_request_new();
			req->player = g_strdup(client->pp->desc->name);
			req->server = client->server;
			req->client_chan = client->net->chan;

			rem_pp_control_async(client->pp->dbus, u, i, param,
								 (rem_pp_control_reply) &pp_reply_control, req);
			
			g_free(param);
			
		break;
		case REM_MSG_ID_REQ_PLOB:
		
			id = NULL;
			ok = rem_serial_in(client->server->msg.ba, client->encoding, "s",
							   &id);
			if (!ok) {
				LOG_WARN("client %s sent malformed data", client->net->addr);
				rem_serial_reset("%s", &id);
				g_hash_table_remove(client->server->cht, client->net->chan);
				return FALSE;
			}
			
			////////// request a plob //////////
			
			req = rem_pp_request_new();
			req->player = g_strdup(client->pp->desc->name);
			req->id = id;
			req->server = client->server;
			req->client_chan = client->net->chan;
			
			rem_pp_request_plob_async(client->pp->dbus, id,
									  (rem_pp_request_plob_reply)
									  &pp_reply_request_plob, req);
			
		break;
		case REM_MSG_ID_REQ_PLOBLIST:
		
			id = NULL;
			ok = rem_serial_in(client->server->msg.ba, client->encoding, "s",
							   &id);
			if (!ok) {
				LOG_WARN("client %s sent malformed data", client->net->addr);
				rem_serial_reset("s", &id);
				g_hash_table_remove(client->server->cht, client->net->chan);
				return FALSE;
			}
			
			////////// request a ploblist //////////

			req = rem_pp_request_new();
			req->player = g_strdup(client->pp->desc->name);
			req->id = id;
			req->server = client->server;
			req->client_chan = client->net->chan;
			
			rem_pp_request_ploblist_async(client->pp->dbus, id,
										  (rem_pp_request_ploblist_reply)
										  &pp_reply_request_ploblist, req);

		break;
		default:
			
			////////// invalid message //////////
			
			LOG_WARN("client %s sent invalid message", client->net->addr);
			g_hash_table_remove(client->server->cht, client->net->chan);
			return FALSE;
			
		break;
	}
	
	return TRUE;
	
}

/** Callback for client hashtable to get comletely rid of a client. */
static void
client_bye(RemClient *client)
{
	if (!client)
		return;
	
	LOG_INFO("disconnect client %s", client->net->addr);
	
	g_source_remove_by_user_data(client);

	if (client->net)
		rem_net_bye(client->net);

	if (client->pp)
		client->pp->clients = g_slist_remove(client->pp->clients, client);
	
	g_free(client->encoding);
	
	g_slice_free(RemClient, client);
}

///////////////////////////////////////////////////////////////////////////////
//
// private functions - misc
//
///////////////////////////////////////////////////////////////////////////////

static RemPP*
pp_get(RemServer *server, const gchar *player, GError **err)
{
	RemPP *pp;
	
	pp = g_hash_table_lookup(server->pht, player);
	
	if (!pp) {
		LOG_WARN("pp %s did not say hello before", player);
		g_set_error(err, REM_SERVER_ERR_DOMAIN, 0, REM_SERVER_ERR_UNKNOWN_PLAYER);
	}
	
	return pp;
}

static gboolean
pp_ping(RemServer *server)
{
	GList			*l, *el;
	RemPP			*pp;
	RemPPRequest	*req;
	
	if (server->state == REM_STATE_SHUTTING_DOWN)
		return FALSE;
	
	l = g_hash_table_get_values(server->pht);
	
	if (!l) {
		LOG_INFO("there are no player proxies -> going down");
		shutdown(server);
		return FALSE;
	}
	
	for (el = l; el; el = el->next) {

		pp = (RemPP*) el->data; g_assert(pp);
		
		req = rem_pp_request_new();
		req->player = g_strdup(pp->desc->name);
		req->server = server;
		
		LOG_DEBUG("ping %s", pp->desc->name);
		rem_pp_control_async(pp->dbus, REM_CTL_IGNORE, 0, "",
							 (rem_pp_control_reply) &pp_reply_control, req);
	}
	
	g_list_free(l);
	
	return TRUE;
}

/** Callback for pp hashtable to get comletely rid of a pp. */
static void
pp_bye(RemPP *pp)
{
	GSList		*el;
	RemClient	*client;
	
	if (!pp)
		return;
	
	LOG_INFO("bye %s", pp->desc->name);
	
	//g_object_unref(pp->dbus); // FIXME: not needed
	
	if (pp->desc) {
		g_free(pp->desc->name);
		g_slice_free(RemPlayerDesc, pp->desc);
	}
	g_free(pp->state.plob_id);
	g_free(pp->state.plob_img);
	g_strfreev(pp->state.plob_meta);
	g_strfreev(pp->state.playlist_ids);
	g_strfreev(pp->state.playlist_names);
	g_strfreev(pp->state.queue_ids);
	g_strfreev(pp->state.queue_names);
	
	for (el = pp->clients; el; el = el->next) {
		
		client = (RemClient*) el->data;
		client->pp = NULL;
	}
	
	g_slist_free(pp->clients);
	
	if (pp->ploblists)
		g_hash_table_destroy(pp->ploblists);
	
	g_source_remove_by_user_data(pp);
	
	////////// trigger player list sync with the clients //////////
	
	if (pp->server->state != REM_STATE_SHUTTING_DOWN &&
		!pp->server->plist_sync_triggered) {
	
		pp->server->plist_sync_triggered = TRUE;

		g_idle_add_full(G_PRIORITY_HIGH, (GSourceFunc) &client_sync_plist,
						pp->server, NULL);
	}
	
	g_slice_free(RemPP, pp);
}

///////////////////////////////////////////////////////////////////////////////
//
// private functions - i/o
//
///////////////////////////////////////////////////////////////////////////////

static gboolean
io_client(GIOChannel *chan, GIOCondition cond, RemClient *client)
{
	gboolean		ok;
	
	if (cond == G_IO_IN) { // client has data
	
		LOG_DEBUG("G_IO_IN");

		////////// rx the message //////////
		
		ok = rem_net_rx(client->net, &client->server->msg);
		if (!ok) {
			g_hash_table_remove(client->server->cht, chan);
			return FALSE;
		}
		
		////////// handle a client which just has connected //////////
		
		if (client->server->msg.id == REM_MSG_ID_IFC_CINFO) {
			
			return client_welcome(client);
		}
		
		////////// handle a client which chooses a player //////////
		
		if (client->server->msg.id == REM_MSG_ID_SEL_PLAYER) {
			
			return client_give_player(client);
		}
		
		////////// handle a fully connected client  //////////

		return client_handle_message(client);
		
	} else if (cond & G_IO_HUP) { // probably client disconnected

		LOG_INFO("a client disconnected");
		g_hash_table_remove(client->server->cht, chan);
		return FALSE;

	} else { // some error
		
		LOG_ERROR("a client connection is broken");
		g_hash_table_remove(client->server->cht, chan);
		return FALSE;
	}
	
	return TRUE;
}

/** Callback for server channel io watch. */ 
static gboolean
io_server(GIOChannel *chan, GIOCondition cond, RemServer *server)
{
	RemNetServer	*server_net;
	RemClient		*client;
	RemNetClient	*client_net;
	gboolean		ok;
	
	if (cond == G_IO_IN) { // client requests connection
	
		LOG_DEBUG("G_IO_IN");

		server_net = g_hash_table_lookup(server->sht, chan);
		
		client_net = rem_net_accept(server_net);
		
		if (!client_net) // error log in rem_net_accept()
			return TRUE;
		
		ok = rem_net_hello(client_net);
		if (!ok)
			rem_net_bye(client_net);
		
		client = g_slice_new0(RemClient);
		
		client->net = client_net;

		g_io_add_watch(client_net->chan, G_IO_INERRHUPNVAL,
					   (GIOFunc) &io_client, client);
		
		client->server = server;
		
		g_hash_table_insert(server->cht, client->net->chan, client);
		
		return TRUE;
		
	} else { // some error
		
		LOG_DEBUG("G_IO_HUP|ERR|NVAL|?? (%u)", cond);
		
		LOG_ERROR("error on server socket");
		
		shutdown(server);
		
		return FALSE;
		
	}
}

/** Callback for server hashtable for watching channels in main loop. */ 
static void
io_server_watch(GIOChannel *chan, RemNetServer *server_net, RemServer *server)
{
	server_net->watch_id = g_io_add_watch(chan, G_IO_INERRHUPNVAL,
										  (GIOFunc) &io_server, server);
}

///////////////////////////////////////////////////////////////////////////////
//
// DBUS interface - implementation
//
///////////////////////////////////////////////////////////////////////////////

static gboolean
rem_server_check(RemServer *server, guint version, GError **err)
{
	LOG_INFO("called");
	
	if (version != REM_DBUS_IF_VERSION) {
		g_set_error(err, REM_SERVER_ERR_DOMAIN, 0, REM_SERVER_ERR_VERSION_MISMATCH);
		return FALSE;
	}
	
	return TRUE;
}

static gboolean
rem_server_hello(RemServer *server, gchar* player,
				 guint flags, guint rating,
				 GError **err)
{
	RemPP		*pp;
	
	////////// check if not known already and name is valid //////////
	
	LOG_INFO("player '%s' says hello", player);

	if (server->state == REM_STATE_SHUTTING_DOWN) {
		LOG_DEBUG("shutting down -> ignore");
		return TRUE;
	}
	
	if (g_hash_table_lookup(server->pht, player)) {
		
		LOG_WARN("player '%s' said hello again .. name conflict?", player);
		return TRUE;
	}

	if (!rem_dbus_check_name(player)) {
		LOG_WARN("pp %s send malformed data (%s), this looks like a pp bug",
				 player, "player name is not valid");
		g_set_error(err, REM_SERVER_ERR_DOMAIN, 0, REM_SERVER_ERR_INVALID_DATA);
		return FALSE;
	}
	
	////////// set up the new pp //////////
	
	pp = g_slice_new0(RemPP);
	
	pp->desc = g_slice_new0(RemPlayerDesc);
	pp->desc->name = g_strdup(player);
	pp->desc->flags = flags;
	pp->desc->rating = rating;
	
	pp->dbus = rem_dbus_proxy(server->bus, player);
	
	pp->server = server;

	g_hash_table_insert(server->pht, pp->desc->name, pp);

	////////// init the ploblists plid <-> name map //////////
	
	pp->ploblists = g_hash_table_new_full(&g_str_hash, &g_str_equal,
										  &g_free, &g_free);
	g_hash_table_insert(pp->ploblists,
						g_strdup(REM_PLAYLIST_ID),
						g_strdup(REM_PLAYLIST_NAME));
	g_hash_table_insert(pp->ploblists,
						g_strdup(REM_QUEUE_ID),
						g_strdup(REM_QUEUE_NAME));
	
	////////// trigger a player list sync with clients //////////
	
	if (!server->plist_sync_triggered) {
		
		server->plist_sync_triggered = TRUE;
		
		g_idle_add_full(G_PRIORITY_HIGH, (GSourceFunc) &client_sync_plist,
						server, NULL);
	}
	
	return TRUE;
}

static gboolean
rem_server_update_state(RemServer *server, gchar *player,
						guint playback, guint volume, gboolean repeat,
						gboolean shuffle, guint position, gboolean queue,
						GError **err)
{
	RemPP		*pp;
	gboolean	diff;
	
	LOG_DEBUG("from %s", player);
	
	if (server->state == REM_STATE_SHUTTING_DOWN) {
		LOG_DEBUG("shutting down -> ignore");
		return TRUE;
	}
	
	pp = pp_get(server, player, err);
	if (!pp)
		return FALSE; // err set by check_player()
	
	diff = (pp->state.playback != playback) ||
		   (pp->state.volume != volume) ||
		   (pp->state.repeat != repeat) ||
		   (pp->state.shuffle != shuffle) ||
		   (pp->state.position != position) ||
		   (pp->state.queue != queue);
	
	if (!diff)
		return TRUE;
	
	pp->state.playback = playback;
	pp->state.volume = volume;
	pp->state.repeat = repeat;
	pp->state.shuffle = shuffle;
	pp->state.position = position;
	pp->state.queue = queue;

	if (!pp->state.triggered_sync_state) {
		
		pp->state.triggered_sync_state = TRUE;
		
		g_idle_add((GSourceFunc) &client_sync_state,	pp);
	}

	return TRUE;
}

static gboolean
rem_server_update_plob(RemServer *server, gchar *player,
					   gchar *id, gchar *img, GHashTable *meta,
					   GError **err)
{
	RemPP		*pp;

	LOG_DEBUG("from %s", player);

	if (server->state == REM_STATE_SHUTTING_DOWN) {
		LOG_DEBUG("shutting down -> ignore");
		return TRUE;
	}
	
	pp = pp_get(server, player, err);
	if (!pp)
		return FALSE; // err set by check_player()
	
	if (pp->state.plob_id == id)
		return TRUE;
	
	if (id && pp->state.plob_id && g_str_equal(pp->state.plob_id, id))
		return TRUE;
	
	// release old plob resources
	g_free(pp->state.plob_id);
	pp->state.plob_id = NULL;
	g_free(pp->state.plob_img);
	pp->state.plob_img = NULL;
	g_strfreev(pp->state.plob_meta);
	pp->state.plob_meta = NULL;
	
	LOG_DEBUG("id of current plob: '%s'", id);
	LOG_DEBUG("img of current plob: '%s'", img);
	
	// get new plob resources
	if (id && strlen(id)) {
	
		pp->state.plob_id = g_strdup(id);
		pp->state.plob_img = img && strlen(img) ? g_strdup(img) : NULL;
		
		g_hash_table_insert(meta, g_strdup(REM_PLOB_META_ID), g_strdup(id));
		pp->state.plob_meta = rem_util_ht2sv(meta, TRUE);
		
	}
	
	if (!pp->state.triggered_sync_plob) {
		
		pp->state.triggered_sync_plob = TRUE;
		
		g_idle_add((GSourceFunc) &client_sync_plob,	pp);
	}
	
	return TRUE;
}

static gboolean
rem_server_update_playlist(RemServer *server, gchar *player,
						   gchar **ids, gchar **names,
						   GError **err)
{
	RemPP	*pp;
	
	LOG_DEBUG("from %s", player);

	if (server->state == REM_STATE_SHUTTING_DOWN) {
		LOG_DEBUG("shutting down -> ignore");
		return TRUE;
	}
	
	pp = pp_get(server, player, err);
	if (!pp)
		return FALSE; // err set by pp_get()
	
	// check for valid input
	if (!rem_util_strv_equal_length(ids, names)) {
		
		g_set_error(err, REM_SERVER_ERR_DOMAIN, 0, REM_SERVER_ERR_INVALID_DATA);
		LOG_WARN("pp %s send malformed data (%s), this looks like a pp bug",
				 player, "ids and names of playlist differ in length");
		g_hash_table_remove(pp->server->pht, player);
		
		return FALSE;
	}

	ids = rem_util_strv_trunc(ids, server->config->list_limit, TRUE);
	names = rem_util_strv_trunc(names, server->config->list_limit, TRUE);
	
	if (rem_util_strv_equal(ids, pp->state.playlist_ids)) {
		g_strfreev(ids);
		g_strfreev(names);
		return TRUE;
	}
	
	// release old playlist resources
	g_strfreev(pp->state.playlist_ids);
	pp->state.playlist_ids = NULL;
	g_strfreev(pp->state.playlist_names);
	pp->state.playlist_names = NULL;
	
	g_assert(ids && names);
	
	rem_util_dump_sv("new playlist", ids);
	
	// get new playlist resources
	pp->state.playlist_ids = ids;
	pp->state.playlist_names = names;
	
	if (!pp->state.triggered_sync_playlist) {
		
		pp->state.triggered_sync_playlist = TRUE;
		
		g_idle_add((GSourceFunc) &client_sync_playlist, pp);
	}

	return TRUE;
}

static gboolean
rem_server_update_queue(RemServer *server, gchar *player,
						gchar **ids, gchar **names,
						GError **err)
{
	RemPP	*pp;
	
	LOG_DEBUG("from %s", player);

	if (server->state == REM_STATE_SHUTTING_DOWN) {
		LOG_DEBUG("shutting down -> ignore");
		return TRUE;
	}
	
	pp = pp_get(server, player, err);
	if (!pp)
		return FALSE; // err set by check_player()
	
	if (rem_util_strv_equal(ids, pp->state.queue_ids))
		return TRUE;
	
	// check for valid input
	if (!rem_util_strv_equal_length(ids, names)) {
		
		g_set_error(err, REM_SERVER_ERR_DOMAIN, 0, REM_SERVER_ERR_INVALID_DATA);
		LOG_WARN("pp %s send malformed data (%s), this looks like a pp bug",
				 player, "ids and names of queue differ in length");
		g_hash_table_remove(pp->server->pht, player);
		return FALSE;
	}
	
	// release old queue resources
	g_strfreev(pp->state.queue_ids);
	pp->state.queue_ids = NULL;
	g_strfreev(pp->state.queue_names);
	pp->state.queue_names = NULL;
	
	g_assert(ids && names);
	
	rem_util_dump_sv("new queue", ids);
	
	// get new queue resources
	pp->state.queue_ids = g_strdupv(ids);
	pp->state.queue_names = g_strdupv(names);
	
	if (!pp->state.triggered_sync_queue) {
		
		pp->state.triggered_sync_queue = TRUE;
		
		g_idle_add((GSourceFunc) &client_sync_queue, pp);
	}

	return TRUE;
}

static gboolean
rem_server_bye(RemServer *server, gchar *player,
			   GError **err)
{
	LOG_DEBUG("from %s", player);

	if (server->state == REM_STATE_SHUTTING_DOWN) {
		LOG_DEBUG("shutting down -> ignore");
		return TRUE;
	}
	
	g_hash_table_remove(server->pht, player);
	
	return TRUE;
}

///////////////////////////////////////////////////////////////////////////////
//
// main
//
///////////////////////////////////////////////////////////////////////////////

static RemServer		*global_server; // global refrence for signal handler

static RemServerConfig	global_config;

static const RemConfigEntry	config_desc[] = {
	{ "net", "ip", G_TYPE_BOOLEAN, FALSE, &global_config.net.ip_on },
	{ "net", "ip-port", G_TYPE_INT, FALSE, &global_config.net.ip_port },
	{ "net", "bt", G_TYPE_BOOLEAN, FALSE, &global_config.net.bt_on },
	{ "misc", "shutdown", G_TYPE_STRING, FALSE, &global_config.cmd_shutdown },
	{ "misc", "img", G_TYPE_STRING, FALSE, &global_config.img },
	{ "misc", "list-limit", G_TYPE_INT, FALSE, &global_config.list_limit },
	{ NULL, NULL, G_TYPE_INVALID, FALSE, NULL }
};

static void
sighandler(gint sig)
{
	LOG_INFO("received signal %s", g_strsignal(sig));
	if (global_server && global_server->state == REM_STATE_RUNNING)
		shutdown(global_server);
}

int main(int argc, char **argv) {
	
	RemServer			*server;
	gboolean			ok;
	gboolean			started_by_dbus;
	GError				*err;
	const gchar			*logname;
	struct sigaction	siga;
	
	////////// early initializations //////////
	
	started_by_dbus = (gboolean) g_getenv("DBUS_STARTER_ADDRESS");

	if (started_by_dbus) {
		logname = "Server";
	} else {
		g_print("- - - manual invocation -> log goes to stdout/err - - -\n");
		logname = NULL;
	}
	
	ok = rem_util_create_cache_dir();
	if (!ok)
		return 1;
	
	rem_log_init(logname);
	
	////////// signal handling //////////
	
	memclr(struct sigaction, &siga);
	siga.sa_handler = &sighandler;

	ok = sigaction(SIGINT, &siga, NULL) == 0;
	ok &= sigaction(SIGTERM, &siga, NULL) == 0;
	ok &= sigaction(SIGUSR1, &siga, NULL) == 0;
	ok &= sigaction(SIGUSR2, &siga, NULL) == 0;
	
	if (!ok) {
		LOG_ERROR("failed to set up signal handler");
		return 1;
	}	
	
	////////// set default configuration //////////

	memclr(RemServerConfig, &global_config);
	
	global_config.net.bt_on = TRUE;
	global_config.net.ip_on = TRUE;
	global_config.net.ip_port = 34271;
	global_config.cmd_shutdown = "sudo /sbin/shutdown";
	global_config.img = "jpg";
	global_config.list_limit = 100;
	
	////////// load and check configuration //////////
	
	ok = rem_config_load("Server", "conf", FALSE, config_desc);
	if (!ok)
		return 1;

	if (!global_config.net.bt_on && !global_config.net.ip_on) {
		
		LOG_ERROR("neither Bluetooth nor IP is activated");
		return 1;
	}
	
	if (global_config.net.ip_port >= 65536) {
	
		LOG_ERROR("invalid port number");
		return 1;
	}
	
	if (strlen(global_config.img) == 0 ||
		g_str_equal(global_config.img, REM_IMG_NONE)) {
		
		g_free(global_config.img);
		global_config.img = NULL;
	}
	
	////////// init server type //////////
	
	g_type_init();
	
	server = g_object_new(rem_server_get_type(), NULL);
	
	server->config = &global_config;
	
	global_server = server;
	
	////////// net (bt, ip, ..) //////////
	
	server->sht = rem_net_up(&server->config->net);
	
	if (!server->sht) {
		LOG_ERROR("failed to start net");
		g_object_unref(server);
		return 1;
	}
	
	g_hash_table_foreach(server->sht, (GHFunc) &io_server_watch, server);

	////////// dbus //////////
	
	err = NULL;
	server->bus = rem_dbus_connect(&err);
	if (!server->bus) {
		LOG_ERROR_GERR(err, "failed to connect to dbus");
		g_hash_table_destroy(server->sht);
		g_object_unref(server);
		return 1;
	}
	
	err = NULL;
	rem_dbus_register(server->bus, &dbus_glib_rem_server_object_info,
					  rem_server_get_type(), server, "Server", &err);
	if (err) {
		LOG_ERROR_GERR(err, "failed to register dbus service");
		g_hash_table_destroy(server->sht);
		g_object_unref(server);
		return 1;
	}
	
	////////// misc initializations //////////
	
	server->ml = g_main_loop_new(NULL, FALSE);
	
	server->msg.ba = g_byte_array_sized_new(1024);
	
	server->cht = g_hash_table_new_full(&g_direct_hash, &g_direct_equal, NULL,
										(GDestroyNotify) &client_bye);
	
	server->pht = g_hash_table_new_full(&g_str_hash, &g_str_equal, NULL,
										(GDestroyNotify) &pp_bye);

	if (server->config->img)
		server->ri = rem_img_up(server->config->img);
	
	////////// here we go //////////
	
	// periodically watch pps (shut down if there are no pps)
	g_timeout_add(30000, (GSourceFunc) &pp_ping, server);

	server->state = REM_STATE_RUNNING;
	
	LOG_INFO("here we go ..");
	
	g_main_loop_run(server->ml); // wait for incoming messages
	
	g_main_loop_unref(server->ml);
	
	g_object_unref(server);

	LOG_INFO("bye");
		
	return 0;
}

