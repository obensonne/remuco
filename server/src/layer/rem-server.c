#include <remuco.h>

#include "rem-net.h"
#include "../data/rem-data.h"
#include "../util/rem-util.h"

#define G_IO_INERRHUPNVAL	(G_IO_IN | G_IO_ERR | G_IO_HUP | G_IO_NVAL)

#define REM_POLL_IVAL		2000

#define REM_PL_MAX_LEN		250

typedef enum {
	REM_MSG_ID_IGNORE,
	REM_MSG_ID_IFS_PINFO,
	REM_MSG_ID_IFS_STATUS,
	REM_MSG_ID_IFS_CAP,			/** CAP = currently active plob */
	REM_MSG_ID_IFS_PLAYLIST,
	REM_MSG_ID_IFS_QUEUE,
	REM_MSG_ID_IFS_SRVDOWN,
	REM_MSG_ID_IFC_CINFO,
	REM_MSG_ID_CTL_SCTRL,
	REM_MSG_ID_CTL_UPD_PLOB,
	REM_MSG_ID_CTL_UPD_PLOBLIST, // FUTURE FEATURE
	REM_MSG_ID_CTL_PLAY_PLOBLIST, // FUTURE FEATURE
	REM_MSG_ID_REQ_PLOB,
	REM_MSG_ID_REQ_PLOBLIST,
	REM_MSG_ID_REQ_SEARCH,
	REM_MSG_ID_REQ_LIBRARY,
	REM_MSG_ID_COUNT
} RemMsgID;

struct _RemServer {
	
	////////// communication related fields //////////
	
	RemNetServer			*net_server;
	GHashTable				*clients;
	RemNetMsg				*net_msg_rx;	// reuse to avoid reallocations
	RemNetMsg				net_msg_bc;		// note: this is no pointer
	gchar					*charset;
	
	GMainContext			*mc;
	gboolean				poll;			// whether we poll or get notified
	
	////////// the player proxy //////////
	
	RemPPCallbacks			*pp_cb;
	RemPPPriv				*pp_priv;
	
	gboolean				pending_sync;	// ensure calm handling of rem_server_notify()
	gboolean				pending_down;	// ensure calm handling of rem_server_down()
	
	////////// player related fields //////////
	
	RemPlayerInfo			*pinfo;
	RemPlayerStatus			*pstatus;
	RemPlayerStatusFP		*pstatus_fp;
	RemPlob					*cap;
	RemPloblist				*playlist;
	guint					playlist_hash;
	RemPloblist				*queue;
	guint					queue_hash;
	RemLibrary				*lib;
};

typedef struct {
	RemNetClient		*net;
	RemClientInfo		*info;
	guint				src_id;
} RemClient;

//////////////////////////////////////////////////////////////////////////////
//
// private functions: IO and serialization
//
//////////////////////////////////////////////////////////////////////////////

static GByteArray*
priv_serialize(const RemServer *server,
			   const RemClient *client,
			   RemMsgID msg_id,
			   const gpointer data,
			   gboolean *client_independent)
{
	g_assert_debug(server && client);
	
	GByteArray					*ba = NULL;
	
	const RemPloblist			*pl;
	const RemLibrary			*lib;
	const RemPlob				*plob;
	const RemPlayerInfo			*pinfo;
	const RemPlayerStatusBasic	*psi;
	
	if (!data) return NULL;
	
	if (client_independent) *client_independent = TRUE;

	switch (msg_id) { // messages from server

	case REM_MSG_ID_IFS_CAP:
	case REM_MSG_ID_REQ_PLOB:
	
		plob = (RemPlob*) data;
		
		ba = rem_plob_serialize(plob,
								server->charset,
								client->info->charsets,
								client->info->img_width,
								client->info->img_height);
		
		break;
		
	case REM_MSG_ID_IFS_PLAYLIST:
	case REM_MSG_ID_IFS_QUEUE:
	case REM_MSG_ID_REQ_PLOBLIST:
	case REM_MSG_ID_REQ_SEARCH:
	
		pl = (RemPloblist*) data;
		
		ba = rem_ploblist_serialize(
				pl, server->charset, client->info->charsets);
		
		if (client_independent) *client_independent = FALSE;

		break;
		
	case REM_MSG_ID_IFS_PINFO:
	
		pinfo = (RemPlayerInfo*) data;
		
		ba = rem_player_info_serialize(
				pinfo, server->charset, client->info->charsets);
		
		break;
		
	case REM_MSG_ID_IFS_STATUS:
				
		psi = (RemPlayerStatusBasic*) data;
		
		ba = rem_player_status_basic_serialize(
				psi, server->charset, client->info->charsets);
				
		break;
		
	case REM_MSG_ID_IFS_SRVDOWN:
	
		break;
		
	case REM_MSG_ID_REQ_LIBRARY:
	
		lib = (RemLibrary*) data;
		
		ba = rem_library_serialize(
				lib, server->charset, client->info->charsets);
		
		if (client_independent) *client_independent = FALSE;

		break;
	
	default:
	
		g_assert_not_reached();
		
		break;
	}
	
	return ba;
}

