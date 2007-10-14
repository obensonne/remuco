#include <bluetooth/sdp.h>
#include <bluetooth/sdp_lib.h>
#include <bluetooth/bluetooth.h>
#include <bluetooth/rfcomm.h>

#include <unistd.h>

#include "rem-net.h"
#include "../util/rem-util.h"

///////////////////////////////////////////////////////////////////////////////
//
// types and structs
//
///////////////////////////////////////////////////////////////////////////////

struct _rem_net_server_priv {
	
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

#define REM_NET_BT_SERVER_CONNECT_NPC	25

#define REM_NET_BT_SOCK_RC_SIZE	(sizeof(struct sockaddr_rc))

///////////////////////////////////////////////////////////////////////////////
//
// private function prototypes
//
///////////////////////////////////////////////////////////////////////////////

static void
priv_service_up(RemNetServer *srv, u_int8_t port);

static void
priv_service_down(RemNetServer *srv);

static gint
priv_server_socket_up(guint8 *port);

///////////////////////////////////////////////////////////////////////////////
//
// public functions
//
///////////////////////////////////////////////////////////////////////////////

RemNetServer*
rem_net_server_new(void)
{
	LOG_NOISE("called\n");

	gint				sock;
	RemNetServer	*server;
	guint8				port = 0;
	
	server = g_slice_new0(RemNetServer);
	
	////////// creatre rfcomm socket ////////// 
	
	sock = priv_server_socket_up(&port);
	
	if (!sock) {
		
		g_free(server);
		return NULL;
	}
	
	////////// create/register SDP service //////////
	
	server->priv = g_slice_new0(rem_net_server_priv_t);

	priv_service_up(server, port);
	
	if (!server->priv->sdp_session) {
		
		close(sock);
		g_free(server->priv);
		g_free(server);
		return NULL;
	}
	
	////////// create IO channel //////////
	
	server->chan = g_io_channel_unix_new(sock);
	
	g_io_channel_set_encoding(server->chan, NULL, NULL);
	
	g_io_channel_set_flags(server->chan, G_IO_FLAG_NONBLOCK, NULL);
	
	LOG_NOISE("done\n");

	return server;
}

/**
 * Destroy the server previously created with rem_bt_server_create().
 */
void
rem_net_server_destroy(RemNetServer *server)
{
	g_assert_debug(server);

	LOG_INFO("shutting down server channel\n");

	priv_service_down(server);
	
	g_io_channel_shutdown(server->chan, TRUE, NULL);
	g_io_channel_unref(server->chan);

	g_slice_free(rem_net_server_priv_t, server->priv);
	g_slice_free(RemNetServer, server);
	
}

rem_net_client_t*
rem_net_client_accept(RemNetServer *server)
{
	g_assert_debug(server);
	
	rem_net_client_t	*client;
	struct sockaddr_rc	addr_client;
	socklen_t			len;
	gint				sock, sock_server;
	
	len = REM_NET_BT_SOCK_RC_SIZE;

	////////// accept client //////////
	
	sock_server = g_io_channel_unix_get_fd(server->chan);
	sock = accept(sock_server, (struct sockaddr *) &addr_client, &len);
	if (sock < 0) {
		LOG_ERRNO("accepting connection failed");
		return NULL;
	}

	client = g_slice_new0(rem_net_client_t);
	
	ba2str(&addr_client.rc_bdaddr, client->addr);
	
	////////// create IO channel //////////
	
	client->chan = g_io_channel_unix_new(sock);
	
	g_io_channel_set_encoding(client->chan, NULL, NULL);
	
	g_io_channel_set_flags(client->chan, G_IO_FLAG_NONBLOCK, NULL);
	
	return client;
}

void
rem_net_client_destroy(rem_net_client_t *client)
{
	LOG_INFO("disconnect client %s\n", client->addr);
	g_io_channel_shutdown(client->chan, TRUE, NULL);
	g_io_channel_unref(client->chan);
	g_slice_free(rem_net_client_t, client);
}

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
priv_service_up(RemNetServer *server, guint8 port)
{
	g_assert_debug(server);
	
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
	
	//record = server->sdp_record;
	
	/* PART ONE */
	
	name = REM_NET_BT_SDP_SERVICE_NAME;
	dsc  = REM_NET_BT_SDP_SERVICE_DESC;
	prov = REM_NET_BT_SDP_SERVICE_PROV;
	uuid = REM_NET_BT_SDP_SERVICE_UUID;
	
	
	// set the general service ID
	sdp_uuid128_create( &svc_uuid, &uuid );
	record = g_malloc0(sizeof(sdp_record_t)); // no slice, since we don't free the record
	//memset(record, 0, sizeof(sdp_record_t));
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
	
	server->priv->sdp_record = record;
	server->priv->sdp_session = session;
	
}

/**
 * Deregister the Remuco servive from the SDP database.
 * 
 * @param bts
 *		the bluetooth server used when the service has been set up
 */
static void
priv_service_down(RemNetServer *server)
{
	g_assert_debug(server && server->priv);
	g_assert_debug(server->priv->sdp_record && server->priv->sdp_session);
	
	sdp_record_unregister(server->priv->sdp_session, server->priv->sdp_record);
	sdp_close(server->priv->sdp_session);
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
priv_server_socket_up(guint8 *port)
{
	gint sock, ret, npc;
	struct sockaddr_rc addr_server;
	
	////////// initialize //////////
	
	memset(&addr_server, 0, sizeof(struct sockaddr_rc));
	npc = REM_NET_BT_SERVER_CONNECT_NPC;
	addr_server.rc_family = AF_BLUETOOTH;
	addr_server.rc_bdaddr = *BDADDR_ANY; // first available bt-adapter
	
	////////// create socket //////////

	sock = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM);
	if (sock < 0) {
		LOG_ERROR("socket creation failed: %s\n", strerror(errno));
		return -1;
	}
	
	////////// bind socket (using first free port) //////////

	LOG_DEBUG("bind rfcomm socket\n");
	for (*port = 1; *port <= 30; (*port)++) {
		
		LOG_NOISE("try port %hhu\n", *port);
		addr_server.rc_channel = *port;
		ret = bind(sock, (struct sockaddr *) &addr_server, REM_NET_BT_SOCK_RC_SIZE);
		if (ret == 0) break;
		
	}
		
	if (*port > 30) {
		
		LOG_ERROR("no free port to bind to\n");
		close(sock);
		return -1;
		
	} else if (ret < 0) {
		
		LOG_ERRNO("bind failed");
		close(sock);
		return -1;
		
	}
	
	LOG_DEBUG("using port %hhu\n", *port);
	
	////////// set socket into listen mode //////////

	ret = listen(sock, npc);
	if (ret == -1) {
		
		LOG_ERRNO("set socket listen failed");
		close(sock);
		return -1;
		
	}

	return sock;
}

