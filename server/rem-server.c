/*
 * Copyright (C) 2006 Christian Buennig - See COPYING
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include <dirent.h>
#include <signal.h>
#include <sys/select.h>
#include <stdlib.h>
#include <unistd.h>

#include "rem-pp.h"
#include "rem-io.h"
#include "rem-bt.h"
#include "rem-log.h"
#include "rem-ps-handler.h"
#include "rem-util.h"
#include "rem.h"

///////////////////////////////////////////////////////////////////////////////
//
// macros general
//
///////////////////////////////////////////////////////////////////////////////

#define EXIT_OK			0
#define EXIT_ERROR		1


///////////////////////////////////////////////////////////////////////////////
//
// macros remuco
//
///////////////////////////////////////////////////////////////////////////////

#ifndef REM_RELEASE_MAIN
#error "Define REM_RELEASE !"
#endif

#ifndef REM_RELEASE_SERVER
#error "Define REM_RELEASE !"
#endif

#ifndef REM_RELEASE_PP
#error "Define REM_RELEASE !"
#endif

#ifndef REM_PP_NAME
#error "Define REM_RELEASE !"
#endif

#define REM_SVC_NAME	"Remuco Server"
#define REM_SVC_DESC	"Bluetooth server for Remuco clients (v " REM_RELEASE_MAIN ")"
#define REM_SVC_PROV	"remuco.sf.net"

#define REM_MAX_CLIENTS	10

///////////////////////////////////////////////////////////////////////////////
//
// structs
//
///////////////////////////////////////////////////////////////////////////////

struct rem_server_conf {
	char		pp_enc[REM_MAX_STR_LEN];
	int		ps_poll_ival;
};

struct rem_client {
	struct rfcomm_srv_client	rfcc;
	struct rem_ci			ci;
};

static int	rem_server_sockets_check(fd_set *sds);
static int	rem_server_sockets_process(fd_set *sds);
static int	rem_server_init(void);
static void	rem_server_usage(void);
static void	rem_server_shutdown(void);
static void	rem_server_sigint(int num);
static void	rem_server_tx_ps_broadcast(void);

///////////////////////////////////////////////////////////////////////////////
//
// global vars
//
///////////////////////////////////////////////////////////////////////////////

// io and bluetooth
static const u_int32_t		REM_SVC_UUID[] = 
					{ 0x95C4, 0x5DF4, 0x5E73, 0x03C7 };
static sdp_session_t		*sdp_session;
static int			ss; // server socket bluetooth (rfcomm)

// data
static struct rem_ps_bin	ps_bin;
static struct rem_server_conf	rsc;
static struct rem_client	*client_list[REM_MAX_CLIENTS];

// application state flags
static int			interrupted;

///////////////////////////////////////////////////////////////////////////////
//
// functions - misc
//
///////////////////////////////////////////////////////////////////////////////

static void
rem_server_usage()
{
	printf("Remuco Server " REM_RELEASE_MAIN " for " REM_PP_NAME "\n");
	printf("Usage: remuco-" REM_PP_NAME " [-p POLL-IVAL] [-e PP-ENC]\n");
	printf("   or: remuco-" REM_PP_NAME " {-h|--help}\n");
	printf("Options:\n");
	printf("  -p POLL-IVAL (in seconds)\n");
	printf("	intervall to poll the player for state changes (def: 2)\n");
	printf("  -e PP-ENC (see 'iconv -l' for a list of valid encodings)\n");
	printf("	encoding of data from player proxy (def: UTF8)\n");
}

static int
rem_server_init()
{
	
	////// signal handler //////

	if (signal(SIGINT, rem_server_sigint) == SIG_ERR) {
		LOG_ERRNO("install sighandler for SIGINT failed");
		return -1;
	}
	
	////// setup the rfcomm server socket //////
	
	u_int8_t port_rfcomm = 0; // use first free port
	ss = rfcomm_srv_sock_setup(&port_rfcomm, 5);
	if (ss < 0) {
		LOG_FATAL("creating bt socket failed\n");
		return -1;
	}
	
	////// setup and register bt-sdp-service //////
	
	sdp_session = sdp_svc_add_spp(port_rfcomm, REM_SVC_NAME, REM_SVC_DESC,
						REM_SVC_PROV, REM_SVC_UUID);
	if (sdp_session == NULL) {
		LOG_ERROR("register service spp failed\n");
		return -1;
	}
	
	////// misc //////
	
	REM_PSB_RESET(&ps_bin, REM_PS_STATE_OFF); // init player state
	
	rem_pp_init(); // init player proxy
	
	LOG_NOISE("init ok\n");

	interrupted = 0;
	
	return 0;
}

static void
rem_server_sigint(int num)
{
	interrupted = 1;
}

static void
rem_server_shutdown()
{
	int i;
	struct rem_client *rec;
	
	LOG_INFO("shutting down..\n");

	// deregister service from sdp-db
	if (sdp_session) {
		sdp_svc_del(sdp_session);
	}
	
	REM_PSB_RESET(&ps_bin, REM_PS_STATE_SRVOFF);
	REM_PSB_SET_PLINCL(&ps_bin, 1);
	rem_server_tx_ps_broadcast();
	REM_SLEEP_MS(100);	// before closing client connections, give clients
			// a chance to rx final player state

	// disconnect all clients
	for (i = 0; i < REM_MAX_CLIENTS; i++) {
		rec = client_list[i];
		if (rec && rec->rfcc.sock > 0) {
			LOG_DEBUG("disconnect client %s\n", rec->rfcc.addr_str);
			close(rec->rfcc.sock);
		}
		free(rec);
		client_list[i] = NULL;
	}
	
	// shutdown server socket
	if (ss > 0) {
		LOG_DEBUG("closing server socket\n");
		close(ss);
	}
	
	rem_pp_dispose();
	
}

///////////////////////////////////////////////////////////////////////////////
//
// functions - data exchange
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Connects and registers the client whihc currently reuqests a connection on
 * the server socket.
 * 
 * @return
 * 	-1 on failure (too much clients or io-error)
 * 	 0 on success
 */