/**
 * Hashtable 'for-each' callback function to broadcast player changes
 * to clients. What this function sends depends on server->net_msg_bc.id.
 */
static void
priv_htcb_tx(gpointer key, gpointer val, gpointer data)
{
	RemServer	*server;
	RemClient	*client;
	gpointer	tx_data;
	
	server = (RemServer*) data;
	client = (RemClient*) val;
	
	// skip clients not fully connected:
	if (!client->info) return;
	
	switch (server->net_msg_bc.id) {
		case REM_MSG_ID_IFS_STATUS:
			tx_data = &server->pstatus_fp->priv;
		break;
		case REM_MSG_ID_IFS_CAP:
			tx_data = server->cap;
		break;
		case REM_MSG_ID_IFS_PLAYLIST:
			tx_data = server->playlist;
		break;
		case REM_MSG_ID_IFS_QUEUE:
			tx_data = server->queue;
		break;
		case REM_MSG_ID_IFS_SRVDOWN:
			tx_data = NULL;
		break;
		default:
			g_assert_not_reached();
			return;
		break;
	}
	
	server->net_msg_bc.ba = priv_serialize(server, client,
								server->net_msg_bc.id, tx_data, NULL);
	
	rem_net_client_txmsg(client->net, &server->net_msg_bc);
	
	if (server->net_msg_bc.ba) {
		g_byte_array_free(server->net_msg_bc.ba, TRUE);
		server->net_msg_bc.ba = NULL;
	}
}

//////////////////////////////////////////////////////////////////////////////
//
// private functions: player interaction
//
//////////////////////////////////////////////////////////////////////////////

/**
 * Given a list of PIDs, this function builds a ploblist, listing the related
 * plobs.
 */
static void
priv_build_ploblist(RemServer *s,
					RemPloblist *pl,
					RemStringList *pids)
{
	g_assert_debug(s && pl && pids);

	const gchar		*pid;
	GString			*title;
	const gchar		*s1, *s2;
	RemPlob			*plob;
	guint			len;
	
	rem_ploblist_clear(pl);
	
	// add the plobs to the ploblist
	
	title = g_string_new("");
	
	rem_sl_iterator_reset(pids);
	
	len = 0;
	while((pid = rem_sl_iterator_next(pids)) && len++ < REM_PL_MAX_LEN) {
		
		plob = s->pp_cb->get_plob(s->pp_priv, pid);
		if (!plob) plob = rem_plob_new_unknown(pid);
			
		s1 = rem_plob_meta_get(plob, REM_PLOB_META_TITLE);
		s2 = rem_plob_meta_get(plob, REM_PLOB_META_ARTIST);
		
		g_string_truncate(title, 0);
		g_string_append_printf(title, "%s - %s", s1, s2);
		
		rem_plob_destroy(plob);
		
		rem_ploblist_append_const(pl, pid, title->str);
	}
	
	if (pid) { // playlist too long
		
		// we add a final entry (with the ID form the last plob) to inform
		// that the following plobs have been discarded
		g_string_printf(title, "Playlist truncated!");
		rem_ploblist_append_const(pl, pid, title->str);
	}
	
	g_string_free(title, TRUE);
	
}

/**
 * Synchronizes our internal player status with the current one via the player
 * proxy. Then checks for changes and broadcasts it to all connected clients.
 */
