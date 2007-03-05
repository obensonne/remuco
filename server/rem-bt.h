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
 
#ifndef LIBBT_H_
#define LIBBT_H_

///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include <bluetooth/sdp.h>
#include <bluetooth/sdp_lib.h>
#include <sys/types.h>

///////////////////////////////////////////////////////////////////////////////
//
// structs
//
///////////////////////////////////////////////////////////////////////////////

struct rfcomm_srv_client {
	int		sock;
	char		addr_str[19];
	struct btaddr {
		uint8_t b[6];
	}		addr;
};

///////////////////////////////////////////////////////////////////////////////
//
// functions
//
///////////////////////////////////////////////////////////////////////////////

extern sdp_session_t*
sdp_svc_add_spp(u_int8_t port, const char *name, const char *dsc,
				const char *prov, const uint32_t uuid[]);

extern void
sdp_svc_del(sdp_session_t *session);


extern int
rfcomm_srv_sock_setup(u_int8_t *port, int npc);

extern int
rfcomm_srv_sock_accept(int ss, struct rfcomm_srv_client *rfcc);

///////////////////////////////////////////////////////////////////////////////

#endif /*LIBBT_H_*/
