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

#define REM_PROTO_VERSION			0x05

typedef struct {
	guint		id;
	GByteArray	*ba;
} RemNetMsg;

typedef struct {
	GIOChannel	*chan;
	gchar		addr[18]; // 12 hex digits + 5 colons + term. null -> 18 chars   
} rem_net_client_t;

typedef struct _rem_net_server_priv rem_net_server_priv_t;

typedef struct {
	GIOChannel				*chan;
	rem_net_server_priv_t	*priv;
} RemNetServer;


RemNetMsg*
rem_net_msg_new(void);

void
rem_net_msg_reset(RemNetMsg *msg);

void
rem_net_msg_destroy(RemNetMsg *msg);

RemNetServer*
rem_net_server_new();

void
rem_net_server_destroy(RemNetServer* server);

rem_net_client_t*
rem_net_client_accept(RemNetServer *server);

void
rem_net_client_destroy(rem_net_client_t *client);

gint
rem_net_client_rxmsg(rem_net_client_t *client, RemNetMsg *msg);

gint
rem_net_client_txmsg(rem_net_client_t *client, const RemNetMsg *msg);

#endif /*REMNET_H_*/
