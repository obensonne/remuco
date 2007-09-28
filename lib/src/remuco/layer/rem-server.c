///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include "../util/rem-common.h"
#include "../data/rem-data.h"
#include "rem-comm.h"
#include "rem-server.h"
#include "../pp.h"

#define REM_PLOBLIST_PLID_SEARCH	"__SEARCH__"
#define REM_PLOBLIST_NAME_SEARCH	"Search Result"

///////////////////////////////////////////////////////////////////////////////
//
// types and structs
//
///////////////////////////////////////////////////////////////////////////////

struct _rem_server {
	rem_pinfo_t		*pinfo;
	rem_ps_t		*ps;
	rem_plob_t		*current_plob;
	rem_ploblist_t		*playlist;
	guint			playlist_hash;
	rem_ploblist_t		*queue;
	guint			queue_hash;
	rem_library_t		*ploblists;
	rem_ploblist_t		*search_result;
	rem_comm_t		*comm;
	GThread			*thread;
	rem_pp_t		*pp;
	gboolean		interrupted;
	gboolean		pp_notfies_server;
	rem_server_notify_flags	change_flags;
};

///////////////////////////////////////////////////////////////////////////////
//
// macros and constants
//
///////////////////////////////////////////////////////////////////////////////

#define INTERRUPTED(_pp) (!(_pp)->server)

#define TICK_INTERVAL	2

#define MUTEX_LOCK(_mutex)
#define MUTEX_UNLOCK(_mutex)

///////////////////////////////////////////////////////////////////////////////
//
// pirvate functions
//
///////////////////////////////////////////////////////////////////////////////

static void
rem_server_down_and_free(rem_server_t *s)
{
	if (!s) return;
	
	rem_comm_down(s->comm);
	rem_ps_destroy(s->ps);
	rem_plob_destroy(s->current_plob);
	rem_ploblist_destroy(s->playlist);
	rem_ploblist_destroy(s->queue);
	rem_ploblist_destroy(s->search_result);
	rem_library_destroy(s->ploblists);
	
	g_free(s);

}

/**
 * Given a list of PIDs, this function builds a ploblist, listing the related
 * plobs.
 */
static void
rem_server_build_ploblist(rem_server_t *s,
			  rem_ploblist_t *pl,
			  const rem_sv_t *pids)
{
	g_assert_debug(s && pl && pids);

	gchar		*pid, *title;
	const gchar	*s1, *s2;
	rem_plob_t	*plob;
	guint		u;
	
	rem_ploblist_clear(pl);
	
	// add the plobs to the ploblist
	
	for (u = 0; u < pids->l; ++u) {
		
		pid = pids->v[u];
		
		plob = pp_get_plob(s->pp, pid);
		if (!plob) plob = rem_plob_new_unknown(pid);
			
		s1 = rem_plob_meta_get(plob, REM_PLOB_META_TITLE);
		s2 = rem_plob_meta_get(plob, REM_PLOB_META_ARTIST);
		title = g_strdup_printf("%s - %s", s1, s2);
		
		rem_plob_destroy(plob);
		
		rem_ploblist_append(pl, g_strdup(pid), title);
		
		
	}
	
}

/**
 * Handles a message received from a client and interacts appropriately with
 * the player proxy to request player data or alter the player state.
 * If the message is a requests from the client, the requested data gets
 * transmitted back to the client.
 */