static void
priv_synchronize(RemServer* server)
{
	LOG_NOISE("called");
	
	RemPlayerStatusDiff	diff;
	
	////////// status change ? //////////
	
	server->pp_cb->synchronize(server->pp_priv, server->pstatus);

	rem_api_check(server->pstatus->cap_pid,
			"bad return from callback function 'synchronize': cap_pid is NULL");
	rem_api_check(server->pstatus->playlist,
			"bad return from callback function 'synchronize': playlist is NULL");
	rem_api_check(server->pstatus->queue,
			"bad return from callback function 'synchronize': queue is NULL");
	
	diff = rem_player_status_fp_update(server->pstatus, server->pstatus_fp);
	
	////////// player status basic //////////

	if (diff & REM_PS_DIFF_SVRSP) {
		LOG_DEBUG("broadcast new player status");
		server->net_msg_bc.id = REM_MSG_ID_IFS_STATUS;
		g_hash_table_foreach(server->clients, &priv_htcb_tx, server);
	}
	
	////////// currently active plob //////////

	if (diff & REM_PS_DIFF_PID) {
		rem_plob_destroy(server->cap);
		if (server->pstatus_fp->cap_pid->len) {
			server->cap = server->pp_cb->get_plob(
					server->pp_priv, server->pstatus_fp->cap_pid->str);
		} else {
			server->cap = NULL;
		}
		LOG_DEBUG("broadcast new cap");
		#ifdef DO_LOG_NOISE
		rem_plob_dump(server->cap);
		#endif
		server->net_msg_bc.id = REM_MSG_ID_IFS_CAP;
		g_hash_table_foreach(server->clients, &priv_htcb_tx, server);
	}
	
	////////// playlist //////////

	if (diff & REM_PS_DIFF_PLAYLIST) {
		priv_build_ploblist(server, server->playlist, server->pstatus->playlist);
		LOG_DEBUG("broadcast new playlist");
		server->net_msg_bc.id = REM_MSG_ID_IFS_PLAYLIST;
		g_hash_table_foreach(server->clients, &priv_htcb_tx, server);
	}
	
	////////// queue //////////

	if (diff & REM_PS_DIFF_QUEUE) {
		priv_build_ploblist(server, server->queue, server->pstatus->queue);
		LOG_DEBUG("broadcast new queue");
		server->net_msg_bc.id = REM_MSG_ID_IFS_QUEUE;
		g_hash_table_foreach(server->clients, &priv_htcb_tx, server);
	}
	
	LOG_NOISE("done");
	
	LOG_NOISE("==============================================================");
	LOG_NOISE("= New Player Status                                          =");
	LOG_NOISE("= ---------------------------------------------------------- =");
	LOG_NOISE("= ");
	LOG_NOISE("= PID    : %s", server->pstatus->cap_pid->str);
	LOG_NOISE("= POS    : %i", server->pstatus->cap_pos);
	LOG_NOISE("= PBS    : %u", server->pstatus->pbs);
	LOG_NOISE("= PL-Len : %u", rem_sl_length(server->pstatus->playlist));
	LOG_NOISE("= QU-Len : %u", rem_sl_length(server->pstatus->queue));
	LOG_NOISE("= Volume : %u", server->pstatus->volume);
	LOG_NOISE("= Flags  : %2X", server->pstatus->flags);
	LOG_NOISE("==============================================================");
}

