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
 * Interface for player proxies.
 * 
 * A player proxy must implement these functions. The functions get called by
 * the Remuco server to get music player state infromation and to control
 * the music player.
 */

#ifndef REMPP_H_
#define REMPP_H_

///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include "rem.h"

///////////////////////////////////////////////////////////////////////////////
//
// macros and constants
//
///////////////////////////////////////////////////////////////////////////////

#define REM_PP_SID_TYPE_UINT	1
#define REM_PP_SID_TYPE_STRING	2

///////////////////////////////////////////////////////////////////////////////
//
// structs
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Union for a song id, which may be a string or an unsigned int.
 */
union rem_pp_sid {
	char		*str;
	unsigned int	uint;
};

/**
 * This structure combines all information, a player proxy is responsible to
 * collect and present the server.
 */
struct rem_pp_ps {
	unsigned int		state,
				volume,
				pl_repeat,
				pl_shuffle,
				pl_pos,
				pl_len;
	u_int8_t		pl_sid_type;
	union rem_pp_sid	*pl_sid_list;// The player's playlist as list of
					     // ids of the songs in the playlist.
					     // These ids will be used to request
				    	     // detailed song information from
				    	     // the player proxy.
};

/**
 * A strcut to store song information.
 * A song contains of a concatenation of tag-names and a related concatenation of
 * tag-values (meta information like artist, title etc.)
 */
struct rem_pp_song {
	int tag_count;
	char *tag_names[REM_MAX_TAGS];
	char *tag_values[REM_MAX_TAGS];
};

/**
 * A struct to store a player command, which consists of a command code and
 * a command parameter.
 */
struct rem_pp_pc {
	u_int16_t	cmd;
	u_int16_t	param;
};

///////////////////////////////////////////////////////////////////////////////
//
// function prototypes
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Init the player proxy.
 * 
 * @return
 * 	-1 on failure (means player proxy is not able to work properly)
 * 	 0 on success
 */
int rem_pp_init(void);

/**
 * Process the player control command.
 * 
 * @param pc
 * 	the player control
 * 
 * @return
 * 	-1 on failure (means further calls to that method are not recommended)
 * 	 0 on success
 */
int rem_pp_process_cmd(struct rem_pp_pc *pc);

/**
 * Do some cleanup work.
 */
void rem_pp_dispose(void);

/**
 * A request by the server to a player proxy to get the current player state.
 * 
 * @param ps
 * 	where to store player state data into
 * 
 * @return
 * 	-1 on failure
 * 	 0 on success
 */
int rem_pp_get_ps(struct rem_pp_ps *ps);

/**
 * If the server has finished processing a player state and does not need the
 * player state data anymore, it calls this function to inform the player
 * proxy that it may release corresponding data/memory (if needed).
 * 
 * @param ps
 * 	the player state data not needed anymore by the server
 */
void rem_pp_free_ps(struct rem_pp_ps *ps);


/**
 * A reuqest by the server to a player proxy to write meta information of song
 * with id 'sid' into the song struct 'song'.
 * 
 * @param sid
 * 	the id of the song to get meta information from
 * @param song
 * 	where to store meta information into
 * 
 * @return
 * 	-1 on failure
 * 	 0 on success
 */
int rem_pp_get_song(const union rem_pp_sid *sid, struct rem_pp_song *song);

#endif /*REMPP_H_*/
