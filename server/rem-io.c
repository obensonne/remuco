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

#include <iconv.h>
#include <unistd.h>
#include <stdlib.h>

#include "rem-io.h"
#include "rem-log.h"
#include "rem-util.h"
#include "rem.h"

/////////////////////////////////////////////////////////////////////////////
//
// constants
//
///////////////////////////////////////////////////////////////////////////////

static int sock_read(int sd, u_int8_t* buf, int len);
static int sock_write(int sd, u_int8_t* buf, int len);
static void rem_dump_song(u_int8_t *sd, int len);
static void rem_dump_pl(struct rem_ps_bin *ps);
static void rem_dump_ps(struct rem_ps_bin *ps, int with_pl);

///////////////////////////////////////////////////////////////////////////////
//
// method - communication
//
///////////////////////////////////////////////////////////////////////////////

int
rem_send_ps(int sd, struct rem_ps_bin *ps)
{
	int ret;
	struct rem_tdhdr tdh;
	u_int8_t *d, d_start[REM_TD_HDR_LEN];
	
	tdh.pver = REM_PROTO_VERSION;
	tdh.dt = REM_DATA_TYPE_PLAYER_STATE;
	tdh.dl = REM_PS_TD_LEN +
		(REM_PSB_GET_PLINCL(ps) ? REM_PSB_GET_PLSIZE(ps) : 0);
	
	d = d_start;
	REM_TD_HDR_SET(d, &tdh);
	
	LOG_DEBUG("sending player state\n");
	#if LOGLEVEL >= LL_NOISE
	rem_dump_ps(ps, REM_PSB_GET_PLINCL(ps));
	#endif
	
	// write transfer header
	ret = sock_write(sd, d_start, REM_TD_HDR_LEN);
	switch (ret) {
		case REM_IORET_CONN_CLOSE:
		case REM_IORET_ERROR:
		case REM_IORET_RETRY:
			return ret;
		case REM_IORET_OK:
			break;
		default:
			LOG_WARN("unexpected return-val %i\n", ret);
			break;
	}

	// write fixed part of player state
	ret = sock_write(sd, ps->fix, REM_PS_TD_LEN);
	switch (ret) {
		case REM_IORET_CONN_CLOSE:
		case REM_IORET_ERROR:
		case REM_IORET_RETRY:
			return ret;
		case REM_IORET_OK:
			break;
		default:
			LOG_WARN("unexpected return-val %i\n", ret);
			break;
	}

	// write playlist
	if (REM_PSB_GET_PLINCL(ps)) {
		ret = sock_write(sd, ps->pl, REM_PSB_GET_PLSIZE(ps));
		switch (ret) {
			case REM_IORET_CONN_CLOSE:
			case REM_IORET_ERROR:
			case REM_IORET_RETRY:
				return ret;
			case REM_IORET_OK:
				break;
			default:
				LOG_WARN("unexpected return-val %i\n", ret);
				break;
		}
	}

	LOG_DEBUG("sent player state\n");	
	return REM_IORET_OK;
}

int
rem_recv_pc(int sd, struct rem_pp_pc *pc)
{
	u_int8_t	buf[255];
	struct rem_tdhdr	th;
	u_int8_t	*data;
	int		ret;

	LOG_DEBUG("receiving player control\n");
	
	ret = sock_read(sd, buf, REM_TD_HDR_LEN + REM_PC_TD_LEN);
	
	if (ret <= 0) {
		return ret;
	}
	
	data = buf;
	
	REM_TD_HDR_GET(data, &th);
	if (th.pver != REM_PROTO_VERSION) {
		LOG_ERROR("protocol version mismatch\n");
		return REM_IORET_ERROR;
	}
	if (th.dt != REM_DATA_TYPE_PLAYER_CTRL) {
		LOG_ERROR("unexpected data type\n");
		return REM_IORET_ERROR;
	}
	LOG_NOISE("the hdr I've read says there are %u bytes of data type %hhu"
		" on the line\n", th.dl, th.dt);

	REM_PC_TD_GET(data, pc);
	
	LOG_DEBUG("received player control\n");
	return REM_IORET_OK;
}

