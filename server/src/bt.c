///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include <bluetooth/sdp.h>
#include <bluetooth/sdp_lib.h>
#include <bluetooth/bluetooth.h>
#include <bluetooth/rfcomm.h>

#include <unistd.h> // close()

#define REM_NET_PRIV

#include "bt.h"

///////////////////////////////////////////////////////////////////////////////
//
// types and structs
//
///////////////////////////////////////////////////////////////////////////////

struct _RemNetServerPriv {
	
	sdp_session_t	*sdp_session;
	sdp_record_t	*sdp_record;
	
};

///////////////////////////////////////////////////////////////////////////////
//
// constants
//
///////////////////////////////////////////////////////////////////////////////

#define	REM_NET_BT_SDP_SERVICE_NAME	"Remuco"
#define REM_NET_BT_SDP_SERVICE_DESC	"Linux Media Player Remote Control"
#define REM_NET_BT_SDP_SERVICE_PROV	"remuco.sf.net"

static const guint32
	REM_NET_BT_SDP_SERVICE_UUID[] = { 0x95C4, 0x5DF4, 0x5E73, 0x03C7 };

#define REM_NET_BT_SOCK_RC_SIZE	(sizeof(struct sockaddr_rc))

#define REM_NET_BT_BTADDR_STR_LEN	18 // 12 digits + 5 colons + term. 0

///////////////////////////////////////////////////////////////////////////////
//
// private functions
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Register the Remuco service (serial port profile (SPP)) in the service
 * discovery protocol (SDP) database.
 * 
 * @param port
 * 	The port to use for serial port (rfcomm)
 * 	Range: 1 <= port <= 30
 * 
 * @return
 * 	The session used to register the service and which may be used to
 * 	deregister the service
 */
static void
sdp_up(RemNetServerPriv *server_priv, guint8 port)
{
	gchar				*name, *dsc,*prov;
	const guint32		*uuid;
	uuid_t				root_uuid,
						l2cap_uuid,
						rfcomm_uuid,
						svc_uuid,
						svc_class_uuid;
				
	sdp_list_t			*l2cap_list = 0,
						*rfcomm_list = 0,
						*root_list = 0,
						*proto_list = 0,
						*access_proto_list = 0,
						*svc_class_list = 0,
						*profile_list = 0;
				
	sdp_data_t			*channel = 0;
	sdp_profile_desc_t	profile;
	sdp_record_t		*record;
	sdp_session_t		*session = 0;
	
	/* PART ONE */
	
	name = REM_NET_BT_SDP_SERVICE_NAME;
	dsc  = REM_NET_BT_SDP_SERVICE_DESC;
	prov = REM_NET_BT_SDP_SERVICE_PROV;
	uuid = REM_NET_BT_SDP_SERVICE_UUID;
	
	// set the general service ID
	sdp_uuid128_create( &svc_uuid, &uuid );
	record = g_new0(sdp_record_t, 1); // no slice, since we don't free the record
	sdp_set_service_id( record, svc_uuid );
	
	// set the service class
	sdp_uuid16_create(&svc_class_uuid, SERIAL_PORT_SVCLASS_ID);
	svc_class_list = sdp_list_append(0, &svc_class_uuid);
	sdp_set_service_classes(record, svc_class_list);
	
	// set the Bluetooth profile information
	memset(&profile, 0, sizeof(sdp_profile_desc_t));
	sdp_uuid16_create(&profile.uuid, SERIAL_PORT_PROFILE_ID);
	profile.version = 0x0100;
	profile_list = sdp_list_append(0, &profile);
	sdp_set_profile_descs(record, profile_list);
	
	// make the service record publicly browsable
	sdp_uuid16_create(&root_uuid, PUBLIC_BROWSE_GROUP);
	root_list = sdp_list_append(0, &root_uuid);
	sdp_set_browse_groups(record, root_list );
	
	// set l2cap information
	sdp_uuid16_create(&l2cap_uuid, L2CAP_UUID);
	l2cap_list = sdp_list_append( 0, &l2cap_uuid );
	proto_list = sdp_list_append( 0, l2cap_list );
	
	// register the RFCOMM channel for RFCOMM sockets
	sdp_uuid16_create(&rfcomm_uuid, RFCOMM_UUID);
	channel = sdp_data_alloc(SDP_UINT8, &port);
	rfcomm_list = sdp_list_append( 0, &rfcomm_uuid );
	sdp_list_append( rfcomm_list, channel );
	sdp_list_append( proto_list, rfcomm_list );
	access_proto_list = sdp_list_append( 0, proto_list );
	sdp_set_access_protos( record, access_proto_list );
	
	// set the name, provider, and description
	sdp_set_info_attr(record, name, prov, dsc);

	/* PART TWO */
	
	// connect to the local SDP server, register the service record
	session = sdp_connect( BDADDR_ANY, BDADDR_LOCAL, 0 );
	sdp_record_register(session, record, 0);

	// cleanup
	sdp_data_free( channel );
	sdp_list_free( l2cap_list, 0 );
	sdp_list_free( rfcomm_list, 0 );
	sdp_list_free( root_list, 0 );
	sdp_list_free( profile_list, 0 );
	sdp_list_free( proto_list, 0 );
	sdp_list_free( access_proto_list, 0 );
	sdp_list_free( svc_class_list, 0 );
	
	server_priv->sdp_record = record;
	server_priv->sdp_session = session;
	
}