static void
priv_handle_pimsg(RemServer* server,
				  const RemClient *client,
				  const RemNetMsg *msg)
{
	RemNetMsg			msg_response;
	RemString			*string;
	RemSimpleControl	*sctrl;
	RemPlob				*plob;
	RemStringList		*sl;
	RemPloblist			*pl;
	const gchar			*pl_name;
	
	msg_response.id = 0;
	msg_response.ba = NULL;
	
	switch (msg->id) {
		
	case REM_MSG_ID_IGNORE:
		break;
			
	case REM_MSG_ID_REQ_PLOB:
		
		string = rem_string_unserialize(msg->ba, server->charset);
		
		plob = server->pp_cb->get_plob(server->pp_priv, string->value);
		
		msg_response.id = msg->id;
		msg_response.ba = priv_serialize(server, client, msg->id, plob, NULL);
		
		rem_plob_destroy(plob);
		rem_string_destroy(string);
		
		break;
		
	case REM_MSG_ID_REQ_PLOBLIST:
		
		string = rem_string_unserialize(msg->ba, server->charset);
		
		sl = server->pp_cb->get_ploblist(server->pp_priv, string->value);
		
		pl_name = rem_library_get_name(server->lib, string->value);
		pl = rem_ploblist_new(string->value, pl_name);
		priv_build_ploblist(server, pl, sl);
		rem_sl_destroy(sl);
		
		msg_response.id = msg->id;
		msg_response.ba = priv_serialize(server, client, msg->id, pl, NULL);
		
		rem_ploblist_destroy(pl);
		rem_string_destroy(string);
		
		break;
		
	case REM_MSG_ID_REQ_LIBRARY:
	
		rem_library_destroy(server->lib);
		
		server->lib = server->pp_cb->get_library(server->pp_priv);
		if (!server->lib) {
			LOG_WARN("library request from pp returned NULL");
			server->lib = rem_library_new();
		}
		
		msg_response.id = msg->id;
		msg_response.ba = priv_serialize(
							server, client, msg->id, server->lib, NULL);
		
		break;

	case REM_MSG_ID_CTL_PLAY_PLOBLIST:
			
		string = rem_string_unserialize(msg->ba, server->charset);
		
		server->pp_cb->play_ploblist(server->pp_priv, string->value);
		
		rem_string_destroy(string);

		break;
			
	case REM_MSG_ID_CTL_SCTRL:
			
		sctrl = rem_simple_control_unserialize(msg->ba, server->charset);
		
		server->pp_cb->simple_ctrl(server->pp_priv,
								  (RemSimpleControlCommand) sctrl->cmd,
								  sctrl->param);
		
		// ensure that important changes immediately go to clients
		if (server->poll && (sctrl->cmd == REM_SCTRL_CMD_JUMP ||
							 sctrl->cmd == REM_SCTRL_CMD_NEXT ||
							 sctrl->cmd == REM_SCTRL_CMD_PREV ||
							 sctrl->cmd == REM_SCTRL_CMD_RESTART ||
							 sctrl->cmd == REM_SCTRL_CMD_STOP ||
							 sctrl->cmd == REM_SCTRL_CMD_VOLUME)) {
			
			priv_synchronize(server);
		}
		
		rem_simple_control_destroy(sctrl);
		
		break;
			
	case REM_MSG_ID_REQ_SEARCH:
		
		LOG_WARN("search not yet implemented");
		//plob = rem_plob_unserialize(msg->ba, server->charset);			
		break;
		
	case REM_MSG_ID_CTL_UPD_PLOB:
			
		LOG_WARN("update plob not yet implemented");
		//plob = rem_plob_unserialize(msg->ba, server->charset);			
		break;
			
	case REM_MSG_ID_CTL_UPD_PLOBLIST:

		LOG_WARN("update ploblist not yet implemented");
		//pl = rem_ploblist_unserialize(msg->ba, server->charset);
		break;
			
	default:
		
		LOG_WARN("unsupported message id");
		break;
	}

	if (msg_response.id) {
		
		rem_net_client_txmsg(client->net, &msg_response);
		
		if (msg_response.ba) g_byte_array_free(msg_response.ba, TRUE);
	}
	
}

//////////////////////////////////////////////////////////////////////////////
//
// private functions: misc
//
//////////////////////////////////////////////////////////////////////////////

/**
 * Frees all resources hold by 'server' _except_:
 * - the net server (net_server)
 * - the clients hash tabel (clients)
 * Also frees the RemServer stucture itself.
 */
static void
priv_server_free(RemServer *server)
{
	if (server->net_msg_bc.ba)	g_byte_array_free(server->net_msg_bc.ba, TRUE);
	if (server->net_msg_rx)		rem_net_msg_destroy(server->net_msg_rx);
	if (server->charset)		g_free(server->charset);

	if (server->cap)			rem_plob_destroy(server->cap);
	if (server->lib)			rem_library_destroy(server->lib);
	if (server->playlist)		rem_ploblist_destroy(server->playlist);
	if (server->queue)			rem_ploblist_destroy(server->queue);
	if (server->pstatus)		rem_player_status_destroy(server->pstatus);
	if (server->pstatus_fp)		rem_player_status_fp_destroy(server->pstatus_fp);
	if (server->pinfo)			rem_player_info_destroy(server->pinfo);
	
	if (server->pp_cb)			g_free(server->pp_cb);

	g_slice_free(RemServer, server);
}

