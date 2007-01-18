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
// Summary
//
// Methods for bluetooth related tasks.
//
///////////////////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include <bluetooth/bluetooth.h>
#include <bluetooth/sdp.h>
#include <bluetooth/sdp_lib.h>
#include <bluetooth/rfcomm.h>

#include <unistd.h>

#include "rem-log.h"
#include "rem-bt.h"

///////////////////////////////////////////////////////////////////////////////
//
// macros
//
///////////////////////////////////////////////////////////////////////////////

#define SOCK_RC_SIZE	(sizeof(struct sockaddr_rc))

///////////////////////////////////////////////////////////////////////////////
//
// functions
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Register a serial port profile (SPP) service in service discovery protocol
 * (SDP) database.
 * 
 * @param port
 * 	The port to use for serial port (rfcomm)
 * 	Range: 1 <= port <= 30
 * @param name
 * 	Name of the service
 * @param dsc
 * 	Description of the service
 * @param prov
 * 	Provider of the service
 * @param uuid
 * 	UUID of the service
 * 
 * @return
 * 	The session used to register the service and which may be used to
 * 	deregister the service
 */
sdp_session_t*
sdp_svc_add_spp(u_int8_t port, const char *name, const char *dsc,
				const char *prov, const uint32_t uuid[])
{
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
	sdp_record_t		record;
	sdp_session_t		*session = 0;
	
	/* PART ONE */
	
	// set the general service ID
	sdp_uuid128_create( &svc_uuid, &uuid );
	memset(&record, 0, sizeof(sdp_record_t));
	sdp_set_service_id( &record, svc_uuid );
	
	// set the service class
	sdp_uuid16_create(&svc_class_uuid, SERIAL_PORT_SVCLASS_ID);
	svc_class_list = sdp_list_append(0, &svc_class_uuid);
	sdp_set_service_classes(&record, svc_class_list);
	
	// set the Bluetooth profile information
	memset(&profile, 0, sizeof(sdp_profile_desc_t));
	sdp_uuid16_create(&profile.uuid, SERIAL_PORT_PROFILE_ID);
	profile.version = 0x0100;
	profile_list = sdp_list_append(0, &profile);
	sdp_set_profile_descs(&record, profile_list);
	
	// make the service record publicly browsable
	sdp_uuid16_create(&root_uuid, PUBLIC_BROWSE_GROUP);
	root_list = sdp_list_append(0, &root_uuid);
	sdp_set_browse_groups( &record, root_list );
	
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
	sdp_set_access_protos( &record, access_proto_list );
	
	// set the name, provider, and description
	sdp_set_info_attr(&record, name, prov, dsc);
	
	/* PART TWO */
	
	// connect to the local SDP server, register the service record
	session = sdp_connect( BDADDR_ANY, BDADDR_LOCAL, 0 );
	sdp_record_register(session, &record, 0);
	
	// cleanup
	sdp_data_free( channel );
	sdp_list_free( l2cap_list, 0 );
	sdp_list_free( rfcomm_list, 0 );
	sdp_list_free( root_list, 0 );
	sdp_list_free( access_proto_list, 0 );
	
	return session;
	
}

/**
 * Deregister a servive previously registered with
 * sdp_register_svc_spp().
 * 
 * @param
 * 	the session returned by sdp_register_svc_spp() when the service
 * 	has been registered
 */
void sdp_svc_del(sdp_session_t *session)
{
	sdp_close(session);
}

/**
 * Set up an rfcomm server socket which is ready for accepting client
 * connections.
 * 
 * @param port (in/out parameter)
 * 	- if 1 <= port <= 30 then this function trys to bind to that port
 * 	- if port is out of that range, the first free port is used and port
 * 	  will be set to that number
 * @param npc
 * 	number of pending clients which want to connect
 * @return
 * 	the server socket descriptor or -1 if something failed
 */
int rfcomm_srv_sock_setup(u_int8_t *port, int npc)
{
	struct sockaddr_rc addr_server;
	memset(&addr_server, 0, sizeof(struct sockaddr_rc));
	int s, ret;
	
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
		ret = bind(s, (struct sockaddr *)&addr_server, SOCK_RC_SIZE);
	} else {
		for (*port = 1; *port <= 30; (*port)++) {
			LOG_NOISE("try port %hhu\n", *port);
			addr_server.rc_channel = *port;
			ret = bind(s, (struct sockaddr *) &addr_server,
								SOCK_RC_SIZE);
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

/**
 * Accept a connection request on the rfcomm server socket 'ss' and store
 * connection related information in 'rec'.
 * 
 * @return
 * 	-1 on failue
 *	 0 on success
 */
int
rfcomm_srv_sock_accept(int ss, struct rfcomm_srv_client *rec)
{
	int len;
	
	len = SOCK_RC_SIZE;
	struct sockaddr_rc addr_client;

	rec->sock = accept(ss, (struct sockaddr *) &addr_client, &len);
	if (rec->sock < 0) {
		LOG_ERRNO("accepting connection failed");
		return -1;
	}
	
	memcpy(&rec->addr, &addr_client.rc_bdaddr, sizeof(bdaddr_t));
	ba2str((bdaddr_t*) &rec->addr, rec->addr_str);
	
	return 0;
	
}

