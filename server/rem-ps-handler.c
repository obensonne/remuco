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
  * DESCRIPTION:
  * 
  * The remuco player state handler is responsible for updating a player state
  * by calling the appropriate player proxy functions and handling their
  * results.
  * 
  */
  
///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include <stdlib.h>
#include <string.h>

#include "rem-ps-handler.h"
#include "rem-pp.h"
#include "rem-util.h"
#include "rem-log.h"

///////////////////////////////////////////////////////////////////////////////
//
// macors and constants
//
///////////////////////////////////////////////////////////////////////////////

#define REM_PL_HASH_SIZE	16

///////////////////////////////////////////////////////////////////////////////
//
// global vars
//
///////////////////////////////////////////////////////////////////////////////

static char	pl_hash[REM_PL_HASH_SIZE];

///////////////////////////////////////////////////////////////////////////////
//
// function prototypes
//
///////////////////////////////////////////////////////////////////////////////

static void
rem_free_song(struct rem_pp_song *song);

static void
rem_free_song_list(struct rem_pp_song *song_list, unsigned int count);

static int
rem_update_pl_hash(char *hash, const struct rem_pp_ps *ps);

static int
rem_reset_ps(struct rem_ps_bin *psb, u_int8_t state);

static int
rem_get_pl_bin(struct rem_pp_ps *ps_pp, u_int8_t **pl_data, u_int32_t *pl_size);


///////////////////////////////////////////////////////////////////////////////
//
// functions public
//
///////////////////////////////////////////////////////////////////////////////

int
rem_update_ps(struct rem_ps_bin *ps_bin)
{
	int			ret, changed = 0;
	u_int8_t		ps_state, ps_volume, ps_flags = 0, *pl_data;
	u_int16_t		ps_pl_pos, ps_pl_len;
	u_int32_t		pl_size;
	struct rem_pp_ps	ps_pp;

	memset(&ps_pp, 0, sizeof(struct rem_pp_ps));
	
	// get the values
	ret = rem_pp_get_ps(&ps_pp);
	if (ret < 0) return rem_reset_ps(ps_bin, REM_PS_STATE_ERROR);

	// check the values
	if (ps_pp.pl_len >  (u_int16_t) 0xFFFF) {
		LOG_ERROR("playlist too long\n");
		rem_pp_free_ps(&ps_pp);
		return rem_reset_ps(ps_bin, REM_PS_STATE_ERROR);
	}
	if ((ps_pp.pl_pos > ps_pp.pl_len) ||
			(ps_pp.pl_len && ps_pp.pl_pos == ps_pp.pl_len)) {
		LOG_ERROR("playlist pos (%i) out of range (%i)\n",
			ps_pp.pl_pos, ps_pp.pl_len);
		rem_pp_free_ps(&ps_pp);
		return rem_reset_ps(ps_bin, REM_PS_STATE_ERROR);
	}
	if (ps_pp.volume > 100) {
		LOG_ERROR("volume out of range\n");
		rem_pp_free_ps(&ps_pp);
		return rem_reset_ps(ps_bin, REM_PS_STATE_ERROR);
	}

	// convert the values
	ps_state = (u_int8_t) ps_pp.state;
	ps_volume = (u_int8_t) ps_pp.volume;
	ps_flags |= ps_pp.pl_repeat ? REM_PS_FLAG_PL_REPEAT : 0;
	ps_flags |= ps_pp.pl_shuffle ? REM_PS_FLAG_PL_SHUFFLE : 0;
	ps_pl_pos = (u_int16_t) ps_pp.pl_pos;
	ps_pl_len = (u_int16_t) ps_pp.pl_len;
	
	// check for playlist change
	ret = rem_update_pl_hash(pl_hash, &ps_pp);
	if (ret < 0) {
		rem_pp_free_ps(&ps_pp);
		return rem_reset_ps(ps_bin, REM_PS_STATE_ERROR);
	}
	if (ret) {	// change in playlist
		ret = rem_get_pl_bin(&ps_pp, &pl_data, &pl_size);
		if (ret < 0) {
			rem_pp_free_ps(&ps_pp);
			return rem_reset_ps(ps_bin, REM_PS_STATE_ERROR);
		}
		REM_PSB_SET_PLINCL(ps_bin, 1);
		REM_PSB_SET_PLSIZE(ps_bin, pl_size);
		free(ps_bin->pl);
		ps_bin->pl = pl_data;
		changed = 1;
	} else {	// no change in playlist, check other values for change
		REM_PSB_SET_PLINCL(ps_bin, 0);
		changed |= ps_state != REM_PSB_GET_STATE(ps_bin);
		changed |= ps_volume != REM_PSB_GET_VOLUME(ps_bin);
		changed |= ps_flags != REM_PSB_GET_FLAGS(ps_bin);
		changed |= ps_pl_pos != REM_PSB_GET_PLPOS(ps_bin);
	}

	rem_pp_free_ps(&ps_pp);

	// no error occured, we finally can assign the values
	if (changed) {
		REM_PSB_SET_STATE(ps_bin, ps_state);
		REM_PSB_SET_VOLUME(ps_bin, ps_volume);
		REM_PSB_SET_FLAGS(ps_bin, ps_flags);
		REM_PSB_SET_PLPOS(ps_bin, ps_pl_pos);
		REM_PSB_SET_PLLEN(ps_bin, ps_pl_len);
	}
		
	return changed;			
}

///////////////////////////////////////////////////////////////////////////////
//
// functions private
//
///////////////////////////////////////////////////////////////////////////////

static void
rem_free_song(struct rem_pp_song *song)
{
	int i;
	for (i = 0; i < song->tag_count; i++) {
		free(song->tag_names[i]);
		song->tag_names[i] = NULL;
		free(song->tag_values[i]);
		song->tag_values[i] = NULL;
	}
}