static void
rem_server_handle_msg(rem_server_t *s, guint cn)
{
	g_assert_debug(s);
	
	rem_strpar_t	*sp;
	rem_sv_t	*pids;
	rem_plob_t	*p;
	rem_ploblist_t	*pl;
	rem_sctrl_t	*sc;
	gint		i;
	const gchar	*str;
	
	rem_msg_t *msg, msg_response;
	
	msg = &(s->comm->cli[cn].msg);
	
	switch (msg->id) {
		case REM_MSG_ID_CTL_PLAY_PLOBLIST:
		
			sp = (rem_strpar_t*) msg->data;
			pp_play_ploblist(s->pp, sp->str);
			rem_strpar_destroy(sp);
			break;
			
		case REM_MSG_ID_CTL_SCTRL:
		
			sc = (rem_sctrl_t*) msg->data;
			pp_ctrl(s->pp, sc);
			rem_sctrl_destroy(sc);
			break;
			
		case REM_MSG_ID_CTL_UPD_PLOB:
		
			p = (rem_plob_t*) msg->data;
			pp_update_plob(s->pp, p);
			rem_plob_destroy(p);
			break;
			
		case REM_MSG_ID_CTL_UPD_PLOBLIST:
		
			pl = (rem_ploblist_t*) msg->data;
			pids = rem_ploblist_get_pids(pl);
			pp_update_ploblist(s->pp, pl->plid, pids);
			rem_sv_destroy(pids);
			rem_ploblist_destroy(pl);			
			break;
			
		case REM_MSG_ID_IGNORE:
			// ignore
			break;
			
		case REM_MSG_ID_REQ_PLOB:
		
			sp = (rem_strpar_t*) msg->data;
			rem_plob_t	*p;
			p = pp_get_plob(s->pp, sp->str);
			g_assert(p);
			msg_response.id = REM_MSG_ID_REQ_PLOB;
			msg_response.data = p;
			rem_comm_tx(s->comm, cn, &msg_response);
			rem_plob_destroy(p);
			rem_strpar_destroy(sp);
			break;
			
		case REM_MSG_ID_REQ_PLOBLIST:
		
			sp = (rem_strpar_t*) msg->data;
			i = rem_library_get_pos(s->ploblists, sp->str);
			if (i < 0) str = sp->str;
			else str = rem_library_get_name(s->ploblists, i);
			pl = rem_ploblist_new(g_strdup(sp->str), g_strdup(str));
			pids = pp_get_ploblist(s->pp, sp->str);
			g_assert(pids);
			rem_server_build_ploblist(s, pl, pids);
			msg_response.id = REM_MSG_ID_REQ_PLOBLIST;
			msg_response.data = pl;
			rem_comm_tx(s->comm, cn, &msg_response);
			rem_ploblist_destroy(pl);
			rem_sv_destroy(pids);
			rem_strpar_destroy(sp);
			break;
			
		case REM_MSG_ID_REQ_LIBRARY:
		
			LOG_DEBUG("got a lib req\n");
			rem_library_destroy(s->ploblists);
			s->ploblists = pp_get_library(s->pp);
			g_assert(s->ploblists);
			msg_response.id = REM_MSG_ID_REQ_LIBRARY;
			msg_response.data = s->ploblists;
			rem_comm_tx(s->comm, cn, &msg_response);
			break;
			
		case REM_MSG_ID_REQ_SEARCH:
		
			p = (rem_plob_t*) msg->data;
			pids = pp_search(s->pp, p);
			g_assert(pids);
			rem_server_build_ploblist(s, s->search_result, pids);
			msg_response.id = REM_MSG_ID_REQ_SEARCH;
			msg_response.data = pl;
			rem_comm_tx(s->comm, cn, &msg_response);
			rem_sv_destroy(pids);
			rem_plob_destroy(p);
			break;
			
		default:
		
			LOG_WARN("should not rx this message: %i\n", msg->id);
		
			break;
	}

	msg->data = NULL;
	msg->id = REM_MSG_ID_IGNORE;
}

static void
rem_server_update_ps(rem_server_t *s)
{
	LOG_NOISE("called\n");

	g_assert_debug(s);
	
	rem_msg_t	msg;
	const rem_ps_t	*ps;

	MUTEX_LOCK(s->mutex);
	
	if (s->interrupted) { MUTEX_UNLOCK(s->mutex); return; LOG_NOISE("return\n");}

	ps = pp_get_ps(s->pp);
	
	g_assert(ps);
	
	if (rem_ps_equal(ps, s->ps)) {
		MUTEX_UNLOCK(s->mutex);
		LOG_NOISE("return\n");	
		return;
	}

	LOG_DEBUG("ps changed\n");

	rem_ps_copy(ps, s->ps);

	msg.id = REM_MSG_ID_IFS_STATE;
	msg.data = s->ps;
		
	rem_comm_tx_bc(s->comm, &msg);

	MUTEX_UNLOCK(s->mutex);
		
	LOG_NOISE("return\n");	
}

