///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include "server.h"
#include "common.h"
#include "config.h"
#include "net.h"
#include "serial.h"
#include "bppl.h"
#include "dbus.h"
#include "pp-glue-c.h"
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

///////////////////////////////////////////////////////////////////////////////
//
// configuration
//
///////////////////////////////////////////////////////////////////////////////

typedef struct {
	
	gboolean		bt_on;
	gboolean		ip_on;
	gboolean		ip_port;
	gchar			*cmd_shutdown;
	gchar			*img;
	guint			list_limit;
	gboolean		enable_bpps;
	
} Config;

/** Configuration, initialized with default values. */
static Config config = {
		TRUE,	/* bt_on */
		TRUE,	/* ip_on */
		34271,	/* ip_port */
		NULL,	/* cmd_shutdown */
		"jpg",	/* img */
		100,	/* list_limit */
		TRUE	/* enable_bpps */
};

static const RemConfigEntry	config_entries[] = {
	{ "net", "bt", G_TYPE_BOOLEAN, FALSE, &config.bt_on },
	{ "net", "ip", G_TYPE_BOOLEAN, FALSE, &config.ip_on },
	{ "net", "ip-port", G_TYPE_INT, FALSE, &config.ip_port },
	{ "misc", "sys-shutdown-cmd", G_TYPE_STRING, FALSE, &config.cmd_shutdown },
	{ "misc", "img", G_TYPE_STRING, FALSE, &config.img },
	{ "misc", "list-limit", G_TYPE_INT, FALSE, &config.list_limit },
	{ "misc", "disable-bpps", G_TYPE_BOOLEAN, FALSE, &config.enable_bpps },
	{ NULL, NULL, G_TYPE_INVALID, FALSE, NULL }
};

///////////////////////////////////////////////////////////////////////////////
//
// types and structs
//
///////////////////////////////////////////////////////////////////////////////

typedef struct _Proxy Proxy;
typedef struct _Client RemClient;

struct _Client {
	
	RemNetClient	*net;
	
	guint			screen_w, screen_h;
	gchar			*encoding;
	
	Proxy			*pp;		// the pp a client is connected to
	
	RemServer		*server;	// back reference for access in callbacks
	
};

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
	
} PlayerState;

typedef struct {
	
	gchar		*name;
	guint		flags;
	guint		rating;
	
} PlayerDesc;

struct _Proxy {
	
	DBusGProxy		*dbus_proxy;

	PlayerDesc		*desc;
	PlayerState		state;
	GHashTable		*ploblists;

	gboolean		client_sync_triggered;
	GSList			*clients;
	RemServer		*server;	// back reference for access in callbacks
	
};

typedef struct {
	
	RemServer	*server;
	gchar		*player;
	GIOChannel	*client_chan;
	gchar		*id;
	
} ProxyRequest;

#define proxy_request_new() g_slice_new0(ProxyRequest)

#define proxy_request_destroy(_ppr) G_STMT_START {	\
	if (_ppr) {										\
		g_free(_ppr->player);						\
		g_free(_ppr->id);							\
		g_slice_free(ProxyRequest, _ppr);			\
		_ppr = NULL;								\
	}												\
} G_STMT_END

///////////////////////////////////////////////////////////////////////////////
//
// server type
//
///////////////////////////////////////////////////////////////////////////////

struct _RemServerPriv {

	DBusGConnection			*dbus_conn;
	GMainLoop				*ml;
	gboolean				shutting_down;
	
	RemBasicProxyLauncher	*bppl;
	RemImg					*ri;

	Config					*config;
	GHashTable				*sht, *cht, *pht;
	gchar					**plist;
	gboolean				plist_sync_triggered;
	RemNetMsg				msg;
	
};

G_DEFINE_TYPE(RemServer, rem_server, G_TYPE_OBJECT);

static void
rem_server_class_init(RemServerClass *class) {
}

