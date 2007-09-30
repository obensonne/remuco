///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#define REM_NEED_SERIALIZATION_FUNCTIONS

#include "rem-comm.h"
#include "rem-net.h"

///////////////////////////////////////////////////////////////////////////////
//
// type definitions
//
///////////////////////////////////////////////////////////////////////////////

struct _rem_comm_priv_t {
	rem_net_t		*net;
	gchar			*srv_enc;	// encoding used internally by
						// the server
	GByteArray		*cfbd;	// cache for conversion free binary data
					// (to reduce serializing work)
};

///////////////////////////////////////////////////////////////////////////////
//
// privat functions
//
///////////////////////////////////////////////////////////////////////////////

static void
rem_comm_client_free(rem_comm_t *comm, guint cn)
{
	rem_comm_client_t *cc;
	
	cc = &(comm->cli[cn]);
	
	cc->state = REM_COMM_CS_DISCONNECTED;
	
	if (cc->info) {
		rem_cinfo_destroy(cc->info);
		cc->info = NULL;
	}
	
	if (cc->msg.data) {
		g_free(cc->msg.data);
		cc->msg.data = NULL;
	}

	cc->msg.id = REM_MSG_ID_IGNORE;
}

static inline GByteArray*
rem_comm_serialize(rem_comm_t *comm, rem_msg_t *msg, const rem_cinfo_t *ci)
{
	g_assert_debug(comm && msg && ci);
	
	GByteArray	*ba = NULL;
	rem_ploblist_t	*pl;
	rem_library_t	*lib;
	rem_plob_t	*plob;
	const gchar	*srv_enc = comm->priv->srv_enc;
	const rem_sv_t	*pte = ci->encodings;
	
	switch (msg->id) { // messages from server

	case REM_MSG_ID_IFS_CURPLOB:
	case REM_MSG_ID_REQ_PLOB:
	
		plob = (rem_plob_t*) msg->data;
		
		ba = plob ? rem_plob_serialize(plob, srv_enc, pte,
					ci->img_width, ci->img_height) : NULL;
		
		break;
		
	case REM_MSG_ID_IFS_PLAYLIST:
	case REM_MSG_ID_IFS_QUEUE:
	//case REM_MSG_ID_IFS_VOTELIST:
	case REM_MSG_ID_REQ_PLOBLIST:
	case REM_MSG_ID_REQ_SEARCH:
	
		pl = (rem_ploblist_t*) msg->data;
		
		ba = rem_ploblist_serialize(pl, srv_enc, pte);
		
		break;
		
	case REM_MSG_ID_IFS_PINFO:
	
		if (!comm->priv->cfbd)
			comm->priv->cfbd = rem_pinfo_serialize(
				(rem_pinfo_t*) msg->data, srv_enc, pte);
		
		ba = comm->priv->cfbd;
				
		break;
		
	case REM_MSG_ID_IFS_STATE:
				
		if (!comm->priv->cfbd)
			comm->priv->cfbd = rem_ps_serialize(
				(rem_ps_t*) msg->data, srv_enc, pte);
		
		ba = comm->priv->cfbd;
				
		break;
		
	case REM_MSG_ID_IFS_SRVDOWN:
	
		break;
		
	case REM_MSG_ID_REQ_LIBRARY:
	
		lib = (rem_library_t*) msg->data;
		
		ba = rem_library_serialize(lib, srv_enc, pte);
		
		break;
	
	default:
	
		g_assert_not_reached();
		
		break;
	}
	
	return ba;
}

static gpointer
rem_comm_unserialize(rem_comm_t *comm, rem_net_msg_t *nmsg)
{
	g_assert_debug(comm && nmsg);
	
	gpointer	data = NULL;
	const gchar	*srv_enc = comm->priv->srv_enc;
	
	data = NULL;
	
	switch (nmsg->id) {
		
	case REM_MSG_ID_IGNORE:	
			break;
			
	case REM_MSG_ID_IFC_CINFO:
	
			data = rem_cinfo_unserialize(nmsg->ba, srv_enc);	
			break;
			
	case REM_MSG_ID_REQ_PLOB:
	case REM_MSG_ID_REQ_PLOBLIST:
	case REM_MSG_ID_CTL_PLAY_PLOBLIST:
			
			data = rem_strpar_unserialize(nmsg->ba, srv_enc);
			break;
			
	case REM_MSG_ID_CTL_SCTRL:
			
			data = rem_sctrl_unserialize(nmsg->ba, srv_enc);
			
			break;
			
	case REM_MSG_ID_REQ_SEARCH:
	case REM_MSG_ID_CTL_UPD_PLOB:
			
			data = rem_plob_unserialize(nmsg->ba, srv_enc);			
			
			break;
			
	case REM_MSG_ID_CTL_UPD_PLOBLIST:

			data = rem_ploblist_unserialize(nmsg->ba, srv_enc);
			
			break;
			
			
	default:
		
			LOG_WARN("unsupported message id\n");
			break;
	}
	
	return data;
}