static RemPlayerInfo*
priv_create_player_info(const RemPPDescriptor *desc, const RemPPCallbacks *cbs)
{
	RemPlayerInfo			*pinfo;
	RemPlayerInfoFeature	features = 0;

	////////// check for player name //////////
	
	if (!desc->player_name) {
		LOG_ERROR("no player name given");
		return NULL;
	}

	////////// mandatory callback functions //////////
	
	if (!cbs->synchronize) {
		LOG_ERROR("mandatory callback function 'get_player_status' not set");
		return NULL;
	}
	if (!cbs->get_plob) {
		LOG_ERROR("mandatory callback function 'get_plob' not set");
		return NULL;
	}
	if (!cbs->notify) {
		LOG_ERROR("mandatory callback function 'notify' not set");
		return NULL;
	}
	if (!cbs->simple_ctrl) {
		LOG_ERROR("mandatory callback function 'simple_ctrl' not set");
		return NULL;
	}
	
	////////// optional callback functions //////////
	
	if (cbs->get_library) {
		if (!desc->supports_playlist) {
			LOG_ERROR("Library support requires playlist support!");
			return NULL;
		}
		features |= REM_FEATURE_LIBRARY;
		LOG_DEBUG("Player/PP supports library");
	}
	if (cbs->get_ploblist) {
		features |= REM_FEATURE_LIBRARY_PL_CONTENT;
		LOG_DEBUG("Player/PP can give us content of any ploblists\n");
		if (!cbs->get_library) {
			LOG_ERROR("You should set callback function 'get_library' too!");
			return NULL;
		}
	}
	if (cbs->play_ploblist) {
		features |= REM_FEATURE_LIBRARY_PL_PLAY;
		LOG_DEBUG("Player/PP supports playing a certain ploblist\n");
		if (!cbs->get_library) {
			LOG_ERROR("You should set callback function 'get_library' too!");
			return NULL;
		}
	}
	if (cbs->search) {
		features |= REM_FEATURE_SEARCH;
		LOG_DEBUG("Player/PP supports plob search");
	}
	if (cbs->update_plob) {
		features |= REM_FEATURE_PLOB_EDIT;
		LOG_DEBUG("Player/PP supports editing plobs");
	}
	if (cbs->update_ploblist) {
		features |= REM_FEATURE_PLOBLIST_EDIT;
		// TODO features |= REM_FEATURE_PL ???;
		LOG_DEBUG("Player/PP supports editing ploblists");
	}
	
	////////// rating //////////
	
	if (desc->max_rating_value) {
		features |= REM_FEATURE_RATE;
	}
	
	////////// misc //////////

	if (desc->supports_playlist) {
		features |= REM_FEATURE_PLAYLIST;
		LOG_DEBUG("Player/PP supports playlist");
	}
	if (desc->supports_queue) {
		features |= REM_FEATURE_QUEUE;
		LOG_DEBUG("Player/PP supports queue");
	}

	if (desc->supports_playlist_jump) {
		features |= REM_FEATURE_PLAYLIST_JUMP;
		if (!desc->supports_playlist) {
			LOG_ERROR("Playlist jump only makes sense with playlist support!");
			return NULL;
		}
	}
	if (desc->supports_queue_jump) {
		features |= REM_FEATURE_QUEUE_JUMP;
		if (!desc->supports_queue) {
			LOG_ERROR("Queue jump only makes sense with queue support!");
			return NULL;
		}
	}
	
	if (desc->supports_seek) {
		features |= REM_FEATURE_SEEK;
	}
	
	if (desc->supports_tags) {
		features |= REM_FEATURE_PLOB_TAGS;
	}

	pinfo = rem_player_info_new(desc->player_name);
	pinfo->features = features;
	pinfo->rating_max = desc->max_rating_value;

	return pinfo;
}

/**
 * Disconnect a client and free all resource occupied by the client.
 * This includes removing the IO watch source of the client's channel.
 * 
 * This function is not intended to be called directly. It is the free function
 * for the values in the hash table rem->clients. So to disconnect a client
 * and release all its resource, instead call g_hash_table_remove(..) with
 * the client's IO channel as key.
 * 
 * @param data
 * 		the client (RemClient*)
 */