static void
rem_server_update_playlist(rem_server_t *s)
{
	LOG_NOISE("called\n");
	
	rem_msg_t	msg;
	rem_sv_t	*pids;
	
	MUTEX_LOCK(s->mutex);
	
	if (s->interrupted) { MUTEX_UNLOCK(s->mutex); return; LOG_NOISE("return\n");}

	pids = pp_get_ploblist(s->pp, REM_PLOBLIST_PLID_PLAYLIST);

	g_assert(pids);

	if (pids->hash == s->playlist_hash) {
		MUTEX_UNLOCK(s->mutex);
		rem_sv_destroy(pids);
		LOG_NOISE("return\n");	
		return;
	}

	LOG_DEBUG("playlist changed\n");
	
	#if LOGLEVEL >= LL_DEBUG
	rem_sv_dump(pids);
	#endif
	
	s->playlist_hash = pids->hash;

	rem_server_build_ploblist(s, s->playlist, pids);
	rem_sv_destroy(pids);

	msg.id = REM_MSG_ID_IFS_PLAYLIST;
	msg.data = s->playlist;
	
	rem_comm_tx_bc(s->comm, &msg);

	MUTEX_UNLOCK(s->mutex);

	LOG_NOISE("return\n");	
}

static void
rem_server_update_queue(rem_server_t *s)
{
	LOG_NOISE("called\n");
	
	rem_msg_t	msg;
	rem_sv_t	*pids;
	
	MUTEX_LOCK(s->mutex);
	
	if (s->interrupted) { MUTEX_UNLOCK(s->mutex); return; LOG_NOISE("return\n");}

	pids = pp_get_ploblist(s->pp, REM_PLOBLIST_PLID_QUEUE);

	g_assert(pids);

	if (pids->hash == s->queue_hash) {
		MUTEX_UNLOCK(s->mutex);
		rem_sv_destroy(pids);
		LOG_NOISE("return\n");	
		return;
	}

	LOG_DEBUG("playlist changed\n");
	
	#if LOGLEVEL >= LL_DEBUG
	rem_sv_dump(pids);
	#endif
	
	s->queue_hash = pids->hash;

	rem_server_build_ploblist(s, s->queue, pids);
	rem_sv_destroy(pids);

	msg.id = REM_MSG_ID_IFS_QUEUE;
	msg.data = s->queue;
	
	rem_comm_tx_bc(s->comm, &msg);

	MUTEX_UNLOCK(s->mutex);

	LOG_NOISE("return\n");	
}

static void
rem_server_update_current_plob(rem_server_t *s)
{
	LOG_NOISE("called\n");
	
	rem_msg_t	msg;
	gchar		*cpid, *pid;

	MUTEX_LOCK(s->mutex);

	if (s->interrupted) { MUTEX_UNLOCK(s->mutex); return; LOG_NOISE("return\n");}

	pid = pp_get_current_plob_pid(s->pp);
	
	// quick check if current plob really changed
	cpid = s->current_plob ? s->current_plob->pid : NULL;
	if (cpid == pid || (cpid && pid && g_str_equal(cpid, pid))) {
		MUTEX_UNLOCK(s->mutex);
		g_free(pid);
		LOG_NOISE("return\n");	
		return;
	}

	LOG_DEBUG("current plob changed (to '%s')\n", pid);

	rem_plob_destroy(s->current_plob);
	
	s->current_plob = NULL;

	if (pid) {	
		s->current_plob = pp_get_plob(s->pp, pid);
		g_free(pid);
		#if LOGLEVEL >= LL_DEBUG
		rem_plob_dump(s->current_plob);
		#endif
	}
	
	msg.id = REM_MSG_ID_IFS_CURPLOB;
	msg.data = s->current_plob;
		
	rem_comm_tx_bc(s->comm, &msg);
	
	MUTEX_UNLOCK(s->mutex);
	
	LOG_NOISE("return\n");	
}

