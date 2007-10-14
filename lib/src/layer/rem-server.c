#include <remuco.h>

#include "rem-net.h"
#include "../data/rem-data.h"
#include "../util/rem-util.h"

#define G_IO_INERRHUPNVAL	(G_IO_IN | G_IO_ERR | G_IO_HUP | G_IO_NVAL)

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
	
	////////// main loop and context and sources //////////
	
	GMainLoop			*ml;
	GMainContext		*mc;
	GSource				*src_notify;
	GSource				*src_down;
	
	////////// communication related fields //////////
	
	RemNetServer		*net_server;
	GHashTable			*clients;
	RemNetMsg			*net_msg_rx;	// reuse to avoid avoidable reallocations
	RemNetMsg			net_msg_tx;		// note: this is no pointer
	gchar				*charset;
	
	////////// the player proxy //////////
	
	RemPPCallbacks		*pp_cb;
	RemPPPriv			*pp_priv;
	gboolean			pp_notifies_changes;
	
	////////// player related fields //////////
	
	RemPlayerInfo		*pinfo;
	RemPlayerStatus		*pstatus;
	RemPlayerStatusPriv	pstatus_priv;
	RemPlob				*cap;
	RemPloblist			*playlist;
	guint				playlist_hash;
	RemPloblist			*queue;
	guint				queue_hash;
	RemLibrary			*lib;
	RemNotifyFlags	changes;
};

typedef struct {
	rem_net_client_t	*net;
	RemClientInfo		*info;
	guint				src_id;
} RemClient;

//////////////////////////////////////////////////////////////////////////////
//
// private functions: IO and serialization
//
//////////////////////////////////////////////////////////////////////////////

static GByteArray*
priv_serialize(const RemServer *rem,
			   const RemClient *client,
			   RemMsgID msg_id,
			   const gpointer data,
			   gboolean *client_independent)
{
	g_assert_debug(rem && client);
	
	GByteArray				*ba = NULL;
	
	const RemPloblist	*pl;
	const RemLibrary		*lib;
	const RemPlob		*plob;
	const RemPlayerInfo		*pinfo;
	const RemPlayerStatusPriv			*psi;
	
	if (!data) return NULL;
	
	if (client_independent) *client_independent = TRUE;
	
	switch (msg_id) { // messages from server

	case REM_MSG_ID_IFS_CAP:
	case REM_MSG_ID_REQ_PLOB:
	
		plob = (RemPlob*) data;
		
		ba = rem_plob_serialize(plob, rem->charset, client->info->charsets,
				client->info->img_width, client->info->img_height);
		
		break;
		
	case REM_MSG_ID_IFS_PLAYLIST:
	case REM_MSG_ID_IFS_QUEUE:
	case REM_MSG_ID_REQ_PLOBLIST:
	case REM_MSG_ID_REQ_SEARCH:
	
		pl = (RemPloblist*) data;
		
		ba = rem_ploblist_serialize(pl, rem->charset, client->info->charsets);
		
		if (client_independent) *client_independent = FALSE;

		break;
		
	case REM_MSG_ID_IFS_PINFO:
	
		pinfo = (RemPlayerInfo*) data;
		
		ba = rem_player_info_serialize(pinfo, rem->charset, client->info->charsets);
		
		break;
		
	case REM_MSG_ID_IFS_STATUS:
				
		psi = (RemPlayerStatusPriv*) data;
		
		ba = rem_player_status_priv_serialize(psi, rem->charset, client->info->charsets);
				
		break;
		
	case REM_MSG_ID_IFS_SRVDOWN:
	
		break;
		
	case REM_MSG_ID_REQ_LIBRARY:
	
		lib = (RemLibrary*) data;
		
		ba = rem_library_serialize(lib, rem->charset, client->info->charsets);
		
		if (client_independent) *client_independent = FALSE;

		break;
	
	default:
	
		g_assert_not_reached();
		
		break;
	}
	
	return ba;
}

