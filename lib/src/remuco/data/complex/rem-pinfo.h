#ifndef REMPINFO_H_
#define REMPINFO_H_

///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include "../../util/rem-common.h"

#include "../basic/rem-sv.h"

///////////////////////////////////////////////////////////////////////////////
//
// types
//
///////////////////////////////////////////////////////////////////////////////

typedef struct {
	gchar		*name;
	gint32		features;
	gint32		rating_max;
	gint32		rating_none;
	GByteArray	*icon;
} rem_pinfo_t;

/* Inspect the current playlist */
#define REM_PINFO_FEATURE_PLAYLIST 0x0001
/* Edit the current playlist */
#define REM_PINFO_FEATURE_PLAYLIST_EDIT 0x0002 // FUTURE FEATURE
/* Jump to a specific song in the current playlist */
#define REM_PINFO_FEATURE_PLAYLIST_JUMP 0x0004
/* Show repeat and shuffle status */
#define REM_PINFO_FEATURE_PLAYLIST_MODE_REPEAT_ONE_PLOB 0x0008
#define REM_PINFO_FEATURE_PLAYLIST_MODE_REPEAT_ALBUM 0x0010
#define REM_PINFO_FEATURE_PLAYLIST_MODE_REPEAT_PLAYLIST 0x0020
#define REM_PINFO_FEATURE_PLAYLIST_MODE_SHUFFLE 0x0040
/* Inspect the play queue */
#define REM_PINFO_FEATURE_QUEUE 0x0080
/* Edit the play queue */
#define REM_PINFO_FEATURE_QUEUE_EDIT 0x0100 // FUTURE FEATURE
/* Jump to a specific song in the play queue */
#define REM_PINFO_FEATURE_QUEUE_JUMP 0x0200
/* Edit the meta information of plobs */
#define REM_PINFO_FEATURE_PLOB_EDIT 0x0400
#define REM_PINFO_FEATURE_PLOB_TAGS 0x0800
/* Seek to some position within the current plob */
#define REM_PINFO_FEATURE_SEEK 0x1000 // FUTURE FEATURE
/* Rate plobs */
#define REM_PINFO_FEATURE_RATE 0x2000
#define REM_PINFO_FEATURE_PLAY_NEXT_CANDIDATE 0x4000 // FUTURE FEATURE
/* Search plobs */
#define REM_PINFO_FEATURE_SEARCH 0x8000 // FUTURE FEATURE
/* Show predefined ploblists and make them the new playlist */
#define REM_PINFO_FEATURE_LIBRARY 0x10000
/* Sow content of a predefined ploblist */
#define REM_PINFO_FEATURE_LIBRARY_PLOBLIST_CONTENT 0x20000


///////////////////////////////////////////////////////////////////////////////
//
// working with mai
//
///////////////////////////////////////////////////////////////////////////////

rem_pinfo_t*
rem_pinfo_new(void);

void
rem_pinfo_destroy(rem_pinfo_t *mai);

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

#ifdef REM_NEED_SERIALIZATION_FUNCTIONS

GByteArray*
rem_pinfo_serialize(const rem_pinfo_t *mai,
		    const gchar *se,
		    const rem_sv_t *pte);

rem_pinfo_t*
rem_pinfo_unserialize(const GByteArray *ba, const gchar *te);

#endif

///////////////////////////////////////////////////////////////////////////////
//
// debug
//
///////////////////////////////////////////////////////////////////////////////

void
rem_pinfo_dump(const rem_pinfo_t *pi);

#endif /*REMPINFO_H_*/