static void
priv_disconnect_client(gpointer data)
{
	g_assert(data);
	
	LOG_NOISE("called");

	RemClient *client = (RemClient*) data;

	g_source_remove(client->src_id);

	rem_net_client_destroy(client->net);
	rem_client_info_destroy(client->info);
	
	g_slice_free(RemClient, client);
	
	LOG_NOISE("done");
}

/**
 * Callback function to handle changes in player state.
 * 
 * We do the work related to the public funtions of the remuco lib in callback
 * functions to make sure that everything by the remuco lib is done in the
 * same main context. 
 */  
static gboolean
priv_cb_notify(gpointer data)
{
	LOG_NOISE("called");
	
	RemServer	*server;
	
	server = (RemServer*) data;
	
	priv_synchronize(server);
	
	server->pending_sync = FALSE;
	
	LOG_NOISE("done");

	return FALSE;
}

static gboolean
priv_cb_poll(gpointer data)
{
	LOG_NOISE("called");
	
	RemServer	*server;
	
	server = (RemServer*) data;
	
	priv_synchronize(server);
	
	LOG_NOISE("done");

	return TRUE;
}


static gboolean
priv_cb_down(gpointer data)
{
	LOG_NOISE("called");

	RemServer		*server;
	RemPPNotifyFunc	pp_cb_notify;
	RemPPPriv		*pp_priv;
	
	server = (RemServer*) data;
	
	// disconnect the clients:
	g_hash_table_destroy(server->clients);
	
	// shut down the server (net):
	rem_net_server_destroy(server->net_server);

	// remove all our sources from main context
	while(g_source_remove_by_user_data(server));

	// rescue this to finally emit shut down finished event:
	pp_cb_notify = server->pp_cb->notify;
	pp_priv = server->pp_priv;
	
	// free all our resources:
	priv_server_free(server);
	
	// let the pp know that we are down:
	pp_cb_notify(pp_priv, REM_SERVER_EVENT_DOWN);
	
	LOG_NOISE("done");

	return FALSE;
	
}

//////////////////////////////////////////////////////////////////////////////
//
// private functions: handle net (IO) events
//
//////////////////////////////////////////////////////////////////////////////

/**
 * IO callback function for a client.
 */ 