/**
 * Deregister the Remuco servive from the SDP database.
 * 
 * @param bts
 *		the bluetooth server used when the service has been set up
 */
static void
sdp_down(RemNetServerPriv *server_priv)
{
	if (!server_priv->sdp_session)
		return;
	
	g_assert(server_priv->sdp_record);
	
	sdp_record_unregister(server_priv->sdp_session, server_priv->sdp_record);
	sdp_close(server_priv->sdp_session);
}

/**
 * Set up an rfcomm server socket which is ready for accepting client
 * connections.
 * 
 * @param port (in/out parameter)
 * 	- used port will be written into that param
 * 
 * @return
 * 	the server socket descriptor or -1 if something failed
 */
static gint
socket_up(guint8 *port)
{
	gint sock, ret;
	struct sockaddr_rc addr_server;
	
	////////// initialize //////////
	
	memset(&addr_server, 0, sizeof(struct sockaddr_rc));
	addr_server.rc_family = AF_BLUETOOTH;
	addr_server.rc_bdaddr = *BDADDR_ANY; // first available bt-adapter
	
	////////// create socket //////////

	sock = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM);
	if (sock < 0) {
		LOG_ERRNO("socket creation failed");
		return -1;
	}
	
	////////// bind socket (using first free port) //////////

	LOG_DEBUG("bind rfcomm socket");
	for (*port = 1; *port <= 30; (*port)++) {
		
		//LOG_DEBUG("try port %hhu", *port);
		addr_server.rc_channel = *port;
		ret = bind(sock, (struct sockaddr *) &addr_server, REM_NET_BT_SOCK_RC_SIZE);
		if (ret == 0) break;
		
	}
		
	if (*port > 30) {
		
		LOG_ERROR("no free port to bind to");
		close(sock);
		return -1;
		
	} else if (ret < 0) {
		
		LOG_ERRNO("bind failed");
		close(sock);
		return -1;
		
	}
	
	LOG_DEBUG("using port %hhu", *port);
	
	////////// set socket into listen mode //////////

	ret = listen(sock, REM_NET_SERVER_QL);
	if (ret == -1) {
		
		LOG_ERRNO("set socket listen failed");
		close(sock);
		return -1;
		
	}

	return sock;
}

///////////////////////////////////////////////////////////////////////////////
//
// public functions
//
///////////////////////////////////////////////////////////////////////////////

RemNetServer*
rem_net_bt_up(void)
{
	gint			sock;
	RemNetServer	*server;
	guint8			port = 0;
	
	// TODO: detect if there already is a service
	// http://people.csail.mit.edu/albert/bluez-intro/x604.html
	
	server = g_slice_new0(RemNetServer);
	
	////////// creatre rfcomm socket ////////// 
	
	sock = socket_up(&port);
	
	if (!sock) {
		
		rem_net_bt_down(server);
		return NULL;
	}
	
	////////// create/register SDP service //////////
	
	server->priv = g_slice_new0(RemNetServerPriv);

	sdp_up(server->priv, port);
	
	if (!server->priv->sdp_session) {
		
		close(sock);
		rem_net_bt_down(server);
		return NULL;
	}
	
	////////// create IO channel //////////
	
	server->chan = rem_net_chan_from_sock(sock);

	if (!server->chan) {
		rem_net_bt_down(server);
		return NULL;		
	}
	
	LOG_DEBUG("server channel is up");
	
	return server;
}

/** Shut down the bt private part of 'server'. */ 
void
rem_net_bt_down(RemNetServer *server)
{
	if (!server)
		return;
	
	if (server->watch_id)
		g_source_remove(server->watch_id);
	
	if (server->chan) {
		// the following shuts down the channel if its ref-count is zero
		// shuts down with flush = TRUE, but this should not matter as the
		// server channels don't send any data
		g_io_channel_unref(server->chan);
	}

	if (!server->priv)
		return;
	
	sdp_down(server->priv);
	g_slice_free(RemNetServerPriv, server->priv);		
	g_slice_free(RemNetServer, server);
}

/**
 * Accepts a client connection request on the socket of a RemNetServer.
 * On success returns a RemNetClient with a new socket and new IO channel.
 * No data is yet communicated at this point!
 */ 
RemNetClient*
rem_net_bt_accept(RemNetServer *server)
{
	RemNetClient		*client;
	struct sockaddr_rc	addr_client;
	socklen_t			len;
	gint				sock, sock_server;

	len = REM_NET_BT_SOCK_RC_SIZE;

	////////// accept client //////////
	
	sock_server = g_io_channel_unix_get_fd(server->chan);
	sock = accept(sock_server, (struct sockaddr *) &addr_client, &len);
	if (sock < 0) {
		LOG_ERRNO("failed to accept client");
		return NULL;
	}

	client = g_slice_new0(RemNetClient);
	
	client->addr = g_new0(gchar, REM_NET_BT_BTADDR_STR_LEN);
	
	ba2str(&addr_client.rc_bdaddr, client->addr);
	
	////////// create IO channel //////////
	
	client->chan = rem_net_chan_from_sock(sock);
	if (!client->chan) {
		rem_net_bye(client);
		return NULL;
	}
	
	return client;
}