int rem_recv_ci(int sd, struct rem_ci *ci)
{
	u_int8_t		buf[REM_TD_HDR_LEN + REM_CI_TD_LEN + 2];
	struct rem_tdhdr	th;
	u_int8_t		*data;
	int			ret;

	LOG_DEBUG("receiving client info\n");
	
	ret = sock_read(sd, buf, REM_TD_HDR_LEN + REM_CI_TD_LEN);
	
	if (ret <= 0) {
		return ret;
	}
	
	data = buf;
	
	REM_TD_HDR_GET(data, &th);
	if (th.pver != REM_PROTO_VERSION) {
		LOG_ERROR("protocol version mismatch\n");
		return REM_IORET_ERROR;
	}
	if (th.dt != REM_DATA_TYPE_CLIENT_INFO) {
		LOG_ERROR("unexpected data type\n");
		return REM_IORET_ERROR;
	}
	LOG_NOISE("the hdr I've read says there are %u bytes of data type %hhu"
		" on the line\n", th.dl, th.dt);

	REM_CI_TD_GET(data, ci);
	
	LOG_DEBUG("received client info (enc: %s)\n", ci->enc);
	
	return REM_IORET_OK;
}

/**
 * @return
 * 	-2: connection relaed to sd is closed
 * 	-1: reading failed (connection seems broken)
 * 	 0: not enough data on line or interrupted while reading
 * 	 1: data successfully read
 */
static int
sock_read(int sd, u_int8_t* buf, int len)
{
	int ret = 0;

	LOG_NOISE("called\n");
	
	do {
		errno = 0;
		ret = read(sd, buf, len);
		LOG_NOISE("read returned %i\n", ret);
		if (ret <= 0 || ret == len) break;
		REM_SLEEP_MS(50);
		buf += ret;
		len -= ret;
	} while(1);
	if (ret == 0) {
		LOG_DEBUG("EOF on socket\n");
		return REM_IORET_CONN_CLOSE;
	} else if (errno == EINTR /* ret < 0 */) {
		LOG_WARN("interrupted before reading\n");
		return REM_IORET_RETRY;
	} else if (errno /* ret < 0 */) {
		LOG_ERRNO("IO error on socket");
		return REM_IORET_ERROR;
	} else { /* ret > 0 */
		return REM_IORET_OK;
	}
}

/**
 * @return
 * 	-2: connection relaed to sd is closed
 * 	-1: writing failed (connection seems broken)
 * 	 0: writing failed, but connection is probably ok, next read may success
 * 	 1: data successfully written
 */
static int
sock_write(int sd, u_int8_t* buf, int len)
{
	int ret;
	
	LOG_NOISE("called\n");
	
	errno = 0;
	ret = write(sd, buf, len);
	LOG_NOISE("write returned %i\n", ret);
	if (ret >= 0 && ret < len) {
		LOG_WARN("could not send all data\n");
		return REM_IORET_RETRY;
	} else if (errno == EINTR) {
		LOG_WARN("interrupted before writing\n");
		return REM_IORET_RETRY;
	} else if (errno == EPIPE) {
		LOG_DEBUG("reading end of socket closed\n");
		return REM_IORET_CONN_CLOSE;
	} else if (errno) {
		LOG_ERRNO("IO error on socket");
		return REM_IORET_ERROR;
	} else {	// ret == len, ok
		return REM_IORET_OK;
	}
}

///////////////////////////////////////////////////////////////////////////////
//
// method (private) - dumping various data
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Dumps a song in binary format. Prints out whole strings (tags) and each byte
 * as hex value.
 * 
 * @param sd
 * 	ptr to begin of data (not including song data size)
 * @len
 * 	song data size in bytes
 */