static void
rem_free_song_list(struct rem_pp_song *song_list, unsigned int count)
{
	unsigned int u;
	for (u = 0; u < count; u++) {
		rem_free_song(&song_list[u]);
	}
	free(song_list);
}

/**
 * Update a playlist's hash value based on the uid of the playlist's songs.
 * Us this if the uids are of type string.
 * 
 * @param hash (in/out param)
 * 	in : the current hash
 * 	out: the new hash
 * @param uid_count
 * @param uid
 * 	array of uids (as strings)
 * 	Note:	This is an array of pointers to strings and not a concatenation
 * 		of strings.
 * @return
 * 	1 : hash changed
 * 	0 : hash did not change
 */
static int
rem_update_pl_hash(char *hash, const struct rem_pp_ps *ps)
{
	unsigned int i, j, n;
	char hash_new[REM_PL_HASH_SIZE], *sid, sid_uint[REM_MAX_STR_LEN];
	memset(hash_new, 0, REM_PL_HASH_SIZE);
	
 	for (n = 0, i = 0; i < ps->pl_len; ++i) {
 		if (ps->pl_sid_type == REM_PP_SID_TYPE_STRING) {
 			sid = ps->pl_sid_list[i].str;
 		} else { // REM_PP_SID_TYPE_UINT
 			snprintf(sid_uint, REM_MAX_STR_LEN, "%u",
 						ps->pl_sid_list[i].uint);
 			sid = sid_uint;
 		}
 		REM_TEST(sid, "sid");
		j = 0;
		while (sid && sid[j] != 0) {
			hash_new[n] = (char) ((hash_new[n] ^ sid[j]) + (i % 8));
			j++;
			n = (n == REM_PL_HASH_SIZE - 1) ? 0 : n + 1;
		}
	}
	if (memcmp(hash, hash_new, REM_PL_HASH_SIZE)) {
		#if LOGLEVEL >= LL_DEBUG
		LOG_DEBUG("pl hash changed: ");
		for (i = 0; i < REM_PL_HASH_SIZE; i++) printf("%02hhX", hash[i]);
		printf(" -> ");
		for (i = 0; i < REM_PL_HASH_SIZE; i++) printf("%02hhX", hash_new[i]);
		printf("\n");
		#endif
		memcpy(hash, hash_new, REM_PL_HASH_SIZE);
		return 1;
	} else {
		return 0;
	}
}

/**
 * Sets a player state to ERROR (next ot setting the state this includes
 * clearing the playlist and related stuff).
 * @return
 * 	0	if 'ps' has been in error state before
 * 	1	otherwise
 */
static int
rem_reset_ps(struct rem_ps_bin *psb, u_int8_t state)
{

	int pl_incl = 0, changed = 0;
	if (REM_PSB_GET_PLLEN(psb)) {
		memset(pl_hash, 0 , REM_PL_HASH_SIZE);
		pl_incl = 1;
		changed = 1;
	}
	if (REM_PSB_GET_STATE(psb) != state) {
		changed = 1;
	}
	
	REM_PSB_RESET(psb, state);
	
	if (pl_incl) {
		REM_PSB_SET_PLINCL(psb, 1);
	}
	
	return changed;
}


static int
rem_get_pl_bin(struct rem_pp_ps *ps_pp, u_int8_t **pl_data, u_int32_t *pl_size)
{
	int		ret;
	unsigned int	u;
	u_int8_t	*p8;
	u_int32_t	song_size, *p32, *song_size_list;
	struct rem_pp_song	*song, *song_list;
	
	*pl_data = NULL;
	*pl_size = 0;
	
	///// empty playlist is easy /////

	if (ps_pp->pl_len == 0) {
		return 0;
	}

	///// get song list and size of each song /////
	
	song_list = calloc(ps_pp->pl_len, sizeof(struct rem_pp_song));
	if (!song_list) {
		LOG_ERROR("calloc failed\n");
		return -1;
	}
	song_size_list = calloc(ps_pp->pl_len, sizeof(u_int32_t));
	if (!song_size_list) {
		LOG_ERROR("calloc failed\n");
		free(song_list);
		return -1;
	}
	for (u = 0; u < ps_pp->pl_len; u++) {
		song = song_list + u;
		ret = rem_pp_get_song(&ps_pp->pl_sid_list[u], song);
		if (ret < 0) {
			free(song_list);
			song_list = NULL;
			return -1;
		}
		song_size_list[u] = rem_song_get_bin_size(song);
		*pl_size += song_size_list[u] + 4;
	}
	
	///// create binary playlist /////
	
	*pl_data = malloc(*pl_size);
	if (*pl_data == NULL) {
		LOG_ERRNO("malloc failed");
		free(song_list);
		free(song_size_list);
		return -1;
	}
	p8 = *pl_data;
	for (u = 0; u < ps_pp->pl_len; u++) {
		song_size = song_size_list[u];
		p32 = (u_int32_t*) p8;
		*p32 = (u_int32_t) htonl(song_size);
		p8 += 4;
		ret = rem_song_get_bin(&song_list[u], p8);
		if (ret < 0) {
			LOG_ERROR("song to binary failed\n");
			rem_free_song_list(song_list, u);
			free(song_size_list);
			free(pl_data);
			return -1;
		}
		p8 += song_size;
		REM_TEST(ret == (int) song_size, "ret == song_len");
	}
	REM_TEST(p8 == *pl_data + *pl_size, "p == pl_data + *pl_size");
	
	LOG_NOISE("converted pl to binary (%u bytes)\n", *pl_size);
	
	///// release resources /////

	rem_free_song_list(song_list, ps_pp->pl_len);
	free(song_size_list);
	
	return 0;
}