/**
 * Hashtable 'for-each' callback function to broadcast player chagnes
 * to clients. What this function sends depends on server->net_msg_tx.id.
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
	
	switch (server->net_msg_tx.id) {
		case REM_MSG_ID_IFS_STATUS:
			tx_data = &server->pstatus_priv;
		break;
		case REM_MSG_ID_IFS_CAP:
			tx_data = server->cap;
		break;
		case REM_MSG_ID_IFS_PLAYLIST:
			data = server->playlist;
		break;
		case REM_MSG_ID_IFS_QUEUE:
			tx_data = server->queue;
		break;
		case REM_MSG_ID_IFS_SRVDOWN:
			tx_data = NULL;
		break;
		default:
			g_assert_not_reached_debug();
			return;
		break;
	}
	
	server->net_msg_tx.ba = priv_serialize(server, client,
								server->net_msg_tx.id, tx_data, NULL);
	
	rem_net_client_txmsg(client->net, &server->net_msg_tx);
	
	if (server->net_msg_tx.ba) {
		g_byte_array_free(server->net_msg_tx.ba, TRUE);
		server->net_msg_tx.ba = NULL;
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
	RemPlob		*plob;
	
	rem_ploblist_clear(pl);
	
	// add the plobs to the ploblist
	
	title = g_string_new("");
	
	rem_sl_iterator_reset(pids);
	
	while((pid = rem_sl_iterator_next(pids))) {
		
		plob = s->pp_cb->get_plob(s->pp_priv, pid);
		if (!plob) plob = rem_plob_new_unknown(pid);
			
		s1 = rem_plob_meta_get(plob, REM_PLOB_META_TITLE);
		s2 = rem_plob_meta_get(plob, REM_PLOB_META_ARTIST);
		
		g_string_truncate(title, 0);
		g_string_append_printf(title, "%s - %s", s1, s2);
		
		rem_plob_destroy(plob);
		
		rem_ploblist_append_const(pl, pid, title->str);
		
	}
	
	g_string_free(title, TRUE);
	
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
			LOG_WARN("library request from pp returned NULL\n");
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
							(RemSimpleControlCommand) sctrl->cmd, sctrl->param);
		
		rem_simple_control_destroy(sctrl);
		
		break;
			
	case REM_MSG_ID_REQ_SEARCH:
		
		LOG_WARN("search not yet implemented\n");
		//plob = rem_plob_unserialize(msg->ba, server->charset);			
		break;
		
	case REM_MSG_ID_CTL_UPD_PLOB:
			
		LOG_WARN("update plob not yet implemented\n");
		//plob = rem_plob_unserialize(msg->ba, server->charset);			
		break;
			
	case REM_MSG_ID_CTL_UPD_PLOBLIST:

		LOG_WARN("update ploblist not yet implemented\n");
		//pl = rem_ploblist_unserialize(msg->ba, server->charset);
		break;
			
	default:
		
		LOG_WARN("unsupported message id\n");
		break;
	}

	if (msg_response.id) {
		
		rem_net_client_txmsg(client->net, &msg_response);
		
		if (msg_response.ba) g_byte_array_free(msg_response.ba, TRUE);
	}
	
}

/**
 * Checks server->changes if some player data changed and broadcasts the changes
 * to the clients. However, if there is a change according to server->changes,
 * we check for changes again anyway. This is needed when the server does not
 * get notified by the PP and must therefore check for changes itself. Further,
 * these change checks are not very expensive and therefore protect against
 * bad PP implementations which announce changes too often.
 */