static int
rem_server_client_connect()
{
	int i;
	struct rem_client *rec;
	
	rec = malloc(sizeof(struct rem_client));
	if (rec == NULL) {
		LOG_ERROR("malloc failed\n");
		return -1;
	}
	memset(rec, 0, sizeof(struct rem_client));
	
	if (rfcomm_srv_sock_accept(ss, &rec->rfcc) < 0) {
		LOG_ERROR("accepting client-connection failed\n");
		free(rec);
		return -1;
	}

	for (i = 0; i < REM_MAX_CLIENTS && client_list[i]; i++);
	if (i == REM_MAX_CLIENTS) {
		LOG_ERROR("too much clients, reject %s\n", rec->rfcc.addr_str);
		close(rec->rfcc.sock);
		free(rec);
		return -1;
	}

	client_list[i] = rec;

	LOG_INFO("client %s connected\n", rec->rfcc.addr_str);
	
	return 0;

}

/**
 * Disconnects client 'rec' and releases related resources.
 * @return
 * 	-1 on failure (client 'rec' not known, no resources will be released)
 * 	 0 on success
 */
static int
rem_server_client_disconnect(struct rem_client *rec)
{
	int i;
	
	for (i = 0; i < REM_MAX_CLIENTS && client_list[i] != rec; i++);
	
	if (i == REM_MAX_CLIENTS)
		return -1;

	if (rec->rfcc.sock) close(rec->rfcc.sock);
		
	free(rec);
	client_list[i] = NULL;
	
	LOG_INFO("client %s disconnected\n", rec->rfcc.addr_str);
	
	return 0;
}

/**
 * Send the current player state to a certain client.
 */
