#ifndef REMNET_H_
#define REMNET_H_

/**
 * \defgroup rem-net Net Layer
 */
/*@{*/

/*@}*/
/**
 * Where does this get displayed?
 */

#include <remuco.h>

#define REM_PROTO_VERSION			0x06

typedef struct {
	guint		id;
	GByteArray	*ba;
} RemNetMsg;

typedef struct {
	GIOChannel	*chan;
	gchar		addr[18]; // 12 hex digits + 5 colons + term. null -> 18 chars
	gboolean	flush_on_close;
} RemNetClient;

typedef struct _RemNetServerPriv RemNetServerPriv;

typedef struct {
	GIOChannel			*chan;
	RemNetServerPriv	*priv;
} RemNetServer;


RemNetMsg*
rem_net_msg_new(void);

void
rem_net_msg_reset(RemNetMsg *msg);

void
rem_net_msg_destroy(RemNetMsg *msg);

void
rem_net_client_destroy(RemNetClient *client);

RemNetServer*
rem_net_server_new();

void
rem_net_server_destroy(RemNetServer* server);

RemNetClient*
rem_net_client_accept(RemNetServer *server);

void
rem_net_client_destroy(RemNetClient *client);

gint
rem_net_client_rxmsg(RemNetClient *client, RemNetMsg *msg);

gint
rem_net_client_txmsg(RemNetClient *client, const RemNetMsg *msg);

gint
rem_net_client_hello(RemNetClient* client);


#endif /*REMNET_H_*/