static void
priv_handle_playerchanges(RemServer* server)
{
	gchar			*cap_pid;
	RemStringList	*pids;
	guint			hash;
	gboolean		change_status = FALSE,
					change_cap = FALSE,
					change_playlist = FALSE,
					change_queue = FALSE;
	
	////////// status change ? //////////
	
	if (server->changes & REM_NF_STATUS_CHANGED) {
		
		server->pp_cb->get_player_status(server->pp_priv, server->pstatus);
	
		////////// check if status (w/o plob) really changed //////////
		
		change_status = rem_player_status_priv_assign(
							&server->pstatus_priv, server->pstatus);
		
		////////// check if cap really changed //////////

		cap_pid = server->pstatus->cap_pid->str;
		
		// check if cap _really_ changed (we don't trust PPs ;)
		if ((cap_pid == NULL && server->cap != NULL) ||
			(cap_pid != NULL && server->cap == NULL) ||
			!g_str_equal(cap_pid, server->cap->pid)) {
			
			change_cap = TRUE;
			
			rem_plob_destroy(server->cap);

			if (cap_pid) {
				server->cap = server->pp_cb->get_plob(server->pp_priv, cap_pid);
			} else {
				server->cap = NULL;
			}
		}
	}
	
	////////// playlist change? //////////

	if (server->changes & REM_NF_PLAYLIST_CHANGED) {
		
		pids = server->pp_cb->get_playlist(server->pp_priv);
		if (!pids) {
			LOG_ERROR("got null playlist\n");
		} else {
			// check if playlist _really_ changed (we don't trust PPs ;)
			hash = rem_sl_hash(pids);
			if (hash != server->playlist_hash) {
				server->playlist_hash = hash;
				priv_build_ploblist(server, server->playlist, pids);
				change_playlist = TRUE;
			}
			rem_sl_destroy(pids);
		}
	}
		
	////////// what says PP about queue change? //////////

	if (server->changes & REM_NF_QUEUE_CHANGED) {
		
		pids = server->pp_cb->get_queue(server->pp_priv);
		if (!pids) {
			LOG_ERROR("got null queue\n");
		} else {
			// check if queue _really_ changed (we don't trust PPs ;)
			hash = rem_sl_hash(pids);
			if (hash != server->queue_hash) {
				server->queue_hash = hash;
				priv_build_ploblist(server, server->queue, pids);
				change_queue = TRUE;
			}
			rem_sl_destroy(pids);
		}
	}
	
	////////// transmit changes to clients //////////

	if (change_status) {
		LOG_DEBUG("send new player status");
		server->net_msg_tx.id = REM_MSG_ID_IFS_STATUS;
		g_hash_table_foreach(server->clients, &priv_htcb_tx, server);
	}
	if (change_cap) {
		LOG_DEBUG("send new cap");
		server->net_msg_tx.id = REM_MSG_ID_IFS_CAP;
		g_hash_table_foreach(server->clients, &priv_htcb_tx, server);
	}
	if (change_playlist) {
		LOG_DEBUG("send new playlist");
		server->net_msg_tx.id = REM_MSG_ID_IFS_PLAYLIST;
		g_hash_table_foreach(server->clients, &priv_htcb_tx, server);
	}
	if (change_queue) {
		LOG_DEBUG("send new queue");
		server->net_msg_tx.id = REM_MSG_ID_IFS_QUEUE;
		g_hash_table_foreach(server->clients, &priv_htcb_tx, server);
	}
}

//////////////////////////////////////////////////////////////////////////////
//
// private functions: misc
//
//////////////////////////////////////////////////////////////////////////////

