///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>

#include <unistd.h> // close()

#define REM_NET_PRIV

#include "ip.h"

///////////////////////////////////////////////////////////////////////////////
//
// types and structs
//
///////////////////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////////////////////////
//
// private functions
//
///////////////////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////////////////////////
//
// public functions
//
///////////////////////////////////////////////////////////////////////////////

RemNetServer*
rem_net_ip_up(guint port)
{
	struct sockaddr_in	addr;
	gint				sock, ret;
	RemNetServer		*server;
	
	////////// initialize //////////
	
	memset(&addr, 0, sizeof(addr));
	addr.sin_family = AF_INET;
	addr.sin_addr.s_addr = g_htonl(INADDR_ANY);
	addr.sin_port = g_htons(port);

	////////// create socket //////////
	
	sock = socket(PF_INET, SOCK_STREAM, 0);
	if (sock < 0) {
		LOG_ERRNO("failed to create socket");
		return NULL;
	}
	
	////////// bind socket //////////
	
	ret = bind(sock, (struct sockaddr *) &addr, sizeof(addr));
	if (ret < 0) {
		LOG_ERRNO("failed to bind socket");
		close(sock);
		return NULL;
	}

	////////// set socket into listen mode //////////
	
	ret = listen(sock, REM_NET_SERVER_QL);
	if (ret < 0) {
		LOG_ERRNO("failed to set socket into listen mode");
		close(sock);
		return NULL;
	}
	
	////////// create an io channel //////////
	
	server = g_slice_new0(RemNetServer);
	
	server->chan = rem_net_chan_from_sock(sock);
	
	if (!server->chan) {
		close(sock);
		g_slice_free(RemNetServer, server);
		return NULL;
	}

	LOG_DEBUG("server channel is up");
	
	return server;
}

/** Shut down the ip private part of 'server'. */ 
void
rem_net_ip_down(RemNetServer *server)
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

	g_slice_free(RemNetServer, server);
}


RemNetClient*
rem_net_ip_accept(RemNetServer *server)
{
	RemNetClient		*client;
	struct sockaddr_in	addr_client;
	socklen_t			len;
	gint				sock, sock_server;

	len = sizeof(addr_client);

	////////// accept client //////////
	
	sock_server = g_io_channel_unix_get_fd(server->chan);
	sock = accept(sock_server, (struct sockaddr *) &addr_client, &len);
	if (sock < 0) {
		LOG_ERRNO("failed to accept client");
		return NULL;
	}

	client = g_slice_new0(RemNetClient);

	client->addr = g_strdup(inet_ntoa(addr_client.sin_addr));
	
	////////// create IO channel //////////
	
	client->chan = rem_net_chan_from_sock(sock);
	if (!client->chan) {
		rem_net_bye(client);
		return NULL;
	}
	
	return client;
}


