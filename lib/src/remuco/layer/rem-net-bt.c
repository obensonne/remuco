#include <bluetooth/sdp.h>
#include <bluetooth/sdp_lib.h>
#include <bluetooth/bluetooth.h>
#include <bluetooth/rfcomm.h>

#include <unistd.h>

#include <remuco/layer/rem-net.h>

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
#define REM_NET_BT_SDP_SERVICE_DESC	"REmote MUsic and media COntrol"
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
rem_net_bt_server_service_up(rem_net_server_t *srv, u_int8_t port);

static void
rem_net_bt_server_service_down(rem_net_server_t *srv);

static gint
rem_net_bt_server_socket_up(guint8 *port);

static void
rem_net_bt_server_socket_down(gint sock);

///////////////////////////////////////////////////////////////////////////////
//
// public functions
//
///////////////////////////////////////////////////////////////////////////////

rem_net_t*
rem_net_up(void)
{
	LOG_NOISE("called\n");

	rem_net_t *net;
	
	net = g_malloc0(sizeof(rem_net_t));
	
	guint8 port;
	
	// socket (RFCOMM)
	
	port = 100; // choose the next free port
	net->server.sock = rem_net_bt_server_socket_up(&port);
	
	if (!net->server.sock) {
		
		g_free(net);
		return NULL;
	}
	
	// service (SPP)
	
	net->server.priv = g_malloc0(sizeof(rem_net_server_priv_t));

	rem_net_bt_server_service_up(&net->server, port);
	
	if (!net->server.priv->sdp_session) {
		
		rem_net_bt_server_socket_down(net->server.sock);
		g_free(net->server.priv);
		g_free(net);
		return NULL;
	}
	
	LOG_NOISE("done\n");

	return net;
}

void
rem_net_down(rem_net_t *net)
{
	g_assert_debug(net && net->server.priv->sdp_session);
	
	guint u;
	
	LOG_INFO("shutting down net\n");

	rem_net_bt_server_service_down(&net->server);
	
	rem_net_bt_server_socket_down(net->server.sock);

	for (u = 0; u < REM_NET_MAX_CLIENTS; u++) {
		
		if (rem_net_client_is_connected(net, u))
		
			rem_net_client_disconnect(net, u);
		
	}
	
	g_free(net->server.priv);
	
	g_free(net);
	
}

gint
rem_net_client_accept(rem_net_t *net)
{
	g_assert_debug(net && net->server.sock > 0);
	
	struct sockaddr_rc addr_client;
	rem_net_client_t *cli;
	socklen_t len;
	guint u;
	gint sock_tmp;
	
	len = REM_NET_BT_SOCK_RC_SIZE;

	sock_tmp = accept(net->server.sock, (struct sockaddr *) &addr_client, &len);
	if (sock_tmp < 0) {
		LOG_ERRNO("accepting connection failed");
		return -1;
	}

	for (u = 0; u < REM_NET_MAX_CLIENTS; u++) {
		
		if (!net->client[u].sock) break;
		
	}
	
	if (u == REM_NET_MAX_CLIENTS) {
		
		LOG_WARN("too much clients -> discard connection request\n");
		
		close(sock_tmp);
		
		return -1;
	}

	
	cli = &net->client[u];
	
	cli->sock = sock_tmp;
	cli->has_data = FALSE;
	
	//memcpy(&cli->addr_hex, &addr_client.rc_bdaddr, sizeof(bdaddr_t));
	ba2str(&addr_client.rc_bdaddr, cli->addr_str);
	
	LOG_INFO("client %s has connected\n", cli->addr_str);
	
	return u;
}

void
rem_net_client_disconnect(rem_net_t *net, guint cn)
{
	g_assert_debug(net && net->client[cn].sock > 0);
	
	LOG_INFO("disconnect client %s\n", net->client[cn].addr_str);
	
	close(net->client[cn].sock);
	
	memset(&net->client[cn], 0, sizeof(rem_net_client_t));
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
rem_net_bt_server_service_up(rem_net_server_t *srv, u_int8_t port)
{
	g_assert_debug(srv);
	
	gchar			*name, *dsc,*prov;
	const guint32		*uuid;
	uuid_t			root_uuid,
				l2cap_uuid,
				rfcomm_uuid,
				svc_uuid,
				svc_class_uuid;
				
	sdp_list_t		*l2cap_list = 0,
				*rfcomm_list = 0,
				*root_list = 0,
				*proto_list = 0,
				*access_proto_list = 0,
				*svc_class_list = 0,
				*profile_list = 0;
				
	sdp_data_t		*channel = 0;
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
	record = g_malloc0(sizeof(sdp_record_t));
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
	
	srv->priv->sdp_record = record;
	srv->priv->sdp_session = session;
	
}

/**
 * Deregister the Remuco servive from the SDP database.
 * 
 * @param srv
 * 	the bluetooth server used when the service has been set up
 */
static void
rem_net_bt_server_service_down(rem_net_server_t *srv)
{
	g_assert_debug(srv);
	
	sdp_record_unregister(srv->priv->sdp_session, srv->priv->sdp_record);
	sdp_close(srv->priv->sdp_session);
}

/**
 * Set up an rfcomm server socket which is ready for accepting client
 * connections.
 * 
 * @param port (in/out parameter)
 * 	- if 1 <= port <= 30 then this function trys to bind to that port
 * 	- if port is out of that range, the first free port is used and port
 * 	  will be set to that number
 * 
 * @return
 * 	the server socket descriptor or -1 if something failed
 */
static gint
rem_net_bt_server_socket_up(guint8 *port)
{
	struct sockaddr_rc addr_server;
	memset(&addr_server, 0, sizeof(struct sockaddr_rc));
	gint s, ret, npc;
	
	npc = REM_NET_BT_SERVER_CONNECT_NPC;
	
	addr_server.rc_family = AF_BLUETOOTH;
	addr_server.rc_bdaddr = *BDADDR_ANY; // first available bt-adapter
	
	s = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM);
	if (s < 0) {
		LOG_ERROR("socket creation failed: %s\n", strerror(errno));
		return -1;
	}
	
	LOG_DEBUG("bind rfcomm socket\n");
	if (*port >= 1 && *port <= 30) {
		
		LOG_NOISE("try port %hhu\n", *port);
		addr_server.rc_channel = *port;
		ret = bind(s, (struct sockaddr *)&addr_server, REM_NET_BT_SOCK_RC_SIZE);
		
	} else {
		
		for (*port = 1; *port <= 30; (*port)++) {
			
			LOG_NOISE("try port %hhu\n", *port);
			addr_server.rc_channel = *port;
			ret = bind(s, (struct sockaddr *) &addr_server,
								REM_NET_BT_SOCK_RC_SIZE);
			if (ret == 0) break;
			
		}
		
	}
	
	if (ret < 0) {
		
		LOG_ERRNO("bind failed");
		close(s);
		return -1;
		
	}
	
	LOG_DEBUG("using port %hhu\n", *port);
	
	ret = listen(s, npc);
	if (ret == -1) {
		
		LOG_ERRNO("set socket listen failed");
		close(s);
		return -1;
		
	}

	return s;
}

static void
rem_net_bt_server_socket_down(gint sock)
{
	close(sock);
}