static RemPlayerInfo*
priv_get_player_info(const RemPlayerProxy *pp)
{
	RemPlayerInfo			*pinfo;
	RemPlayerInfoFeature	features = 0;

	////////// check for player name //////////
	
	if (!pp->desc->player_name) {
		LOG_ERROR("no player name given\n");
		return NULL;
	}

	////////// mandatory callback functions //////////
	
	if (!pp->callbacks->get_player_status) {
		LOG_ERROR("mandatory callback function 'get_player_status' not set\n");
		return NULL;
	}
	if (!pp->callbacks->get_plob) {
		LOG_ERROR("mandatory callback function 'get_plob' not set\n");
		return NULL;
	}
	if (!pp->callbacks->notify_error) {
		LOG_ERROR("mandatory callback function 'notify_error' not set\n");
		return NULL;
	}
	if (!pp->callbacks->simple_ctrl) {
		LOG_ERROR("mandatory callback function 'simple_ctrl' not set\n");
		return NULL;
	}
	
	////////// optional callback functions //////////
	
	if (pp->callbacks->get_library) {
		features |= REM_FEATURE_LIBRARY;
		LOG_DEBUG("Player/PP supports library\n");
	}
	if (pp->callbacks->get_playlist) {
		features |= REM_FEATURE_PLAYLIST;
		LOG_DEBUG("Player/PP supports playlist\n");
	}
	if (pp->callbacks->get_ploblist) {
		features |= REM_FEATURE_LIBRARY_PL_CONTENT;
		LOG_DEBUG("Player/PP can give us content of any ploblists\n");
	}
	if (pp->callbacks->get_queue) {
		features |= REM_FEATURE_QUEUE;
		LOG_DEBUG("Player/PP supports queue\n");
	}
	if (pp->callbacks->play_ploblist) {
		features |= REM_FEATURE_LIBRARY_PL_PLAY;
		LOG_DEBUG("Player/PP supports playing a certain ploblist\n");
		if (!pp->callbacks->get_library) {
			LOG_ERROR("You should set callback function 'get_library' too!\n");
			return NULL;
		}
	}
	if (pp->callbacks->search) {
		features |= REM_FEATURE_SEARCH;
		LOG_DEBUG("Player/PP supports plob search\n");
	}
	if (pp->callbacks->update_plob) {
		features |= REM_FEATURE_PLOB_EDIT;
		LOG_DEBUG("Player/PP supports editing plobs\n");
	}
	if (pp->callbacks->update_ploblist) {
		//features |= REM_FEATURE_PL ???;
		LOG_DEBUG("Player/PP supports editing plobs\n");
	}
	
	////////// rating //////////
	
	if (pp->desc->max_rating_value) {
		features |= REM_FEATURE_RATE;
	}
	
	////////// repeat modes //////////
	
	if (pp->desc->supported_repeat_modes & REM_PS_REPEAT_MODE_ALBUM) {
		features |= REM_FEATURE_REPEAT_MODE_ALBUM;
	}
	if (pp->desc->supported_repeat_modes & REM_PS_REPEAT_MODE_PL) {
		features |= REM_FEATURE_REPEAT_MODE_PL;
	}
	if (pp->desc->supported_repeat_modes & REM_PS_REPEAT_MODE_PLOB) {
		features |= REM_FEATURE_REPEAT_MODE_PLOB;
	}
	
	////////// misc //////////

	if (pp->desc->supported_shuffle_modes & REM_PS_SHUFFLE_MODE_ON) {
		features |= REM_FEATURE_SHUFFLE_MODE;
	}
	
	if (pp->desc->supports_jump_playlist) {
		features |= REM_FEATURE_PLAYLIST_JUMP;
		if (!pp->callbacks->get_playlist) {
			LOG_ERROR("Playlist jump enabled -> you should enable callback "
					"function 'get_playlist' too!\n");
			return NULL;
		}
	}
	if (pp->desc->supports_jump_queue) {
		features |= REM_FEATURE_QUEUE_JUMP;
		if (!pp->callbacks->get_queue) {
			LOG_ERROR("Queue jump enabled -> you should enable callback "
					"function 'get_playlist' too!\n");
			return NULL;
		}
	}
	
	if (pp->desc->supports_seek) {
		features |= REM_FEATURE_SEEK;
	}
	
	if (pp->desc->supports_tags) {
		features |= REM_FEATURE_PLOB_TAGS;
	}

	pinfo = rem_player_info_new(pp->desc->player_name);
	pinfo->features = features;
	pinfo->rating_max = pp->desc->max_rating_value;

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
	
	RemClient *client = (RemClient*) data;

	g_source_remove(client->src_id);
	rem_net_client_destroy(client->net);
	rem_client_info_destroy(client->info);
	
	g_slice_free(RemClient, client);
}