static void
rem_dump_song(u_int8_t *sd, int len)
{
	int i, j, n;
	u_int8_t *ptr, *sd_end;
	sd_end = sd + len;
	printf("---------- SONG DATA (%i bytes) BEGIN -\n", len);
	for (i = 0, ptr = sd; ptr < sd_end; i++, ptr += strlen((char*) ptr) + 1) {
		if (i % 2) {
			printf("%s\n", (char*) ptr);
			n = strlen((char*) ptr);
			for (j = 0; j < n; j++)
				printf("%hhX ", ptr[j]);
			printf("\n");
		} else {
			printf("%-20s : ", (char*) ptr);
		}
	}
	printf("---------- SONG DATA END -----------------\n");
}

static void
rem_dump_pl(struct rem_ps_bin *ps)
{
	int i;
	u_int32_t len = 0;
	u_int8_t *ptr, *pl_data_end;
	pl_data_end = ps->pl + REM_PSB_GET_PLSIZE(ps);
	i = 0;
	ptr = ps->pl;
	while (i < REM_PSB_GET_PLLEN(ps) && ptr < pl_data_end) {			
		len = ntohl(*((u_int32_t*) (ptr)));
		rem_dump_song(ptr + 4, len);
		i++;
		ptr += len + 4;
	}
	if (i < REM_PSB_GET_PLLEN(ps)) {
		LOG_WARN("end of pl data but did not dumped all songs yet\n");
	}
	if (ptr < pl_data_end) {
		LOG_WARN("dumped all songs but there is still pl data\n");
	}
	if (ptr > pl_data_end) {
		LOG_WARN("pl data size and song sizes are not aligned\n");
	}
}

static void
rem_dump_ps(struct rem_ps_bin *ps, int with_pl)
{
	printf("XXXXXXXXXX PLAYERSTATE DATA BEGIN XXXXXXXX\n");
	printf("state   = %hhu\n", REM_PSB_GET_STATE(ps));
	printf("volume  = %hhu\n", REM_PSB_GET_VOLUME(ps));
	printf("flags   = %hhu\n", REM_PSB_GET_FLAGS(ps));
	printf("pl_incl = %hhu\n", REM_PSB_GET_PLINCL(ps));
	printf("pl_pos  = %hu\n",  REM_PSB_GET_PLPOS(ps));
	printf("pl_len  = %hu\n",  REM_PSB_GET_PLLEN(ps));
	printf("pl_size = %u\n",   REM_PSB_GET_PLSIZE(ps));
	printf("pl_data = %p\n",   ps->pl);
	if (with_pl) {
		rem_dump_pl(ps);
	}
	printf("XXXXXXXXXX PLAYERSTATE DATA END XXXXXXXXXX\n");
}

///////////////////////////////////////////////////////////////////////////////
//
// Character Encoding Conversion
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Convert a playlist from one encoding to another.
 * See output of command 'iconv -l' for a list of possible encoding names to use
 * for params 'enc_from' and 'enc_to'.
 * 
 * @param ps
 * 	The player state containing the playlist to convert.
 * @param pl_c
 * 	This is an out-parameter. The pointer will point to where the converted
 * 	playlist is stored (memory is allocated within this function).
 * @param pl_csize
 * 	This is an out-parameter. It will be set with the size in bytes of the
 * 	converted playlist.
 * @param enc_from
 * 	Character encoding name of playlist in 'ps'
 * @param enc_to
 * 	Character encoding name of converted playlist.
 * 
 * @return -1 on error, 1 on success
 */
