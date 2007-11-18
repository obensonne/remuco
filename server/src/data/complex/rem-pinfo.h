#ifndef REMPINFO_H_
#define REMPINFO_H_

///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include <remuco.h>

#include "../../util/rem-util.h"

///////////////////////////////////////////////////////////////////////////////
//
// types
//
///////////////////////////////////////////////////////////////////////////////

typedef enum {
	/** Inspect the current playlist */
	REM_FEATURE_PLAYLIST			= 1 << 0,
	/** Edit the current playlist. <i>FUTURE FEATURE</i> */
	REM_FEATURE_PLAYLIST_EDIT		= 1 << 1,
	/* Jump to a specific song in the current playlist */
	REM_FEATURE_PLAYLIST_JUMP		= 1 << 2,
	/* Show repeat and shuffle status */
	REM_FEATURE_REPEAT_MODE_PLOB	= 1 << 3,
	REM_FEATURE_REPEAT_MODE_ALBUM	= 1 << 4,
	REM_FEATURE_REPEAT_MODE_PL		= 1 << 5,
	REM_FEATURE_SHUFFLE_MODE		= 1 << 6,
	/* Inspect the play queue */
	REM_FEATURE_QUEUE				= 1 << 7,
	/* Edit the play queue. <i>FUTURE FEATURE</i>  */
	REM_FEATURE_QUEUE_EDIT			= 1 << 8,
	/* Jump to a specific song in the play queue */
	REM_FEATURE_QUEUE_JUMP			= 1 << 9,
	/* Edit the meta information of plobs */
	REM_FEATURE_PLOB_EDIT			= 1 << 10,
	REM_FEATURE_PLOB_TAGS			= 1 << 11,
	/* Seek to some position within the current plob */
	REM_FEATURE_SEEK				= 1 << 12, // FUTURE FEATURE
	/* Rate plobs */
	REM_FEATURE_RATE				= 1 << 13,
	REM_FEATURE_PLAY_NEXT			= 1 << 14, // FUTURE FEATURE
	/* Search plobs */
	REM_FEATURE_SEARCH				= 1 << 15, // FUTURE FEATURE
	/* Show predefined ploblists and make them the new playlist */
	REM_FEATURE_LIBRARY				= 1 << 16,
	/* Show content of a predefined ploblist */
	REM_FEATURE_LIBRARY_PL_CONTENT	= 1 << 17,
	/* Play a certain ploblists (Decprecated since this is requires by
	 * REM_FEATURE_LIBRARY) */
	REM_FEATURE_LIBRARY_PL_PLAY		= 1 << 18,
	/* Edit any ploblist (not only playlist or queue). */
	REM_FEATURE_PLOBLIST_EDIT		= 1 << 19
	
} RemPlayerInfoFeature;

/** Player information for a client. */
typedef struct {
	gchar		*name;
	gint32		features;
	gint32		rating_max;
	GByteArray	*icon;
} RemPlayerInfo;

RemPlayerInfo*
rem_player_info_new(const gchar* name);

void
rem_player_info_destroy(RemPlayerInfo *pi);

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

GByteArray*
rem_player_info_serialize(const RemPlayerInfo *mai,
						  const gchar *se,
						  const RemStringList *pte);

RemPlayerInfo*
rem_player_info_unserialize(const GByteArray *ba, const gchar *te);

///////////////////////////////////////////////////////////////////////////////
//
// debug
//
///////////////////////////////////////////////////////////////////////////////

void
rem_player_info_dump(const RemPlayerInfo *pi);

#endif /*REMPINFO_H_*/