/**
 * Callback function to handle changes in player state.
 * 
 * This function may be the callback of
 * 	- a timeout source if the pp does not notify us about changes so that a
 * 	  repeating timeout causes the periodical check for player changes
 *	- an idle source which has been activated when the pp called rem_server_notify()
 * 
 * We do the work related to the public funtions of the remuco lib in callback
 * functions to make sure that everything by the remuco lib is done in the
 * same main context. 
 */  
static gboolean
priv_cb_notify(gpointer data)
{
	RemServer	*rem;
	
	rem = (RemServer*) data;
	
	priv_handle_playerchanges(rem);
	
	if (rem->pp_notifies_changes) {
		
		rem->changes = 0;
		
		g_source_unref(rem->src_notify);
		rem->src_notify = NULL;
		
		return FALSE;
		
	} else { // we check player changes periodically with a timer 
		
		rem->changes = REM_NF_ALL_CHANGED;
		
		return TRUE;
	}
}

static gboolean
priv_cb_down(gpointer data)
{
	RemServer	*server;
	
	server = (RemServer*) data;
	
	// disconnect the clients:
	g_hash_table_destroy(server->clients);
	
	// shut down the server:
	rem_net_server_destroy(server->net_server);

	// remove all our sources from main context
	while(g_source_remove_by_user_data(server));

	// free the sources we created
	if (server->src_notify)
		g_source_unref(server->src_notify);
	if (server->src_down)
	g_source_unref(server->src_down);
	
	// free all player state info data:
	rem_player_info_destroy(server->pinfo);
	rem_player_status_destroy(server->pstatus);
	rem_plob_destroy(server->cap);
	rem_ploblist_destroy(server->playlist);
	rem_ploblist_destroy(server->queue);
	rem_library_destroy(server->lib);
	
	g_source_unref(server->src_down);
	server->src_down = NULL;

	// if needed, stop the main loop:
	if (server->ml) {
		g_main_loop_quit(server->ml);
		g_main_loop_unref(server->ml);
	}
	
	rem_net_msg_destroy(server->net_msg_rx);
	
	g_free(server->charset);
	
	g_slice_free(RemServer, server);
	
	return FALSE;
	
}

//////////////////////////////////////////////////////////////////////////////
//
// private functions: handle net (IO) events
//
//////////////////////////////////////////////////////////////////////////////

/**
 * IO callback function for a client when we wait for it's client info.
 */ 