static void
rem_server_check_for_player_changes(rem_server_t *s)
{
	g_assert_debug(s);
	
	rem_server_notify_flags	flags;
	
	MUTEX_LOCK(s->mutex);

	if G_UNLIKELY(s->interrupted) { MUTEX_UNLOCK(s->mutex); return; }
	
	flags = s->change_flags;
	
	MUTEX_UNLOCK(s->mutex);	

	if (s->change_flags & REM_SERVER_NF_PS_CHANGED)
		rem_server_update_ps(s);
		
	if (s->change_flags & REM_SERVER_NF_PLOB_CHANGED)
		rem_server_update_current_plob(s);
		
	if (s->change_flags & REM_SERVER_NF_PLAYLIST_CHANGED)
		rem_server_update_playlist(s);
		
	if (s->change_flags & REM_SERVER_NF_QUEUE_CHANGED)
		rem_server_update_queue(s);
	
	
}

/**
 * Loops continuesly while recveiving data from clients and checking for player
 * state changes.
 */
static gpointer
rem_server_loop(gpointer data) {
	
	g_assert_debug(data);
	
	guint			u;
	gint			ret;
	rem_comm_client_t	*cli;
	rem_msg_t		msg;
	rem_server_t		*s;
	GError			*err;
	
	s = (rem_server_t*) data;
	
	// loop
	
	while G_UNLIKELY(!s->interrupted) {
		
	LOG_NOISE("loop: start\n");
	
	ret = rem_comm_listen(s->comm);
	if (ret < 0) {
		err = g_error_new(0, 0, "error in comm layer");
		pp_error(s->pp, err);
		break;
	}

	if G_UNLIKELY(s->interrupted) break;
	
	MUTEX_LOCK(s->mutex);
	
	if G_UNLIKELY(s->interrupted) { MUTEX_UNLOCK(s->mutex); break; }
	
	// check if clients sent us some data
	
	LOG_NOISE("loop: check clients\n");

	for (u = 0; u < REM_MAX_CLIENTS; u++) {

		cli = &s->comm->cli[u];
		
		switch (cli->state) {
			
		case REM_COMM_CS_DISCONNECTED:
			
			// nothing todo

			break;

		case REM_COMM_CS_CONNECTED:

			// handle the date received from the client
			
			if (cli->msg.id == REM_MSG_ID_IGNORE) break;				
			
			rem_server_handle_msg(s, u);
			
			break;

		case REM_COMM_CS_HANDSHAKE_CLI_WAITS_FOR_PINFO:
		
			// send all available current information about the
			// media app to the client
			
			g_assert_debug(cli->msg.id == REM_MSG_ID_IGNORE);
			
			LOG_DEBUG("send player info\n");
			
			msg.id = REM_MSG_ID_IFS_PINFO;
			msg.data = s->pinfo;
			rem_comm_tx(s->comm, u, &msg);
		
			cli->state = REM_COMM_CS_CONNECTED;
			
			LOG_DEBUG("send player state\n");

			msg.id = REM_MSG_ID_IFS_STATE;
			msg.data = s->ps;				
			rem_comm_tx(s->comm, u, &msg);
			
			LOG_DEBUG("send current plob\n");
			
			msg.id = REM_MSG_ID_IFS_CURPLOB;
			msg.data = s->current_plob;				
			rem_comm_tx(s->comm, u, &msg);
			
			if (s->pinfo->features & REM_PINFO_FEATURE_PLAYLIST) {
				LOG_DEBUG("send playlist\n");
				g_assert_debug(s->playlist);
				msg.id = REM_MSG_ID_IFS_PLAYLIST;
				msg.data = s->playlist;
				rem_comm_tx(s->comm, u, &msg);
			}
			
			if (s->pinfo->features & REM_PINFO_FEATURE_QUEUE) {
				LOG_DEBUG("send queue\n");
				g_assert_debug(s->queue);
				msg.id = REM_MSG_ID_IFS_QUEUE;
				msg.data = s->queue;
				rem_comm_tx(s->comm, u, &msg);
			}
			
			break;

		case REM_COMM_CS_HANDSHAKE_SRV_WAITS_FOR_CINFO:
			
			// nothing todo .. comm layer handles this
							
			break;

		default:
			g_assert_not_reached();
		
			break;
		}	
	}
	
	MUTEX_UNLOCK(s->mutex);
	
	LOG_NOISE("loop: set notify flags\n");

	if (!s->pp_notfies_server)
		rem_server_notify(s, REM_SERVER_NF_ALL_CHANGED);
	
	LOG_NOISE("loop: check for changes\n");

	rem_server_check_for_player_changes(s);
	
	LOG_NOISE("loop: end\n");	
	
	} /* while(pp->server) */
	
	LOG_NOISE("return\n");

	return NULL;
}

