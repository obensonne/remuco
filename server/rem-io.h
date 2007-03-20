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

/**
 * This header file describes IO related constants, macros and methods.
 * Further it documents the Remuco data exchange protocol.
 */

#ifndef REMIO_H_
#define REMIO_H_

//////////////////////////////////////////////////////////////////////////////
//
// includes
//
//////////////////////////////////////////////////////////////////////////////

#include <netinet/in.h>	// htons()
#include <sys/types.h>	// u_int8_t, ...

#include "rem.h"
#include "rem-pp.h"

//////////////////////////////////////////////////////////////////////////////
//
// transfer data: constants
//
//////////////////////////////////////////////////////////////////////////////

#define REM_FEATURE_PL_BROWSE		0x0001
#define REM_FEATURE_PL_REPEAT		0x0002
#define REM_FEATURE_PL_SHUFFLE		0x0004
#define REM_FEATURE_RATING		0x0008

#define REM_TRANSFER_PREAMBLE		0xFF00FF00

#define REM_PROTO_VERSION		0x03

#define REM_DATA_TYPE_PLAYER_CTRL	0x01
#define REM_DATA_TYPE_PLAYER_STATE	0x02
#define REM_DATA_TYPE_CLIENT_INFO	0x03
#define REM_DATA_TYPE_PLAYER_INFO	0x04
#define REM_DATA_TYPE_NULL		0x10
#define REM_DATA_TYPE_UNKNOWN		0xFF

//////////////////////////////////////////////////////////////////////////////
//
// transfer data: macros and more
//
// bytes	num	content
//         0	  1	remuco protocol version
//         1	  1	type of data after the header
//   2 -   5	  4	size of data after the header in bytes
//   6 - ???	???	data (if any)
//
// at all: 6 bytes + variable number of bytes
//
//////////////////////////////////////////////////////////////////////////////

#define REM_TD_HDR_LEN	6

struct rem_tdhdr {
	u_int8_t	pver;
	u_int8_t	dt;
	u_int32_t	dl;
};

/** 
 * Writes the transfer data header _tdh as byte array in net byte order to
 * _d (where at least REM_TD_HDR_LEN bytes must be available)
 * So byte order translations are handled by this macro.
 */
#define REM_TD_HDR_SET(_d, _tdh) do {			\
	u_int32_t *p32;					\
	*(_d) = (_tdh)->pver;		(_d)++;		\
	*(_d) = (_tdh)->dt;		(_d)++;		\
	p32 = (u_int32_t *) _d;				\
	*(p32) = htonl((_tdh)->dl);	(_d)+=4;	\
} while(0)


/** 
 * Sets the transfer data header _tdh as read from the byte array in net byte
 * order at _d.
 * So byte order translations are handled by this macro.
 */
#define REM_TD_HDR_GET(_d, _tdh) do {					\
	(_tdh)->pver = *(_d);				(_d)++;		\
	(_tdh)->dt = *(_d);				(_d)++;		\
	(_tdh)->dl = ntohl(*((u_int32_t *) _d));	(_d)+=4;	\
} while(0)

//////////////////////////////////////////////////////////////////////////////
//
// player state: as binary transfer data
//
// bytes	num	content
//         0	  1	state
//         1	  1	volume (in percent)
//         2	  1	flags
//         3	  1	playlist included in transfer data
//   4 -   5	  2	playlist position
//   6 -   7	  2	playlist length (only valid if byte 3 != 0)
//   8 -  11	  4	size of following playlist data in bytes (if any)
//						(only valid if byte 3 != 0)
//  12 - ???	???	playlist data as a sequence of songs
//
// Songs have the following format when transfered:
// bytes	num	content
//   0 -   3	  4	size of following song data in bytes
//   4 - ???	???	song data as concatenation of tag-name and -value pairs
//			each pair consists of 2 concatenated null-temrinated
//			strings
//
// at all: 12 bytes + variable number of bytes (playlist data)
//
// Whether or not there is playlist data after the first 12 bytes depends
// on the byte 3.
//
//////////////////////////////////////////////////////////////////////////////

#define REM_PS_TD_LEN		12		// at least (without playlist
						// data which has no fixed size)

struct rem_ps_bin {
	u_int8_t		fix[REM_PS_TD_LEN];
	u_int8_t		*pl;
};

// these are mainly for debugging (but not only :)

#define REM_PSB_GET_STATE(_psb) ((_psb)->fix[0])
#define REM_PSB_GET_VOLUME(_psb) ((_psb)->fix[1])
#define REM_PSB_GET_FLAGS(_psb) ((_psb)->fix[2])
#define REM_PSB_GET_PLINCL(_psb) ((_psb)->fix[3])
#define REM_PSB_GET_PLPOS(_psb) (ntohs(*((u_int16_t*)((_psb)->fix + 4))))
#define REM_PSB_GET_PLLEN(_psb) (ntohs(*((u_int16_t*)((_psb)->fix + 6))))
#define REM_PSB_GET_PLSIZE(_psb) (ntohl(*((u_int32_t*)((_psb)->fix + 8))))

#define REM_PSB_SET_STATE(_psb, _val) ((_psb)->fix[0] = (u_int8_t) (_val))
#define REM_PSB_SET_VOLUME(_psb, _val) ((_psb)->fix[1] = (u_int8_t) (_val))
#define REM_PSB_SET_FLAGS(_psb, _val) ((_psb)->fix[2] = (u_int8_t) (_val))
#define REM_PSB_SET_PLINCL(_psb, _val) ((_psb)->fix[3] = (u_int8_t) (_val))
#define REM_PSB_SET_PLPOS(_psb, _val) \
	(*((u_int16_t*)((_psb)->fix + 4)) = (u_int16_t) htons(_val))