static gboolean
priv_iocb_client(GIOChannel *chan, GIOCondition cond, gpointer data)
{
	RemServer	*server;
	RemClient	*client;
	gint		ret;
	RemNetMsg	msg;
	
	server = (RemServer*) data;
	
	client = g_hash_table_lookup(server->clients, chan);
	
	g_assert(client); // this assures we remove clients from the hashtable only
					  // here, where we return FALSE if we removed a client
	
	if (cond & G_IO_IN) {
	
		LOG_DEBUG("G_IO_IN\n");
		
		msg.id = 0;
		msg.ba = NULL;

		ret = rem_net_client_rxmsg(client->net, &msg);
		if (ret < 0) {
			g_hash_table_remove(server->clients, chan);
			return FALSE;
		}
		
		if (msg.id == REM_MSG_ID_IFC_CINFO) {
			
			// check if there already is a client info:
			if (client->info) {
				LOG_WARN("rx'ed client info, but already have it");
				g_byte_array_free(msg.ba, TRUE);
				g_hash_table_remove(server->clients, chan);
				return FALSE;
			}
			
			// unserialize the client info
			client->info = rem_client_info_unserialize(msg.ba, server->charset);
			
			// send player info
			LOG_DEBUG("send player info to client %s\n", client->net->addr);
			msg.id = REM_MSG_ID_IFS_PINFO;
			msg.ba = priv_serialize(
						server, client, msg.id, server->pinfo, NULL);
			rem_net_client_txmsg(client->net, &msg);
			g_byte_array_free(msg.ba, TRUE);
			
			// send player status
			LOG_DEBUG("send player status to client %s\n", client->net->addr);
			msg.id = REM_MSG_ID_IFS_STATUS;
			msg.ba = priv_serialize(server, client, msg.id, server->pstatus, NULL);
			rem_net_client_txmsg(client->net, &msg);
			g_byte_array_free(msg.ba, TRUE);
			
			// send current plob
			LOG_DEBUG("send current plob to client %s\n", client->net->addr);
			msg.id = REM_MSG_ID_IFS_CAP;
			msg.ba = priv_serialize(
						server, client, msg.id, server->cap, NULL);
			rem_net_client_txmsg(client->net, &msg);
			g_byte_array_free(msg.ba, TRUE);
			
			// send playlist
			if (server->pinfo->features & REM_FEATURE_PLAYLIST) {
				LOG_DEBUG("send playlist to client %s\n", client->net->addr);
				msg.id = REM_MSG_ID_IFS_PLAYLIST;
				msg.ba = priv_serialize(
							server, client, msg.id, server->playlist, NULL);
				rem_net_client_txmsg(client->net, &msg);
				g_byte_array_free(msg.ba, TRUE);
			}
			
			// send queue
			if (server->pinfo->features & REM_FEATURE_QUEUE) {
				LOG_DEBUG("send queueu to client %s\n", client->net->addr);
				msg.id = REM_MSG_ID_IFS_QUEUE;
				msg.ba = priv_serialize(
							server, client, msg.id, server->queue, NULL);
				rem_net_client_txmsg(client->net, &msg);
				g_byte_array_free(msg.ba, TRUE);
			}
			
		} else {
			
			priv_handle_pimsg(server, client, &msg);
			
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

		LOG_DEBUG("G_IO_NVAL (socket closed)\n");

		g_return_val_if_reached(FALSE);
		
		return FALSE;
		
	} else if (cond & G_IO_HUP) { // prob. client disconnected

		LOG_DEBUG("G_IO_HUP\n");
		
		g_hash_table_remove(server->clients, chan);
		
		return FALSE;
		
	} else if (cond & G_IO_ERR) { // error
		
		LOG_DEBUG("G_IO_ERR\n");
		
		LOG_WARN("connection with client %s has errors\n", client->net->addr);

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

	server = (RemServer*) data;
	
	if (cond == G_IO_IN) { // client requests connection
	
		LOG_DEBUG("G_IO_IN\n");

		client = g_slice_new0(RemClient);
		
		// accept the client
		client->net = rem_net_client_accept(server->net_server);
		if (!client->net) {
			g_free(client);
			return TRUE;
		}
	
		// register the client
		g_hash_table_insert(server->clients, chan, client);
		
		// watch the client
		client->src_id = g_io_add_watch(client->net->chan,
										G_IO_INERRHUPNVAL,
										&priv_iocb_client,
										server);
	
		return TRUE;
		
	} else if (cond == G_IO_NVAL) { // channel has been shut down (prob. by us)
	
		LOG_DEBUG("G_IO_NVAL\n");
		
		// TODO signalize error to pp

		g_return_val_if_reached(FALSE);
		
	} else { // some error
		
		LOG_DEBUG("G_IO_HUP|ERR|?? (%u)\n", cond);
		
		LOG_ERROR("error on server socket\n");

		// TODO signalize error to pp
		
		return FALSE;
		
	}
	
}

//////////////////////////////////////////////////////////////////////////////
//
// public interface for player proxies
//
//////////////////////////////////////////////////////////////////////////////

RemServer*
rem_server_up(const RemPlayerProxy *pp, GError **err)
{
	RemServer				*server;
	G_CONST_RETURN gchar	*charset_locale;

	g_return_val_if_fail(pp && pp->callbacks && pp->desc && pp->priv, NULL);
	g_return_val_if_fail(concl(err, !(*err)), NULL);
	
	////////// set up server struct //////////

	server = g_slice_new0(RemServer);
	
	server->pinfo = priv_get_player_info(pp);
	g_return_val_if_fail(server->pinfo, NULL);

	server->pp_cb = pp->callbacks;
	server->pp_priv = pp->priv;
		
	if (!pp->desc->charset) {
		g_get_charset(&charset_locale);
		server->charset = g_strdup(charset_locale);
	} else {
		server->charset = g_strdup(pp->desc->charset);		
	}
	LOG_DEBUG("used charset: %s\n", server->charset);
	
	server->pp_notifies_changes = pp->desc->notifies_changes;

	server->pstatus = rem_player_status_new();
	
	////////// set up a server (channel) //////////
	
	server->net_server = rem_net_server_new();
	if (!server->net_server) {
		g_set_error(err, 0, 0, "setting up the server failed");
		g_free(server);
		return NULL;
	}

	////////// hash table to keep clients //////////

	server->clients = g_hash_table_new_full(&g_direct_hash,
										 &g_direct_equal,
										 NULL,
										 &priv_disconnect_client);
	
	////////// what to do on server activity //////////
	
	g_io_add_watch(server->net_server->chan,
				   G_IO_INERRHUPNVAL,
				   &priv_iocb_server,
				   server);
	
	////////// if needed, periodically check for player changes //////////

	if (!server->pp_notifies_changes)
		g_timeout_add_full(G_PRIORITY_HIGH_IDLE,
						   2000,
						   &priv_cb_notify,
						   server,
						   NULL);
	
	LOG_INFO("server started\n");
	
	////////// main loop and context management //////////
	
	if (pp->desc->run_main_loop) {
		
		LOG_DEBUG("running main loop (blocking)\n");
		
		server->ml = g_main_loop_new(NULL, FALSE);
		g_main_loop_run(server->ml);
	}
	
	server->mc = g_main_context_default();
	
	////////// other //////////
	
	server->net_msg_rx = rem_net_msg_new();
	
	return server;

}

void
rem_server_notify(RemServer* server, RemNotifyFlags flags)
{
	
	g_return_if_fail(server);
	g_return_if_fail(concl(
			flags & REM_NF_PLAYLIST_CHANGED, server->pp_cb->get_playlist));
	g_return_if_fail(concl(
			flags & REM_NF_QUEUE_CHANGED, server->pp_cb->get_queue));

	server->changes |= flags;	// remember all changes - this func may get called
							// multiple times before the changes get processed

	if (server->src_notify)	// already called
		return;
	
	server->src_notify = g_idle_source_new();
	g_source_set_priority(server->src_notify, G_PRIORITY_DEFAULT_IDLE);
	g_source_set_callback(server->src_notify, &priv_cb_notify, server, NULL);
	
	// attach the source to the main context which was the default when
	// rem_server_up() has been called
	g_source_attach(server->src_notify, server->mc);
}

void
rem_server_down(RemServer* server)
{
	g_return_if_fail(server);
	
	// if this func gets called more than one, 'rem' might already be freed, so
	// in theory, here is a segfault possible (also during the following check)
	g_return_if_fail(!server->src_down);
	
	server->src_down = g_idle_source_new();
	g_source_set_priority(server->src_down, G_PRIORITY_HIGH);
	g_source_set_callback(server->src_down, &priv_cb_down, server, NULL);

	// attach the source to the main context which was the default when
	// rem_server_up() has been called
	g_source_attach(server->src_down, server->mc);
	
}