static int rem_server_tx_ps_unicast(struct rem_client *rec)
{
	int ret;
	u_int8_t *pl_conv, *pl_orig;
	u_int32_t pl_conv_size = 0, pl_orig_size = 0;
	
	LOG_NOISE("send ps to client %s\n", rec->rfcc.addr_str);
	
	pl_orig = NULL;
	
	// convert playlist encoding (if needed)
	if (strcmp(rec->ci.enc, rsc.pp_enc) && REM_PSB_GET_PLINCL(&ps_bin)) {
		ret = rem_convert_pl_enc(&ps_bin, &pl_conv, &pl_conv_size,
						rsc.pp_enc, rec->ci.enc);
		if (ret < 0) {
			LOG_WARN("send playlist unconverted\n");
		} else {
			LOG_NOISE("converted pl takes %u bytes\n", pl_conv_size);
			pl_orig = ps_bin.pl;
			pl_orig_size = REM_PSB_GET_PLSIZE(&ps_bin);
			ps_bin.pl = pl_conv;
			REM_PSB_SET_PLSIZE(&ps_bin, pl_conv_size);
		}
	}
	
	// send player state
	ret = rem_send_ps(rec->rfcc.sock, &ps_bin);
	
	// reset to unconverted playlist (if needed)
	if (pl_orig) {
		ps_bin.pl = pl_orig;
		REM_PSB_SET_PLSIZE(&ps_bin, pl_orig_size);
		free(pl_conv);
	}

	return ret;
	
}

/**
 * Send the current player state to all connected clients.
 */
static void rem_server_tx_ps_broadcast()
{
	struct rem_client *rec;
	int i, ret;
	
	for (i = 0; i < REM_MAX_CLIENTS; i++) {
		rec = client_list[i];
		if (!rec) continue;
		do {
			ret = rem_server_tx_ps_unicast(rec);
		} while (ret == REM_IORET_RETRY && !interrupted);
		switch (ret) {
			case REM_IORET_CONN_CLOSE:
				LOG_WARN("lost connection to client %s\n", 
							rec->rfcc.addr_str);
				rem_server_client_disconnect(rec);
				break;
			case REM_IORET_ERROR:
				LOG_ERROR("errors on connection to client %s "
					"-> closing\n", rec->rfcc.addr_str);
				rem_server_client_disconnect(rec);
				break;
			case REM_IORET_RETRY: // interrupted == 1
			case REM_IORET_OK:
				break;
			default:
				LOG_WARN("unexpected return-val %i\n", ret);
				break;
		}
	}
}

/**
 * Adds the Remuco client 'rec' to the client list.
 * 
 * @return
 * 	-1 on failure (client list is full)
 * 	 0 on success
 */
/**
 * Removes the Remuco client 'rec' from the client list.
 * 
 * @return
 * 	-1 on failure (client 'rec' not in client list)
 * 	 0 on success
 */