static gboolean
priv_iocb_client(GIOChannel *chan, GIOCondition cond, gpointer data)
{
	RemServer	*server;
	RemClient	*client;
	gint		ret;
	RemNetMsg	*mrx, mtx;
	
	server = (RemServer*) data;
	mrx = server->net_msg_rx;
	
	client = g_hash_table_lookup(server->clients, chan);
	
	g_assert(client); // this assures we remove clients from the hashtable only
					  // here, where we return FALSE if we removed a client
	
	if (cond & G_IO_IN) {
	
		LOG_DEBUG("G_IO_IN");
		
		ret = rem_net_client_rxmsg(client->net, mrx);
		if (ret < 0) {
			g_hash_table_remove(server->clients, chan);
			return FALSE;
		}
		
		if (mrx->id == REM_MSG_ID_IFC_CINFO) {
			
			// check if there already is a client info:
			if (client->info) {
				LOG_WARN("rx'ed client info, but already have it");
				g_hash_table_remove(server->clients, chan);
				return FALSE;
			}
			
			// unserialize the client info
			client->info = rem_client_info_unserialize(mrx->ba, server->charset);
			
			// send player info
			LOG_DEBUG("send player info to client %s", client->net->addr);
			mtx.id = REM_MSG_ID_IFS_PINFO;
			mtx.ba = priv_serialize(
						server, client, mtx.id, server->pinfo, NULL);
			rem_net_client_txmsg(client->net, &mtx);
			g_byte_array_free(mtx.ba, TRUE);
			
			// send player status
			LOG_DEBUG("send player status to client %s", client->net->addr);
			mtx.id = REM_MSG_ID_IFS_STATUS;
			mtx.ba = priv_serialize(server, client, mtx.id, server->pstatus, NULL);
			rem_net_client_txmsg(client->net, &mtx);
			g_byte_array_free(mtx.ba, TRUE);
			
			// send current plob
			LOG_DEBUG("send current plob to client %s", client->net->addr);
			mtx.id = REM_MSG_ID_IFS_CAP;
			mtx.ba = priv_serialize(
						server, client, mtx.id, server->cap, NULL);
			rem_net_client_txmsg(client->net, &mtx);
			g_byte_array_free(mtx.ba, TRUE);
			
			// send playlist
			if (server->pinfo->features & REM_FEATURE_PLAYLIST) {
				LOG_DEBUG("send playlist to client %s", client->net->addr);
				mtx.id = REM_MSG_ID_IFS_PLAYLIST;
				mtx.ba = priv_serialize(
							server, client, mtx.id, server->playlist, NULL);
				rem_net_client_txmsg(client->net, &mtx);
				g_byte_array_free(mtx.ba, TRUE);
			}
			
			// send queue
			if (server->pinfo->features & REM_FEATURE_QUEUE) {
				LOG_DEBUG("send queueu to client %s", client->net->addr);
				mtx.id = REM_MSG_ID_IFS_QUEUE;
				mtx.ba = priv_serialize(
							server, client, mtx.id, server->queue, NULL);
				rem_net_client_txmsg(client->net, &mtx);
				g_byte_array_free(mtx.ba, TRUE);
			}
			
		} else {
			
			priv_handle_pimsg(server, client, mrx);
			
		}
		
		return TRUE;
		
	} else if (cond & G_IO_NVAL) {
		
		/* Actually we should not be here. If this client channel has been
		 * shut down by us, then because we disconnect the client and this
		 * always happens via priv_disconnect_client() where also the client's
		 * io watch source gets removed from the main context. So we only can be
		 * here if some external entity has shut down the channel .. is this
		 * possible? TODO
		 */

		LOG_BUG("G_IO_NVAL (socket closed)");

		return FALSE;
		
	} else if (cond & G_IO_HUP) { // prob. client disconnected

		LOG_DEBUG("G_IO_HUP (client disconnected)");
		
		g_hash_table_remove(server->clients, chan);
		
		return FALSE;
		
	} else if (cond & G_IO_ERR) { // error
		
		LOG_DEBUG("G_IO_ERR");
		
		LOG_WARN("connection with client %s has errors", client->net->addr);

		g_hash_table_remove(server->clients, chan);
		
		return FALSE;
		
	} else {
		
		g_assert_not_reached();
		
		return FALSE;
		
	}

}

/**
 * When does this function get called with which cond.
 * NVAL:
 * 	- server shutdown by rem_net_server_destroy
 *		- wird nu in rem_server_down() und hier aufgerufen
 * 			- rem_server_down() source wird automatisch entfernt
 * 			- source wird entfernt
 *  - also nicht möglich
 */
static gboolean
priv_iocb_server(GIOChannel *chan, GIOCondition cond, gpointer data)
{
	RemServer		*server;
	RemClient		*client;
	gint			ret;
	GSource			*src;

	server = (RemServer*) data;
	
	if (cond == G_IO_IN) { // client requests connection
	
		LOG_DEBUG("G_IO_IN");

		client = g_slice_new0(RemClient);
		
		// accept the client
		client->net = rem_net_client_accept(server->net_server);
		if (!client->net) {
			g_free(client);
			return TRUE;
		}
	
		// send hello message
		ret = rem_net_client_hello(client->net);
		if (ret < 0) {
			LOG_WARN("sending hello code failed");
			rem_net_client_destroy(client->net);
			g_slice_free(RemClient, client);
		}
		
		// register the client
		g_hash_table_insert(server->clients, client->net->chan, client);
		
		// watch the client in the given main context
		src = g_io_create_watch(client->net->chan, G_IO_INERRHUPNVAL);
		g_source_set_callback(src, (GSourceFunc) &priv_iocb_client, server, NULL);
		client->src_id = g_source_attach(src, server->mc);
		g_source_unref(src); // g_source_attach increases refcount
		
		return TRUE;
		
	} else { // some error
		
		LOG_DEBUG("G_IO_HUP|ERR|NVAL|?? (%u)", cond);
		
		LOG_ERROR("error on server socket");

		server->pp_cb->notify(server->pp_priv, REM_SERVER_EVENT_ERROR);
		
		return FALSE;
		
	}
	
}

//////////////////////////////////////////////////////////////////////////////
//
// public interface for player proxies
//
//////////////////////////////////////////////////////////////////////////////