static void
rem_comm_rx(rem_comm_t *comm, guint cn)
{	
	
	g_assert_debug(comm);
	g_assert_debug(comm->cli[cn].state != REM_COMM_CS_DISCONNECTED);
	g_assert_debug(comm->cli[cn].msg.data == NULL);
	g_assert_debug(comm->cli[cn].msg.id == REM_MSG_ID_IGNORE);
	
	gint			ret;
	rem_net_msg_t		nmsg;
	rem_comm_client_t	*cc;
	gpointer		data;
	
	data = NULL;
	
	nmsg.id = REM_MSG_ID_IGNORE;
	nmsg.ba = NULL;
	
	cc = &(comm->cli[cn]);
	
	g_assert_debug(cc->msg.data == NULL);
	
	ret = rem_net_recv(comm->priv->net, cn, &nmsg);
	
	if (ret < 0) {
		
		LOG_WARN("receiving data failed (probably client disconnected)\n");
		
		g_assert_debug(!nmsg.ba);
		rem_comm_client_free(comm, cn);
		return;
		
	}
	
	// check if client state and msg type are compatible

	if (!concl(nmsg.id == REM_MSG_ID_IFC_CINFO,
			cc->state == REM_COMM_CS_HANDSHAKE_SRV_WAITS_FOR_CINFO)) {
	    	
		LOG_WARN("received invalid msg\n");

		rem_net_client_disconnect(comm->priv->net, cn);		 
		rem_comm_client_free(comm, cn);
		if (nmsg.ba) g_byte_array_free(nmsg.ba, TRUE);
		
		return;
	}

	// unserialize the message
	
	if (nmsg.ba) {
		data = rem_comm_unserialize(comm, &nmsg);
		if (!data) nmsg.id = REM_MSG_ID_IGNORE;
		g_byte_array_free(nmsg.ba, TRUE);
	}
	
	if (nmsg.id == REM_MSG_ID_IFC_CINFO) {
		
		// handle client info

		cc->state = REM_COMM_CS_HANDSHAKE_CLI_WAITS_FOR_PINFO;
	    	
	    	comm->cli[cn].info = (rem_cinfo_t*) data;
	    	
	    	LOG_DEBUG("received cinfo from client %u:\n", cn);
	    	rem_cinfo_dump(comm->cli[cn].info);
	    	
	} else {
		
		cc->msg.id = nmsg.id;
		cc->msg.data = data;
		
	}
	
	g_assert_debug(cc->msg.id != REM_MSG_ID_IGNORE || cc->msg.data == NULL);
	
	return;
}

///////////////////////////////////////////////////////////////////////////////
//
// public functions
//
///////////////////////////////////////////////////////////////////////////////

rem_comm_t*
rem_comm_up(const gchar *srv_enc, guint listen_timeout)
{
	g_assert_debug(srv_enc);
	
	LOG_NOISE("called\n");

	rem_comm_t *comm;
	
	comm = g_malloc0(sizeof(rem_comm_t));

	comm->listen_timeout = listen_timeout;
	
	comm->priv = g_malloc0(sizeof(rem_comm_priv_t));

	comm->priv->net = rem_net_up();
	
	if (!comm->priv->net) {
		g_free(comm->priv);
		g_free(comm);
		return NULL;
	}
	
	comm->priv->srv_enc = g_strdup(srv_enc);

	LOG_NOISE("done\n");

	return comm;
}