///////////////////////////////////////////////////////////////////////////////
//
// server interface (called by a player proxy)
//
///////////////////////////////////////////////////////////////////////////////

G_CONST_RETURN rem_server_t*
rem_server_start(const rem_pp_t *pp,
		 const rem_pinfo_t *pi,
		 gboolean pp_notifies_server,
		 GError **err)
{
	g_assert(pi && pi->name);
	g_assert(err && !(*err));
	
	rem_server_t	*s;
	
	s = g_malloc0(sizeof(rem_server_t));
	
	// set up the communication layer

	s->comm = rem_comm_up("UTF-8", TICK_INTERVAL);
	
	if (!s->comm) {
		
		g_free(s);
		*err = g_error_new(0, 0, "failed to init comm layer");
		return NULL;
	}
	
	// init some data
	
	s->pinfo = (rem_pinfo_t*) pi;
	s->pp_notfies_server = pp_notifies_server;
	s->pp = (rem_pp_t*) pp;
	
	s->playlist = rem_ploblist_new( g_strdup(REM_PLOBLIST_PLID_PLAYLIST),
					g_strdup(REM_PLOBLIST_NAME_PLAYLIST));
	s->queue = rem_ploblist_new(    g_strdup(REM_PLOBLIST_PLID_QUEUE),
					g_strdup(REM_PLOBLIST_NAME_QUEUE));
	s->search_result = rem_ploblist_new(
					g_strdup(REM_PLOBLIST_PLID_SEARCH),
					g_strdup(REM_PLOBLIST_NAME_SEARCH));
					
	s->ploblists = rem_library_new();
					
	s->ps = rem_ps_new();
	s->ps->state = REM_PS_STATE_OFF;
	
	// create new thread for server

	if (!g_thread_supported()) g_thread_init(NULL);
	
	s->thread = g_thread_create(rem_server_loop, s, TRUE, err);

	if (*err) {
		LOG_ERROR("failed to start server thread (%s)\n", (*err)->message);
		rem_server_down_and_free(s);
		return NULL;
	} else {
		LOG_INFO("server thread started\n");
		return s;
	}
	
}

void
rem_server_stop(rem_server_t *s)
{
	g_assert(s);
	
	if (s->interrupted) return; // s->interrupted only gets set here

	s->interrupted = TRUE;
	
	LOG_INFO("waiting for server thread to finish..\n");
	g_thread_join(s->thread);
	LOG_INFO("ok, server thread finished\n");
	
	rem_server_down_and_free(s);
}

void
rem_server_notify(rem_server_t *s, rem_server_notify_flags flags)
{
	g_assert(s);
	
	MUTEX_LOCK(s->mutex);
	
	if G_UNLIKELY(s->interrupted) { MUTEX_UNLOCK(s->mutex); return; }

	s->change_flags |= flags;
	
	MUTEX_UNLOCK(s->mutex);
}

gboolean
rem_server_check_compatibility(guint major, guint minor)
{
	LOG_INFO("PP     Version : %u.%u\n", major, minor);
	LOG_INFO("Server Version : %u.%u\n", REM_LIB_MAJOR, REM_LIB_MINOR);
	
	if (major != REM_LIB_MAJOR) {
		LOG_ERROR("incompatible versions\n");
		return FALSE;
	} else {
		return TRUE;
	}

}

