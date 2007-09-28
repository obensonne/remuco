#ifndef REMCOMM_H_
#define REMCOMM_H_

///////////////////////////////////////////////////////////////////////////////
//
// Includes
//
///////////////////////////////////////////////////////////////////////////////

#include "../util/rem-common.h"
#include "rem-msg.h"
#include "../data/rem-data.h"

///////////////////////////////////////////////////////////////////////////////
//
// constants
//
///////////////////////////////////////////////////////////////////////////////

#define REM_COMM_PIVAL_DEFAULT	2

///////////////////////////////////////////////////////////////////////////////
//
// type definitions
//
///////////////////////////////////////////////////////////////////////////////

enum rem_comm_cli_state_enum {
	REM_COMM_CS_DISCONNECTED,
	REM_COMM_CS_HANDSHAKE_SRV_WAITS_FOR_CINFO,
	REM_COMM_CS_HANDSHAKE_CLI_WAITS_FOR_PINFO,
	REM_COMM_CS_CONNECTED
};

typedef struct {
	rem_cinfo_t	*info;		// meta infos about the client
	guint		state;		// if the handshake (exchange of 'mai'
					// and 'mas') is already done
	rem_msg_t	msg;		// holds the message, just rx'ed from
					// that client (for use by the upper
					// layer)
} rem_comm_client_t;

typedef struct _rem_comm_priv_t rem_comm_priv_t; 

typedef struct {
	rem_comm_client_t	cli[REM_MAX_CLIENTS];
	guint			listen_timeout;
	rem_comm_priv_t		*priv;
} rem_comm_t;


///////////////////////////////////////////////////////////////////////////////
//
// using the comm layer
//
///////////////////////////////////////////////////////////////////////////////

rem_comm_t*
rem_comm_up(const gchar *srv_enc, guint listen_timeout);

void
rem_comm_down(rem_comm_t *comm);

// do select on all the sockets
// returns flags if data is available or a client has connected
gint
rem_comm_listen(rem_comm_t *comm);

//gint
//rem_comm_rx(rem_comm_t *comm, guint cli_num, guint *msg_code, gpointer *msg_data);

#define rem_comm_tx_bc(_comm, _msg) rem_comm_tx(_comm, REM_MAX_CLIENTS, _msg)

void
rem_comm_tx(rem_comm_t *comm, guint cli_num, rem_msg_t *msg);

void
rem_comm_disconnect_client(rem_comm_t *comm, guint cli_num);

#endif /*REMCOMM_H_*/