RemServer*
rem_server_up(const RemPPDescriptor *pp_desc,
			  const RemPPCallbacks *pp_callbacks,
			  const RemPPPriv *pp_priv,
			  GError **err)
{
	RemServer	*server;
	GSource		*src;

	rem_log_init(REM_LL_DEBUG); // calls rem_create_needed_dirs()
	
	rem_api_check(pp_callbacks && pp_desc && pp_priv,
			"arguments must not be NULL");
	rem_api_check(concl(err, !(*err)), "*err is not NULL");
	
	////////// init server struct //////////

	server = g_slice_new0(RemServer);
	
	server->pinfo = priv_create_player_info(pp_desc, pp_callbacks);
	if (!server->pinfo) {
		rem_api_check(server->pinfo,
				"invalid PP descriptor and/or callbacks configuration");
	}
	
	server->pp_cb = g_memdup(pp_callbacks, sizeof(RemPPCallbacks));
	server->pp_priv = (RemPPPriv*) pp_priv;

	server->mc = g_main_context_default();

	////////// charset //////////
	
	server->charset = g_strdup(pp_desc->charset ? pp_desc->charset : "UTF-8"); 
	
	LOG_DEBUG("using charset: %s", server->charset);
	
	////////// set up a server (io channel) //////////
	
	server->net_server = rem_net_server_new();
	if (!server->net_server) {
		g_set_error(err, 0, 0, "setting up the server failed");
		priv_server_free(server);
		return NULL;
	}

	////////// watch server in given main context //////////
	
	src = g_io_create_watch(server->net_server->chan, G_IO_INERRHUPNVAL);
	g_source_set_callback(src, (GSourceFunc) &priv_iocb_server, server, NULL);
	g_source_attach(src, server->mc);
	g_source_unref(src); // g_source_attach increases refcount
	
	////////// hash table to keep clients //////////

	server->clients = g_hash_table_new_full(&g_direct_hash,
											&g_direct_equal,
											NULL,
											&priv_disconnect_client);
	
	////////// other //////////
	
	server->pstatus		= rem_player_status_new();
	
	server->pstatus_fp	= rem_player_status_fp_new();
	
	server->playlist	= rem_ploblist_new(REM_PLOBLIST_PLID_PLAYLIST,
										   REM_PLOBLIST_NAME_PLAYLIST);
	server->queue		= rem_ploblist_new(REM_PLOBLIST_PLID_QUEUE,
										   REM_PLOBLIST_NAME_QUEUE);
	
	server->net_msg_rx	= rem_net_msg_new();
	
	LOG_INFO("server started");
	
	return server;

}

void
rem_server_notify(RemServer *server)
{
	GSource	*src;
	
	rem_api_check(server, "server is NULL");

	if (server->pending_sync) {	// already called
		LOG_NOISE("already got notification");
		return;
	}
	
	server->pending_sync = TRUE;
	
	src = g_idle_source_new();
	
	g_source_set_priority(src, G_PRIORITY_DEFAULT_IDLE);
	g_source_set_callback(src, &priv_cb_notify, server, NULL);
	
	g_source_attach(src, server->mc);
	g_source_unref(src);
	
	LOG_DEBUG("got notification, will process it on idle");
}

void
rem_server_poll(RemServer *server)
{
	GSource *src;
	
	rem_api_check(server, "server is NULL");
	
	if (server->poll) return;

	LOG_DEBUG("start polling");
	
	server->poll = TRUE;
	
	src = g_timeout_source_new(2000);
	g_source_set_priority(src, G_PRIORITY_HIGH_IDLE);
	g_source_set_callback(src, &priv_cb_poll, server, NULL);

	g_source_attach(src, server->mc);
	g_source_unref(src); // attach increases ref-count
}

void
rem_server_down(RemServer* server)
{
	GSource	*src;
	
	rem_api_check(server, "server is NULL");
	// if this func gets called more than one, 'server' might already be freed,
	// so here is a segfault possible (also during the following check :/)
	rem_api_check(!server->pending_down, "already called");
	
	server->pending_down = TRUE;
	
	src = g_idle_source_new();
	g_source_set_priority(src, G_PRIORITY_HIGH);
	g_source_set_callback(src, &priv_cb_down, server, NULL);

	// attach the source to the main context which was the default when
	// rem_server_up() has been called (attach increases ref count of source)
	g_source_attach(src, server->mc);
	g_source_unref(src);	
}

