#ifndef REM_NET_H_
#define REM_NET_H_

///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include "common.h"

///////////////////////////////////////////////////////////////////////////////
//
// types and structs
//
///////////////////////////////////////////////////////////////////////////////

G_BEGIN_DECLS

typedef struct {
	guint		id;
	GByteArray	*ba;
} RemNetMsg;

typedef struct {
	gboolean	bt_on, ip_on;
	guint		ip_port;
} RemNetConfig;

typedef struct {
	GIOChannel	*chan;
	gchar		*addr;
	gboolean	flushing;
} RemNetClient;

typedef struct _RemNetServerPriv RemNetServerPriv;

typedef struct {
	
	GIOChannel				*chan;
	guint					watch_id;
	RemNetServerPriv		*priv;
	guint					priv_type;
	
} RemNetServer;

///////////////////////////////////////////////////////////////////////////////
//
// public functions
//
///////////////////////////////////////////////////////////////////////////////

GHashTable*
rem_net_up(RemNetConfig *config);

RemNetClient*
rem_net_accept(RemNetServer *Server);

gboolean
rem_net_rx(RemNetClient *client, RemNetMsg *msg);

gboolean
rem_net_tx(RemNetClient *client, const RemNetMsg *msg);

gboolean
rem_net_hello(RemNetClient* client);

void
rem_net_bye(RemNetClient *client);

#ifdef REM_NET_PRIV

#define REM_NET_SERVER_QL	25

GIOChannel*
rem_net_chan_from_sock(gint sock);

#endif

#endif /*REM_NET_H_*/
