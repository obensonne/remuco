#ifndef REMNET_H_
#define REMNET_H_

#include "../util/rem-common.h"

#define REM_NET_MAX_CLIENTS	50

enum rem_net_client_state_enum {
	REM_NET_CS_NOACTIVITY,
	REM_NET_CS_HASDATA,
	REM_NET_CS_CONNECTED
};

///////////////////////////////////////////////////////////////////////////////
//
// types and structs
//
///////////////////////////////////////////////////////////////////////////////

typedef struct {
	guint		id;
	GByteArray	*ba;
} rem_net_msg_t;

typedef struct _rem_net_server_priv rem_net_server_priv_t;

typedef struct {
	gint			sock;
	rem_net_server_priv_t	*priv;
} rem_net_server_t;

typedef struct {
	gint			sock;
	gchar			addr_str[48];
	gboolean		has_data;
} rem_net_client_t;

typedef struct {
	rem_net_server_t	server;
	rem_net_client_t	client[REM_NET_MAX_CLIENTS];
} rem_net_t;

///////////////////////////////////////////////////////////////////////////////
//
// public functions
//
///////////////////////////////////////////////////////////////////////////////

rem_net_t*
rem_net_up(void);

void
rem_net_down(rem_net_t *net);

gint
rem_net_client_accept(rem_net_t *net);

#define rem_net_client_is_connected(_net, _cn) ((_net)->client[_cn].sock > 0)

void
rem_net_client_disconnect(rem_net_t *net, guint cli_num);

gint
rem_net_recv(rem_net_t *net, guint cn, rem_net_msg_t *nmsg);

gint
rem_net_send(rem_net_t *net, guint cn, rem_net_msg_t *nmsg);

gint
rem_net_select(rem_net_t *net, guint select_timeout);

#endif /*REMNET_H_*/