static int
rem_server_rx_client_data(struct rem_client *rec)
{
	int ret;
	struct rem_pp_pc pc;

	// case 1: receive client info

	if (rec->ci.enc[0] == 0) { // did not yet rx'ed any client info
		
		// get infos about client
		
		do {
			ret = rem_recv_ci(rec->rfcc.sock, &rec->ci);
		} while (ret == REM_IORET_RETRY && !interrupted);
		switch (ret) {
			case REM_IORET_CONN_CLOSE:
				LOG_INFO("lost connection to client %s\n",
							rec->rfcc.addr_str);
				rem_server_client_disconnect(rec);
				return 0;
			case REM_IORET_ERROR:
				LOG_ERROR("errors on connection to client %s "
					"-> closing\n", rec->rfcc.addr_str);
				rem_server_client_disconnect(rec);
				return 0;
			case REM_IORET_RETRY:
				// interrupted == 1 => we are shutting down
				return 0;
			case REM_IORET_OK:
				break;
			default:
				LOG_WARN("unexpected return-val %i\n", ret);
				break;
		}
		
		// send initial player state

		LOG_NOISE("send first ps to client %s\n", rec->rfcc.addr_str);
		REM_PSB_SET_PLINCL(&ps_bin, 1);
		do {
			ret = rem_server_tx_ps_unicast(rec);
		} while (ret == REM_IORET_RETRY && !interrupted);
		switch (ret) {
			case REM_IORET_CONN_CLOSE:
				LOG_WARN("lost connection to client %s\n", 
							rec->rfcc.addr_str);
				rem_server_client_disconnect(rec);
				break;
			case REM_IORET_ERROR:
				LOG_ERROR("errors on connection to client %s "
					"-> closing\n", rec->rfcc.addr_str);
				rem_server_client_disconnect(rec);
				break;
			case REM_IORET_RETRY:
			case REM_IORET_OK:
				break;
			default:
				LOG_WARN("unexpected return-val %i\n", ret);
				break;
		}
		return 0;
	}

	// case 2: receive player control

	do {
		ret = rem_recv_pc(rec->rfcc.sock, &pc);
	} while (ret == REM_IORET_RETRY && !interrupted);
	switch (ret) {
		case REM_IORET_CONN_CLOSE:
			LOG_INFO("lost connection to client %s\n\n",
							rec->rfcc.addr_str);
			rem_server_client_disconnect(rec);
			return 0;
		case REM_IORET_ERROR:
			LOG_ERROR("errors on connection to client %s "
					"-> closing\n", rec->rfcc.addr_str);
			rem_server_client_disconnect(rec);
			return 0;
		case REM_IORET_RETRY: // interrupted == 1
			return 0;
		case REM_IORET_OK:
			break;
		default:
			LOG_WARN("unexpected return-val %i\n", ret);
			break;
	}
	
	if (pc.cmd == REM_PC_CMD_LOGOFF) {
		LOG_INFO("rx'ed logoff command from client %s\n",
							rec->rfcc.addr_str);
		rem_server_client_disconnect(rec);
		return 1;
	}

	rem_pp_process_cmd(&pc);
	
	return 0;
}

/**
 * Having a look into 'man 2 select_tut' there is file descriptor set in use
 * to look for exceptions and this function processes this set used in the
 * select call below .. TODO: cannot imagine a real situation where this
 * function has something to do .. ?
 * 
 */
static int
rem_server_sockets_check(fd_set *sds)
{
	LOG_NOISE("called\n");
	
	int ret, retval, i;
	char c;
	struct rem_client *rec;
	
	retval = 0;
	
	// check rfcomm/bluetooth server socket
	if (FD_ISSET(ss, sds)) {
		ret = recv(ss, &c, 1, MSG_OOB);
		if (ret < 0) {	// socket is down/broken
			LOG_ERROR("server socket broken\n");
			REM_PSB_SET_STATE(&ps_bin, REM_PS_STATE_ERROR);
			retval = -1;
		} else {	// actually not possible
			LOG_WARN("rx'ed OOB data .. ignore\n");
		}
	}

	// check rfcomm/bluetooth client sockets
	for (i = 0; i < REM_MAX_CLIENTS; i++) {
		rec = client_list[i];
		if (!rec) continue;
		if (FD_ISSET(rec->rfcc.sock, sds)) {
			ret = recv(rec->rfcc.sock, &c, 1, MSG_OOB);
			if (ret < 0) {	// socket is down/broken
				LOG_INFO("lost connection to client %s\n",
							rec->rfcc.addr_str);
				rem_server_client_disconnect(rec);
			} else {	// actually not possible
				LOG_WARN("rx'ed OOB data .. ignore\n");
			}
		}
	}
	
	return retval;
}