static void
rem_server_init(RemServer *server) {
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
pp_reply_handle_error(GError *err, ProxyRequest *req)
{
	if (err && err->domain == DBUS_GERROR) {
		
		if (err->code == DBUS_GERROR_NO_REPLY) {
			LOG_WARN_GERR(err, "no reply from pp %s, probably it is busy",
						  req->player);
		} else if (err->code == DBUS_GERROR_SERVICE_UNKNOWN) {
			LOG_WARN_GERR(err, "pp %s is down, but did not say bye",
						  req->player);
			g_hash_table_remove(req->server->priv->pht, req->player);
		} else {
			LOG_ERROR_GERR(err, "failed to talk to pp %s", req->player);
			g_hash_table_remove(req->server->priv->pht, req->player);
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
pp_reply_control(DBusGProxy *dbus_proxy, GError *err, ProxyRequest *req)
{
	pp_reply_handle_error(err, req);
	
	proxy_request_destroy(req);
}

static void
pp_reply_request_plob(DBusGProxy *dbus_proxy, GHashTable *meta, GError *err,
					  ProxyRequest *req)
{
	RemClient	*client;
	Proxy		*pp;
	gchar		**as;
	gboolean	ok;

	////////// error handling //////////
	
	ok = pp_reply_handle_error(err, req);

	if (!ok) {
		proxy_request_destroy(req);
		// freeing 'meta' would cause segfault
		return;
	}

	if (req->server->priv->shutting_down) {
		LOG_DEBUG("shutting down -> ignore");
		if (meta) g_hash_table_destroy(meta);
		return;
	}
	
	if (!req->client_chan) { // this was just a ping
		LOG_INFO("pong from %s", req->player);
		if (meta) g_hash_table_destroy(meta);
		proxy_request_destroy(req);
		return;
	}
	
	////////// check if client and pp are still present //////////
	
	client = g_hash_table_lookup(req->server->priv->cht, req->client_chan);
	pp = g_hash_table_lookup(req->server->priv->pht, req->player);
	
	// theoretically, client or pp may be away now or client may have another pp
	if (!client || !pp || client->pp != pp) {
		LOG_DEBUG("discard reply (%p, %p)", client, pp);
		if (meta) g_hash_table_destroy(meta);
		proxy_request_destroy(req);
		return;
	}

	////////// serialize plob//////////

	if (meta) {
		g_hash_table_insert(meta, g_strdup(REM_PLOB_META_ID), req->id);
		req->id = NULL; // prevent a free by rem_proxy_request_destroy()
		as = rem_util_ht2sv(meta, FALSE);	// flat copy
	} else {
		as = NULL;
	}
	
	client->server->priv->msg.id = REM_MSG_ID_REQ_PLOB;
	rem_serial_out(req->server->priv->msg.ba, client->encoding, "as ay", as,
				   NULL);
	
	g_free(as);	// 'as' is just a flat copy of 'meta'
	if (meta) g_hash_table_destroy(meta);	// also frees original 'req->id'
	
	////////// send plob to client //////////
	
	ok = rem_net_tx(client->net, &req->server->priv->msg);
	if (!ok)
		g_hash_table_remove(req->server->priv->cht, req->client_chan);

	proxy_request_destroy(req);
}

static void
pp_reply_request_ploblist(DBusGProxy *dbus_proxy, gchar **nested_ids,
						  gchar **nested_names, gchar **ids, gchar **names,
						  GError *err, ProxyRequest *req)
{
	RemClient	*client;
	Proxy		*pp;
	gboolean	ok;
	gchar		*name;	// ploblist name
	guint		u, list_limit;

	////////// error handling //////////
	
	ok = pp_reply_handle_error(err, req);

	if (!ok) {
		proxy_request_destroy(req);
		// freeing arg data would cause segfault
		return;
	}

	if (req->server->priv->shutting_down) {
		LOG_DEBUG("shutting down -> ignore");
		return;
	}
	
	////////// check if client and pp are still present //////////
	
	client = g_hash_table_lookup(req->server->priv->cht, req->client_chan);
	pp = g_hash_table_lookup(req->server->priv->pht, req->player);
	
	// theoretically, client or pp may be away now or client may have another pp
	if (!client || !pp || client->pp != pp) {
		LOG_DEBUG("discard reply (%p, %p)", client, pp);
		g_strfreev(nested_ids);
		g_strfreev(nested_names);
		g_strfreev(ids);
		g_strfreev(names);
		proxy_request_destroy(req);
		return;
	}
	
	////////// check data //////////
	
	if (!rem_util_strv_equal_length(nested_ids, nested_names) ||
		!rem_util_strv_equal_length(ids, names))	{
		
		LOG_WARN("pp %s send malformed data (%s), this looks like a pp bug",
				 req->player, "ids and names of entries or nested lists in "
				 "ploblist differ in length");
		g_hash_table_remove(req->server->priv->pht, req->player);
	}
	
	////////// serialize ploblist //////////

	if (req->id && strlen(req->id)) {
		name = (gchar*) g_hash_table_lookup(pp->ploblists, req->id);
		name = name ? name : "???NAME???";
	} else { // root playlist -> library
		name = "Library";
	}
	
	list_limit = req->server->priv->config->list_limit;
	
	nested_ids = rem_util_strv_trunc(nested_ids, list_limit, FALSE);
	nested_names = rem_util_strv_trunc(nested_names, list_limit,  FALSE);
	ids = rem_util_strv_trunc(ids, list_limit,  FALSE);
	names = rem_util_strv_trunc(names, list_limit, FALSE);
	
	req->server->priv->msg.id = REM_MSG_ID_REQ_PLOBLIST;
	rem_serial_out(req->server->priv->msg.ba, client->encoding,
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
	
	ok = rem_net_tx(client->net, &req->server->priv->msg);
	if (!ok)
		g_hash_table_remove(req->server->priv->cht, req->client_chan);

	proxy_request_destroy(req);
}

static void
pp_reply_bye(DBusGProxy *dbus_proxy, GError *err, gpointer data)
{
	if (err)
		g_error_free(err);
}

///////////////////////////////////////////////////////////////////////////////
//
// private functions - shutdown procedure
//
///////////////////////////////////////////////////////////////////////////////

static gboolean
shutdown_stage2(RemServer *server)
{
	rem_bppl_down(server->priv->bppl);
	
	g_hash_table_destroy(server->priv->pht);
	g_hash_table_destroy(server->priv->cht);

	rem_img_down(server->priv->ri);
	
	g_free(server->priv->plist);
	
	g_byte_array_free(server->priv->msg.ba, TRUE);
	
	g_main_loop_quit(server->priv->ml);

	g_main_loop_unref(server->priv->ml);
	
	g_slice_free(RemServerPriv, server->priv);
	
	server->priv = NULL;
	
	return FALSE;
}

static gboolean
shutdown_stage1(RemServer *server)
{
	GList		*l, *el;
	RemClient	*client;
	Proxy		*proxy;
	gboolean	ok;
	
	g_assert(server->priv->shutting_down);
	
	g_hash_table_destroy(server->priv->sht);
	
	////////// say bye to the proxies //////////
	
	l = g_hash_table_get_values(server->priv->pht);
	
	for (el = l; el; el = el->next) {
		
		proxy = (Proxy*) el->data;
		
		net_sf_remuco_PP_bye_async(proxy->dbus_proxy,
			(net_sf_remuco_PP_bye_reply) &pp_reply_bye, NULL);
	}
	
	g_list_free(l);

	////////// say bye to the clients //////////
	
	server->priv->msg.id = REM_MSG_ID_IFS_SRVDOWN;
	server->priv->msg.ba->len = 0;

	l = g_hash_table_get_values(server->priv->cht);
	
	for (el = l; el; el = el->next) {
		
		client = (RemClient*) el->data;
		
		ok = rem_net_tx(client->net, &server->priv->msg);
	}
	
	g_list_free(l);

	////////// do the rest //////////
	
	// give clients and proxies a chance to receive bye message
	g_timeout_add(1000, (GSourceFunc) &shutdown_stage2, server);
	
	return FALSE;
}

static void
shutdown_sys(RemServer *server)
{
	GError		*err;
	gchar		*stdout, *stderr;
	gint		exit;
	
	if (server->priv->shutting_down)
		return;

	if (!server->priv->config->cmd_shutdown) {
		LOG_INFO("system shutdown disabled");
		return;
	}
	
	//shutdown(server);

	LOG_INFO("system shutdown: '%s'", server->priv->config->cmd_shutdown);
	
	err = NULL; stdout = NULL; stderr = NULL; exit = 0;
	g_spawn_command_line_sync(server->priv->config->cmd_shutdown, &stdout,
							  &stderr, &exit, &err);
	
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
		client->server->priv->msg.id = REM_MSG_ID_SYN_QUEUE;
		pl_id = REM_QUEUE_ID;
		pl_name = REM_QUEUE_NAME;
		ids = client->pp->state.queue_ids;
		names = client->pp->state.queue_names;
	} else {
		client->server->priv->msg.id = REM_MSG_ID_SYN_PLAYLIST;
		pl_id = REM_PLAYLIST_ID;
		pl_name = REM_PLAYLIST_NAME;
		ids = client->pp->state.playlist_ids;
		names = client->pp->state.playlist_names;		
	}
	
	rem_serial_out(client->server->priv->msg.ba, client->encoding,
				   "s s as as as as", pl_id, pl_name, NULL, NULL, ids, names);
	
	ok = rem_net_tx(client->net, &client->server->priv->msg);
	if (!ok)
		g_hash_table_remove(client->server->priv->cht, client->net->chan);
	
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
	
	g_free(server->priv->plist);	// strings are borrowed from pp descriptors
	
	server->priv->plist = rem_util_htk2sv(server->priv->pht, FALSE); // flat copy

	rem_util_dump_sv("new player list", server->priv->plist);
	
	server->priv->msg.id = REM_MSG_ID_SYN_PLIST;

	l = g_hash_table_get_values(server->priv->cht);
	
	el = l;
	while(el) {
		
		client = (RemClient*) el->data;
		el = el->next;
		
		if (!client->encoding)	// skip clients which did not send client info
			continue;
		
		rem_serial_out(server->priv->msg.ba, client->encoding, "as",
					   server->priv->plist);
		
		ok = rem_net_tx(client->net, &server->priv->msg);
		if (!ok )
			g_hash_table_remove(server->priv->cht, client->net->chan);
	}
	
	g_list_free(l);
	
	server->priv->plist_sync_triggered = FALSE;
	
	return FALSE;
}


static gboolean
client_sync_state(Proxy *pp)
{
	GSList		*l;
	RemClient	*client;
	gboolean	ok;
	
	pp->state.triggered_sync_state = FALSE;
	
	pp->server->priv->msg.id = REM_MSG_ID_SYN_STATE;
	
	l = pp->clients;
	while (l) {

		client = (RemClient*) l->data;
		l = l->next;
		
		g_assert(client->encoding);

		rem_serial_out(pp->server->priv->msg.ba, client->encoding,
					   "i i b b i b", pp->state.playback, pp->state.volume,
					   pp->state.repeat, pp->state.shuffle,
					   pp->state.position, pp->state.queue);
		
		ok = rem_net_tx(client->net, &pp->server->priv->msg);
		
		if (!ok)
			g_hash_table_remove(pp->server->priv->cht, client->net->chan);
	}
	
	return FALSE;
}

static gboolean
client_sync_plob(Proxy *pp)
{
	GSList		*l;
	RemClient	*client;
	gboolean	ok;
	GByteArray	*plob_img_data;
	
	pp->state.triggered_sync_plob = FALSE;
	
	pp->server->priv->msg.id = REM_MSG_ID_SYN_PLOB;

	l = pp->clients;
	while (l) {

		client = (RemClient*) l->data;
		l = l->next;
		
		g_assert(client->encoding);

		if (pp->server->priv->config->img && pp->state.plob_img)
			plob_img_data = rem_img_get(pp->server->priv->ri, pp->state.plob_img,
										client->screen_w, client->screen_h);
		else
			plob_img_data = NULL;
		
		rem_serial_out(pp->server->priv->msg.ba, client->encoding, "as ay",
					   pp->state.plob_meta, plob_img_data);
		
		if (plob_img_data)
			g_byte_array_free(plob_img_data, TRUE);
		
		ok = rem_net_tx(client->net, &pp->server->priv->msg);
		if (!ok)
			g_hash_table_remove(pp->server->priv->cht, client->net->chan);
		
	}
	
	return FALSE;
}

static gboolean
client_sync_playlist(Proxy *pp)
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
client_sync_queue(Proxy *pp)
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
		g_hash_table_remove(client->server->priv->cht, client->net->chan);
		return FALSE;
	}
	
	ok = rem_serial_in(client->server->priv->msg.ba, NULL, "i i s",
					   &client->screen_w, &client->screen_h,
					   &client->encoding);

	////////// check data //////////
	
	if (!ok) {
		LOG_WARN("client %s sent malformed data", client->net->addr);
		g_hash_table_remove(client->server->priv->cht, client->net->chan);
		return FALSE;
	}
	
	if (!client->encoding || !client->encoding[0]) {
		LOG_WARN("client %s sent no encoding", client->net->addr);
		g_hash_table_remove(client->server->priv->cht, client->net->chan);
		return FALSE;
	}
	
	if (client->screen_w + client->screen_h > 4000) {
		LOG_WARN("client %s sent bad screen size", client->net->addr);
		g_hash_table_remove(client->server->priv->cht, client->net->chan);
		return FALSE;
	}

	LOG_DEBUG("client encoding: %s", client->encoding);
	
	////////// send player list //////////
	
	LOG_DEBUG("send player list to client %s", client->net->addr);
	
	rem_serial_out(client->server->priv->msg.ba, client->encoding, "as",
				   client->server->priv->plist);
	
	client->server->priv->msg.id = REM_MSG_ID_SYN_PLIST;
	
	ok = rem_net_tx(client->net, &client->server->priv->msg);
	if (!ok ) {
		g_hash_table_remove(client->server->priv->cht, client->net->chan);
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
	ok = rem_serial_in(client->server->priv->msg.ba, client->encoding, "s",
					   &player);
	
	if (!ok) {
		LOG_WARN("client %s sent malformed data", client->net->addr);
		rem_serial_reset("s", &player);
		g_hash_table_remove(client->server->priv->cht, client->net->chan);
		return FALSE;
	}
	
	if (client->pp) {	// remove from current pp's client list
		client->pp->clients = g_slist_remove(client->pp->clients, client);
		client->pp = NULL;
	}
	
	client->pp = g_hash_table_lookup(client->server->priv->pht, player);
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
	
	client->server->priv->msg.id = REM_MSG_ID_IFS_PINFO;
	
	rem_serial_out(client->server->priv->msg.ba, client->encoding, "s i i",
				   client->pp->desc->name, client->pp->desc->flags,
				   client->pp->desc->rating);
	
	ok = rem_net_tx(client->net, &client->server->priv->msg);
	if (!ok) {
		g_hash_table_remove(client->server->priv->cht, client->net->chan);
		return FALSE;
	}
	
	////////// send player state //////////
	
	client->server->priv->msg.id = REM_MSG_ID_SYN_STATE;
	
	rem_serial_out(client->server->priv->msg.ba, client->encoding, "i i b b i b",
				   client->pp->state.playback, client->pp->state.volume,
				   client->pp->state.repeat, client->pp->state.shuffle,
				   client->pp->state.position, client->pp->state.queue);
	
	ok = rem_net_tx(client->net, &client->server->priv->msg);
	if (!ok) {
		g_hash_table_remove(client->server->priv->cht, client->net->chan);
		return FALSE;
	}
	
	////////// send current plob //////////
	
	client->server->priv->msg.id = REM_MSG_ID_SYN_PLOB;
	
	if (client->server->priv->config->img && client->pp->state.plob_img) {
		plob_img_data = rem_img_get(client->server->priv->ri,
									client->pp->state.plob_img,
									client->screen_w, client->screen_h);
	} else {
		plob_img_data = NULL;
	}
	
	rem_serial_out(client->server->priv->msg.ba, client->encoding, "as ay",
				   client->pp->state.plob_meta, plob_img_data);
	
	if (plob_img_data)
		g_byte_array_free(plob_img_data, TRUE);
	
	ok = rem_net_tx(client->net, &client->server->priv->msg);
	if (!ok) {
		g_hash_table_remove(client->server->priv->cht, client->net->chan);
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
	ProxyRequest	*req;

	if (!client->encoding) {
		LOG_WARN("client %s sent invalid message (%u)",
				 client->net->addr, client->server->priv->msg.id);
		g_hash_table_remove(client->server->priv->cht, client->net->chan);
		return FALSE;			
	}
	
	if (!client->pp) {
		LOG_WARN("client %s must choose a player", client->net->addr);
		// ignore, the client should receive the new player list soon
		return TRUE;
	}
	
	switch (client->server->priv->msg.id) {
		case REM_MSG_ID_CTL:
		
			////////// do a player control //////////
			
			param = NULL;
			ok = rem_serial_in(client->server->priv->msg.ba, client->encoding,
							   "i i s", &u, &i, &param);
			if (!ok) {
				LOG_WARN("client %s sent malformed data", client->net->addr);
				rem_serial_reset("i i s", &u, &i, &param);
				g_hash_table_remove(client->server->priv->cht,
									client->net->chan);
				return FALSE;
			}
			
			if (u == REM_CTL_SHUTDOWN) {
				g_free(param);
				shutdown_sys(client->server);
				return TRUE;
			}
			
			req = proxy_request_new();
			req->player = g_strdup(client->pp->desc->name);
			req->server = client->server;
			req->client_chan = client->net->chan;

			net_sf_remuco_PP_control_async(client->pp->dbus_proxy, u, i, param,
				(net_sf_remuco_PP_control_reply) &pp_reply_control, req);
			
			g_free(param);
			
		break;
		case REM_MSG_ID_REQ_PLOB:
		
			id = NULL;
			ok = rem_serial_in(client->server->priv->msg.ba, client->encoding,
							   "s", &id);
			if (!ok) {
				LOG_WARN("client %s sent malformed data", client->net->addr);
				rem_serial_reset("%s", &id);
				g_hash_table_remove(client->server->priv->cht,
									client->net->chan);
				return FALSE;
			}
			
			////////// request a plob //////////
			
			req = proxy_request_new();
			req->player = g_strdup(client->pp->desc->name);
			req->id = id;
			req->server = client->server;
			req->client_chan = client->net->chan;
			
			net_sf_remuco_PP_request_plob_async(client->pp->dbus_proxy, id,
					(net_sf_remuco_PP_request_plob_reply)
					&pp_reply_request_plob, req);
			
		break;
		case REM_MSG_ID_REQ_PLOBLIST:
		
			id = NULL;
			ok = rem_serial_in(client->server->priv->msg.ba, client->encoding,
							   "s", &id);
			if (!ok) {
				LOG_WARN("client %s sent malformed data", client->net->addr);
				rem_serial_reset("s", &id);
				g_hash_table_remove(client->server->priv->cht,
									client->net->chan);
				return FALSE;
			}
			
			////////// request a ploblist //////////

			req = proxy_request_new();
			req->player = g_strdup(client->pp->desc->name);
			req->id = id;
			req->server = client->server;
			req->client_chan = client->net->chan;
			
			net_sf_remuco_PP_request_ploblist_async(client->pp->dbus_proxy, id,
				(net_sf_remuco_PP_request_ploblist_reply)
				&pp_reply_request_ploblist, req);

		break;
		default:
			
			////////// invalid message //////////
			
			LOG_WARN("client %s sent invalid message", client->net->addr);
			g_hash_table_remove(client->server->priv->cht, client->net->chan);
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

static Proxy*
pp_get(RemServer *server, const gchar *player, GError **err)
{
	Proxy *pp;
	
	pp = g_hash_table_lookup(server->priv->pht, player);
	
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
	Proxy			*pp;
	ProxyRequest	*req;
	
	if (server->priv->shutting_down)
		return FALSE;
	
	l = g_hash_table_get_values(server->priv->pht);
	
	if (!l) {
		LOG_INFO("there are no player proxies -> going down");
		shutdown(server);
		return FALSE;
	}
	
	for (el = l; el; el = el->next) {

		pp = (Proxy*) el->data; g_assert(pp);
		
		req = proxy_request_new();
		req->player = g_strdup(pp->desc->name);
		req->server = server;
		
		LOG_DEBUG("ping %s", pp->desc->name);
		
		net_sf_remuco_PP_control_async(pp->dbus_proxy, REM_CTL_IGNORE, 0, "",
			(net_sf_remuco_PP_control_reply) &pp_reply_control, req);
	}
	
	g_list_free(l);
	
	return TRUE;
}

/** Callback for pp hashtable to get comletely rid of a pp. */
static void
pp_bye(Proxy *pp)
{
	GSList		*el;
	RemClient	*client;
	
	if (!pp)
		return;
	
	LOG_INFO("bye %s", pp->desc->name);
	
	//g_object_unref(pp->dbus); // FIXME: not needed
	
	if (pp->desc) {
		g_free(pp->desc->name);
		g_slice_free(PlayerDesc, pp->desc);
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
	
	if (!pp->server->priv->shutting_down &&
		!pp->server->priv->plist_sync_triggered) {
	
		pp->server->priv->plist_sync_triggered = TRUE;

		g_idle_add_full(G_PRIORITY_HIGH, (GSourceFunc) &client_sync_plist,
						pp->server, NULL);
	}
	
	g_slice_free(Proxy, pp);
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
		
		ok = rem_net_rx(client->net, &client->server->priv->msg);
		if (!ok) {
			g_hash_table_remove(client->server->priv->cht, chan);
			return FALSE;
		}
		
		////////// handle a client which just has connected //////////
		
		if (client->server->priv->msg.id == REM_MSG_ID_IFC_CINFO) {
			
			return client_welcome(client);
		}
		
		////////// handle a client which chooses a player //////////
		
		if (client->server->priv->msg.id == REM_MSG_ID_SEL_PLAYER) {
			
			return client_give_player(client);
		}
		
		////////// handle a fully connected client  //////////

		return client_handle_message(client);
		
	} else if (cond & G_IO_HUP) { // probably client disconnected

		LOG_INFO("a client disconnected");
		g_hash_table_remove(client->server->priv->cht, chan);
		return FALSE;

	} else { // some error
		
		LOG_ERROR("a client connection is broken");
		g_hash_table_remove(client->server->priv->cht, chan);
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

		server_net = g_hash_table_lookup(server->priv->sht, chan);
		
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
		
		g_hash_table_insert(server->priv->cht, client->net->chan, client);
		
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
// dbus interface
//
///////////////////////////////////////////////////////////////////////////////

gboolean
rem_server_check(RemServer *server, guint version, GError **err)
{
	LOG_INFO("called");
	
	if (version != REM_SERVER_PP_PROTO_VERSION) {
		g_set_error(err, REM_SERVER_ERR_DOMAIN, 0, REM_SERVER_ERR_VERSION_MISMATCH);
		return FALSE;
	}
	
	return TRUE;
}

gboolean
rem_server_hello(RemServer *server, gchar* player,
				 guint flags, guint rating,
				 GError **err)
{
	Proxy		*pp;
	
	////////// check if not known already and name is valid //////////
	
	LOG_INFO("player '%s' says hello", player);

	if (server->priv->shutting_down) {
		LOG_DEBUG("shutting down -> ignore");
		return TRUE;
	}
	
	if (g_hash_table_lookup(server->priv->pht, player)) {
		
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
	
	pp = g_slice_new0(Proxy);
	
	pp->desc = g_slice_new0(PlayerDesc);
	pp->desc->name = g_strdup(player);
	pp->desc->flags = flags;
	pp->desc->rating = rating;
	
	pp->dbus_proxy = rem_dbus_proxy(server->priv->dbus_conn, player);
	
	pp->server = server;

	g_hash_table_insert(server->priv->pht, pp->desc->name, pp);

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
	
	if (!server->priv->plist_sync_triggered) {
		
		server->priv->plist_sync_triggered = TRUE;
		
		g_idle_add_full(G_PRIORITY_HIGH, (GSourceFunc) &client_sync_plist,
						server, NULL);
	}
	
	return TRUE;
}

gboolean
rem_server_update_state(RemServer *server, gchar *player,
						guint playback, guint volume, gboolean repeat,
						gboolean shuffle, guint position, gboolean queue,
						GError **err)
{
	Proxy		*pp;
	gboolean	diff;
	
	LOG_DEBUG("from %s", player);
	
	if (server->priv->shutting_down) {
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

gboolean
rem_server_update_plob(RemServer *server, gchar *player,
					   gchar *id, gchar *img, GHashTable *meta,
					   GError **err)
{
	Proxy		*pp;

	LOG_DEBUG("from %s", player);

	if (server->priv->shutting_down) {
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

gboolean
rem_server_update_playlist(RemServer *server, gchar *player,
						   gchar **ids, gchar **names,
						   GError **err)
{
	Proxy	*pp;
	
	LOG_DEBUG("from %s", player);

	if (server->priv->shutting_down) {
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
		g_hash_table_remove(pp->server->priv->pht, player);
		
		return FALSE;
	}

	ids = rem_util_strv_trunc(ids, server->priv->config->list_limit, TRUE);
	names = rem_util_strv_trunc(names, server->priv->config->list_limit, TRUE);
	
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

gboolean
rem_server_update_queue(RemServer *server, gchar *player,
						gchar **ids, gchar **names,
						GError **err)
{
	Proxy	*pp;
	
	LOG_DEBUG("from %s", player);

	if (server->priv->shutting_down) {
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
		g_hash_table_remove(pp->server->priv->pht, player);
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

gboolean
rem_server_bye(RemServer *server, gchar *player,
			   GError **err)
{
	LOG_DEBUG("from %s", player);

	if (server->priv->shutting_down) {
		LOG_DEBUG("shutting down -> ignore");
		return TRUE;
	}
	
	g_hash_table_remove(server->priv->pht, player);
	
	return TRUE;
}

///////////////////////////////////////////////////////////////////////////////
//
// main
//
///////////////////////////////////////////////////////////////////////////////

gboolean
rem_server_up(RemServer *server, GMainLoop *ml)
{
	RemServerPriv		*priv;
	gboolean			ok;
	GError				*err;
	RemNetConfig		config_net;
	
	g_assert(!server->priv);
	
	////////// load and check configuration //////////
	
	ok = rem_config_load("Server", "conf", FALSE, config_entries);
	if (!ok) {
		return FALSE;
	}

	if (!config.bt_on && !config.ip_on) {
		
		LOG_ERROR("neither Bluetooth nor IP is activated");
		return FALSE;
	}
	
	if (config.ip_port >= 65536) {
	
		LOG_ERROR("invalid port number");
		return FALSE;
	}
	
	if (strlen(config.img) == 0 ||
		g_str_equal(config.img, REM_IMG_NONE)) {
		
		g_free(config.img);
		config.img = NULL;
	}
	
	////////// init server type //////////
	
	priv = g_slice_new0(RemServerPriv);

	priv->config = &config;
	
	////////// net (bt, ip, ..) //////////
	
	config_net.bt_on = priv->config->bt_on;
	config_net.ip_on = priv->config->ip_on;
	config_net.ip_port = priv->config->ip_port;
	
	priv->sht = rem_net_up(&config_net);
	
	if (!priv->sht) {
		LOG_ERROR("failed to start net");
		g_slice_free(RemServerPriv, priv);
		return FALSE;
	}
	
	g_hash_table_foreach(priv->sht, (GHFunc) &io_server_watch, priv);

	////////// dbus //////////
	
	err = NULL;
	priv->dbus_conn = rem_dbus_connect(&err);
	if (!priv->dbus_conn) {
		LOG_ERROR_GERR(err, "failed to connect to dbus");
		g_hash_table_destroy(priv->sht);
		g_slice_free(RemServerPriv, priv);
		return FALSE;
	}
	
	////////// bpp launcher //////////
	
	if (priv->config->enable_bpps) {
		priv->bppl = rem_bppl_up();
	}
	
	if (!priv->config->enable_bpps || !rem_bppl_bpp_count(priv->bppl)) {
		// if we do not use BPPs, shut down automatically once there are no
		// more player proxies
		g_timeout_add(30000, (GSourceFunc) &pp_ping, priv);
	}
	
	////////// misc initializations //////////
	
	priv->msg.ba = g_byte_array_sized_new(1024);
	
	priv->cht = g_hash_table_new_full(&g_direct_hash, &g_direct_equal, NULL,
										(GDestroyNotify) &client_bye);
	
	priv->pht = g_hash_table_new_full(&g_str_hash, &g_str_equal, NULL,
										(GDestroyNotify) &pp_bye);

	if (priv->config->img)
		priv->ri = rem_img_up(priv->config->img);
	
	////////// here we go //////////
	
	priv->shutting_down = FALSE;
	
	g_main_loop_ref(ml);
	
	priv->ml = ml;
	
	server->priv = priv;
	
	return TRUE;
}

/**
 * Initiates the shut down process. Server still needs the main loop running!
 * Server will stop the main loop if shutdown is finished.
 */
void
rem_server_down(RemServer *server)
{
	g_assert(server && server->priv);
	
	if (server->priv->shutting_down) {
		LOG_DEBUG("already shutting down");
		return;
	}
	
	server->priv->shutting_down = TRUE;
	
	LOG_DEBUG("shutting down");
	
	g_idle_add_full(G_PRIORITY_HIGH, (GSourceFunc) &shutdown_stage1,
					server, NULL);
}