void
rem_comm_down(rem_comm_t *comm)
{
	g_assert_debug(comm);
	
	rem_msg_t	msg;
	guint		u;
	
	LOG_INFO("shutting down comm\n");

	// say good bye to clients
	
	msg.id = REM_MSG_ID_IFS_SRVDOWN;
	msg.data = NULL;
	rem_comm_tx_bc(comm, &msg);
	
	// give the clients some time to recognize this
	
	g_msleep(1000);
	
	// shut down the net and clean up
	
	rem_net_down(comm->priv->net);

	for (u = 0; u < REM_MAX_CLIENTS; ++u) {
		
		if (comm->cli[u].state != REM_COMM_CS_DISCONNECTED)
		
			rem_comm_client_free(comm, u);
		
		g_assert_debug(comm->cli[u].info == NULL);
		g_assert_debug(comm->cli[u].msg.data == NULL);
	}

	if (comm->priv->cfbd) g_byte_array_free(comm->priv->cfbd, TRUE);
	
	g_free(comm->priv->srv_enc);

	g_free(comm->priv);
	
	g_free(comm);
}


// do select on all the sockets
// returns flags if data is available or a client has connected
gint
rem_comm_listen(rem_comm_t *comm)
{
	g_assert_debug(comm);
	
	gint ret;
	guint u;
	GByteArray *ba;
		
	ba = NULL;
	
	ret = rem_net_select(comm->priv->net, comm->listen_timeout);
	if (ret < 0) {
		return -1;
	}
	
	for (u = 0; u < REM_MAX_CLIENTS; u++) {
		
		if (!rem_net_client_is_connected(comm->priv->net, u)) {
			
			// check if client recently disconnected
			if (comm->cli[u].state != REM_COMM_CS_DISCONNECTED) {
				rem_comm_client_free(comm, u);
			}
			
			g_assert_debug(
				comm->cli[u].state == REM_COMM_CS_DISCONNECTED
				&& comm->cli[u].msg.id == REM_MSG_ID_IGNORE);
			
			continue;
		}
		
		// check if client recently connected
		if (comm->cli[u].state == REM_COMM_CS_DISCONNECTED) {
			
			comm->cli[u].state = REM_COMM_CS_HANDSHAKE_SRV_WAITS_FOR_CINFO;
			
			g_assert_debug(!comm->priv->net->client[u].has_data);
			g_assert_debug(!comm->cli[u].info);
		}

		if (comm->priv->net->client[u].has_data) {
			
			
			LOG_DEBUG("client %s has data\n",
					comm->priv->net->client[u].addr_str);
			rem_comm_rx(comm, u);

			comm->priv->net->client[u].has_data = FALSE;
		}
	}

	return 0;
}

void
rem_comm_tx(rem_comm_t *comm, guint cn, rem_msg_t *msg)
{
	g_assert_debug(comm && msg && msg->id < REM_MSG_ID_COUNT);
	
	LOG_NOISE("called\n");
	
	rem_net_msg_t	nmsg;
	guint		u, from, to;
	gint		ret;

	if (cn == REM_MAX_CLIENTS) {	// broadcast the message
		
		from = 0;
		to = REM_MAX_CLIENTS;
		
	} else {
		
		from = cn;
		to = cn + 1;

	}

	nmsg.id = msg->id;

	for (u = from; u < to; u++) {

		// get binary data (handle each client seperatly: because for
		// character encoding reasons, binary data may be different
		// for clients)
		
		// until we did not get a client info, we do not send anything
		if (comm->cli[u].state < REM_COMM_CS_HANDSHAKE_CLI_WAITS_FOR_PINFO)
			continue;
			
		// until the client is not fully connected, only send player info
		if (comm->cli[u].state == REM_COMM_CS_HANDSHAKE_CLI_WAITS_FOR_PINFO &&
					msg->id != REM_MSG_ID_IFS_PINFO)
			continue;
		
		nmsg.ba = rem_comm_serialize(comm, msg, comm->cli[u].info);
		
		ret = rem_net_send(comm->priv->net, u, &nmsg);
		
		if (ret < 0) {
			rem_comm_client_free(comm, u);
		}
		
		// Encoding independent data gets cached within
		// rem_comm_serialize() and will be free'd after the loop.
		// Encoding dependent data must be free'd here:
		if (!comm->priv->cfbd && nmsg.ba)
			g_byte_array_free(nmsg.ba, TRUE);		
		
	}
		
	// free the binary data cache (used for encoding independent data within
	// rem_comm_serialize())
	if (comm->priv->cfbd) {
		g_byte_array_free(comm->priv->cfbd, TRUE);
		comm->priv->cfbd = NULL;
	}
	
	
}