static int
rem_server_sockets_process(fd_set *sds)
{
	int i;
	struct rem_client *rec;
	
	LOG_NOISE("called\n");

	// check if a clients wants to connect
	if (FD_ISSET(ss, sds)) {
		LOG_DEBUG("process client connection request\n");
		rem_server_client_connect();
	}
	
	// check if a client has some data for us
	for (i = 0; i < REM_MAX_CLIENTS; i++) {
		rec = client_list[i];
		if (!rec) continue;
		if (FD_ISSET(rec->rfcc.sock, sds)) {
			rem_server_rx_client_data(rec);
		}
	}
	
	return 1;
}

///////////////////////////////////////////////////////////////////////////////
//
// main
//
///////////////////////////////////////////////////////////////////////////////

static void
rem_server_main_loop()
{	
	LOG_NOISE("called\n");
	
	int			i, ret, sd_max;
	fd_set			sds_r, sds_e;
	struct rem_client	*rec;
	struct timeval		tv;

	while (!interrupted) {
		
		// reset fd set
		sd_max = 0;
		FD_ZERO(&sds_r); FD_ZERO(&sds_e);
		FD_SET(ss, &sds_r); FD_SET(ss, &sds_e);
		sd_max = sd_max < ss ? ss : sd_max;
		for (i = 0; i < REM_MAX_CLIENTS; i++) {
			rec = client_list[i];
			if (!rec) continue;
			FD_SET(rec->rfcc.sock, &sds_r);
			FD_SET(rec->rfcc.sock, &sds_e);
			sd_max = sd_max < rec->rfcc.sock ?
							rec->rfcc.sock : sd_max;
		}
				
		// wait for any socket activity or timeout
		tv.tv_sec = rsc.ps_poll_ival;
		tv.tv_usec = 0;	
		LOG_NOISE("select..\n");
		ret = select(sd_max + 1, &sds_r, NULL, &sds_e, &tv);
		LOG_NOISE("select returned\n");
		if (ret < 0) {
			if (errno == EINTR) {
				continue; // if there was SIGINT, we won't loop
					  // again, otherwise try to continue
			} else {
				LOG_ERRNO("select failed");
				break;
			}
		}
		if (ret) {
			LOG_DEBUG("there is live on the socks\n");
			if (rem_server_sockets_check(&sds_e) < 0) {
				break;
			}
			if (rem_server_sockets_process(&sds_r) < 0) {
				break;
			}
		}
				
		// poll the player
		//ret = rem_pp_update_ps(&ps);
		ret = rem_update_ps(&ps_bin);
		REM_TEST(ret >= 0, "ret >= 0");
		if (ret <= 0) continue;
		LOG_DEBUG("player state changed\n");
		rem_server_tx_ps_broadcast();
	}
	
	if (interrupted) {
		LOG_INFO("stop looping\n");
	} else {
		LOG_ERROR("error while looping\n");
		rem_server_shutdown();
	}
	
}

int
main(int argc, char **argv)
{
	int			ret;
	char			*str;
	
	if (arg_is_set(argc, argv, "-h") || arg_is_set(argc, argv, "--help")) {
		rem_server_usage();
		return EXIT_OK;
	}
	
	LOG_INFO("Remuco Server " REM_RELEASE_MAIN "." REM_RELEASE_SERVER "\n");
	LOG_INFO("Player Proxy " REM_PP_NAME " " REM_RELEASE_MAIN "."
							REM_RELEASE_PP "\n\n");

	if (arg_get_val_int(argc, argv, "-p", &ret) >= 0)
		rsc.ps_poll_ival = ret;
	else {
		rsc.ps_poll_ival = 2;
	}
	LOG_INFO("poll intervall: %i\n", rsc.ps_poll_ival);

	str = arg_get_val_string(argc, argv, "-e");
	sprintf(rsc.pp_enc, str ? str : REM_PP_ENC_DEF);
	
	////// initialize //////
	
	if (rem_server_init() < 0) {
		LOG_FATAL("starting remuco failed\n");
		return 1;
	}	

	////// start loop //////
	
	rem_server_main_loop();
	
	////// shut down //////

	rem_server_shutdown();
	
	return interrupted ? EXIT_OK : EXIT_ERROR;
}