#define REM_PSB_SET_PLLEN(_psb, _val) \
	(*((u_int16_t*)((_psb)->fix + 6)) = (u_int16_t) htons(_val))
#define REM_PSB_SET_PLSIZE(_psb, _val) \
	(*((u_int32_t*)((_psb)->fix + 8)) = (u_int32_t) htonl(_val))

#define REM_PSB_RESET(_psb, _state) do {		\
	memset((_psb)->fix, 0, REM_PS_TD_LEN);		\
	free((_psb)->pl);				\
	(_psb)->pl = NULL;				\
	REM_PSB_SET_STATE(_psb, _state);		\
} while(0)

//////////////////////////////////////////////////////////////////////////////
//
// player info: as binary transfer data
//
//
//////////////////////////////////////////////////////////////////////////////

#define REM_PI_NAMESTR_LEN	256
#define REM_PI_TD_LEN		(REM_PI_NAMESTR_LEN + 2)

struct rem_pi {
	u_int16_t	features;
	u_int16_t	padding;
	char		name[REM_PI_NAMESTR_LEN];
};

#define REM_PI_TD_SET(_d, _pi) do { 			\
	(*((u_int16_t*)(_d))) = htons((_pi)->features); \
	((u_int8_t*)(_d)) += 2; 			\
	((u_int8_t*)(_d)) += 2; 			\
	memset(_d, 0, REM_PI_NAMESTR_LEN);		\
	memcpy(_d, (_pi)->name, REM_PI_NAMESTR_LEN);	\
	((u_int8_t*)(_d)) += REM_PI_NAMESTR_LEN; 	\
} while(0);

//////////////////////////////////////////////////////////////////////////////
//
// player control: as transfer data
//
// bytes	num	content
//   0 -   1	  2	command code
//   2 -   3	  2	command param
// 
// at all:   4 bytes
//
//////////////////////////////////////////////////////////////////////////////

#define REM_PC_TD_LEN		4

#define REM_PC_TD_GET(_d, _pc) do {					\
	(_pc)->cmd   = ntohs(*((u_int16_t*) _d));	(_d)+=2;	\
	(_pc)->param = ntohs(*((u_int16_t*) _d));	(_d)+=2;	\
} while(0)

//////////////////////////////////////////////////////////////////////////////
//
// client: as transfer data
//
// bytes	num	content
//   0 -   1	  2	maximum playlist len the client is able to handle
//   2 - 257	256	character encoding used by the client (null terminated
//			string)
//
// at all: 258 bytes
//
//////////////////////////////////////////////////////////////////////////////

#define REM_CI_ENCSTR_LEN	256
#define REM_CI_TD_LEN	(REM_CI_ENCSTR_LEN + 2)

struct rem_ci {
	u_int16_t	pl_len_max;
	char		enc[REM_CI_ENCSTR_LEN];
};

#define REM_CI_TD_GET(_d, _ci) do {					\
	(_ci)->pl_len_max = ntohs(*((u_int16_t*) _d));	\
	(_d) += 2;										\
	memset((_ci)->enc, 0, REM_CI_ENCSTR_LEN);		\
	memcpy((_ci)->enc, _d, REM_CI_ENCSTR_LEN);		\
	(_ci)->enc[REM_CI_ENCSTR_LEN - 1] = '\0';		\
	(_d) += REM_CI_ENCSTR_LEN;						\
} while(0)

//////////////////////////////////////////////////////////////////////////////

/*
struct rem_strcol_bin {
	u_int32_t	len;
	u_int8_t	data;
};

#define REM_SC_TD_GET(_d, _sc) do {		\
	(_sc)->len = ntohl((u_int32_t*)(_d));	\
	((u_int8_t*)(_d)) += 4 ;		\
	(_sc)->data = (u_int8_t*) (_d);		\
	((u_int8_t*)(_d)) += (_sc)->len;	\
} while(0)
	
#define REM_SC_TD_SET(_d, _sc) do {		\
	((u_int32_t*)(_d)) = htonl((_sc)->len);	\
	((u_int32_t*)(_d)) = htonl((_sc)->len);	\
	(_sc)->len = ntohl((u_int32_t*)(_d));	\
	((u_int8_t*)(_d)) += 4 ;		\
	(_sc)->data = (u_int8_t*) (_d);		\
	((u_int8_t*)(_d)) += (_sc)->len;	\
} while(0)
*/

//////////////////////////////////////////////////////////////////////////////
//
// functions
//
//////////////////////////////////////////////////////////////////////////////

#define REM_IORET_CONN_CLOSE	-2	// connection is closed
#define REM_IORET_ERROR		-1	// connection has permanent errors
#define REM_IORET_RETRY		 0	// connection has temporary errors
#define REM_IORET_OK		 1	// success

int	rem_send_ps(int sd, struct rem_ps_bin *ps);
int	rem_recv_pc(int sd, struct rem_pp_pc *pc);
int	rem_recv_ci(int sd, struct rem_ci *ci);

int	rem_convert_pl_enc(struct rem_ps_bin *ps, u_int8_t **pl_c,
			u_int32_t *pl_c_size, char* enc_from, char* enc_to);

int	rem_song_get_bin(struct rem_pp_song *song, u_int8_t *ba);
int	rem_song_get_bin_size(struct rem_pp_song *song);

#endif /*REMIO_H_*/