int
rem_convert_pl_enc(struct rem_ps_bin *psb, u_int8_t **pl_c, u_int32_t *pl_c_size,
	char* enc_from, char* enc_to)
{
	LOG_DEBUG("from %s to %s\n", enc_from, enc_to);
	
	int ret;
	unsigned int i;
	size_t bytes_in_left, bytes_out_left, bytes_out_left_pre;
	char *ptr_pl_c, *buf_in, *buf_out, *song_size_pos;
	u_int32_t song_size, pl_size;
	u_int16_t pl_len;
	
	pl_size = REM_PSB_GET_PLSIZE(psb);
	pl_len = REM_PSB_GET_PLLEN(psb);
	
	LOG_NOISE("will convert %u songs (%u bytes at all)\n", pl_len, pl_size);
	
	iconv_t ic = iconv_open(enc_to, enc_from);
	if (ic == (iconv_t)(-1)) {
		LOG_ERRNO("conversion failed");
		return -1;
	}
	
//	#if LOGLEVEL >= LL_NOISE
//	LOG_NOISE("playlist to convert:\n");
//	for (i=0; i < pl_size; i++)
//		printf("%hhX ", ps->pl_data[i]);
//	printf("\n");
//	#endif

	// alloc enoug space for converted playlist (which may need more space
	// e.g. if conversion from ISO-8859-X to UTF-8)
	ptr_pl_c = malloc(pl_size * 2);
	if (ptr_pl_c == NULL) {
		LOG_ERROR("malloc failed\n");
		return -1;
	}
	memset(ptr_pl_c, 0, pl_size * 2);
	
	buf_out = ptr_pl_c;
	bytes_out_left = pl_size * 2;
	buf_in = (char*) psb->pl;
	for (i = 0; i < pl_len; i++) {
		LOG_NOISE("converting song %i .. ", i);
		bytes_in_left = ntohl(*((u_int32_t*) buf_in));
		buf_in += 4;
		song_size_pos = buf_out;
		buf_out += 4;
		bytes_out_left -= 4;
		bytes_out_left_pre = bytes_out_left;
		ret = iconv(ic, &buf_in, &bytes_in_left, &buf_out, &bytes_out_left);
		if (ret < 0) {
			LOG_ERRNO("conversion failed at input byte %i (%hhX)",
				(void*) buf_in - (void*) psb->pl, buf_in[0]);
			*pl_c_size = 0;
			*pl_c = NULL;
			free(ptr_pl_c);
			iconv_close(ic);
			return -1;
		}
		
		song_size = htonl(bytes_out_left_pre - bytes_out_left);
		memcpy(song_size_pos, &song_size, 4);
		#if LOGLEVEL >= LL_NOISE
		printf("ok\n");
		#endif
	}
	
	*pl_c_size = (pl_size * 2) - bytes_out_left;
	*pl_c = realloc(ptr_pl_c, *pl_c_size);

	LOG_NOISE("allocated %u bytes for converted pl, used %u\n", (pl_size * 2), *pl_c_size);

//	#if LOGLEVEL >= LL_NOISE
//	LOG_NOISE("playlist aftrer conversion:\n");
//	for (i=0; i < *pl_c_size; i++)
//		printf("%hhX ", (*pl_c)[i]);
//	printf("\n");
//	#endif

	iconv_close(ic);
	return 1;
}

///////////////////////////////////////////////////////////////////////////////
//
// methods - working on binary data
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Returns the size in bytes a song needs in binary format.
 */
int
rem_song_get_bin_size(struct rem_pp_song *song)
{
	int size = 0, i;
	
	if (!song) return 0;
	
	for (i = 0; i < song->tag_count; i++) {
		size += strlen(song->tag_names[i]) + 1;
		size += strlen(song->tag_values[i]) + 1;
	}
	
	return size;
}

/**
 * Writes a song in binary format into the byte array at position 'ba'.
 * !!! Before calling this method, check if the song data does not exceed the
 * allocated memory at 'ba' with the method 'rem_song_get_bin_size()' !!!
 * 
 * @return
 * 	size in bytes of binary data needed or < 0 if something failed
 */
int
rem_song_get_bin(struct rem_pp_song *song, u_int8_t *ba)
{
	int i, ret;
	u_int8_t *p = ba;
	
	for (i = 0; i < song->tag_count; i++) {
		ret = sprintf((char*) p, "%s", song->tag_names[i]);
		if (ret < 0) return -1;
		p += (unsigned int) ret + 1;
		ret = sprintf((char*) p, "%s", song->tag_values[i]);
		if (ret < 0) return -1;
		p += (unsigned int) ret + 1;
	}
	
	return (p - ba);
}

