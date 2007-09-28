#ifndef REMPLOBLIST_H_
#define REMPLOBLIST_H_

///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include "../../util/rem-common.h"
#include "../basic/rem-sv.h"

///////////////////////////////////////////////////////////////////////////////
//
// types and constants
//
///////////////////////////////////////////////////////////////////////////////

typedef struct {
	gchar		*plid;
	gchar		*name;
	rem_sv_t	*plobs;
} rem_ploblist_t;

#define REM_PLOBLIST_PLID_PLAYLIST	"__PLAYLIST__"
#define REM_PLOBLIST_NAME_PLAYLIST	"Playlist"
#define REM_PLOBLIST_PLID_QUEUE		"__QUEUE__"
#define REM_PLOBLIST_NAME_QUEUE		"Queue"
//#define REM_PLOBLIST_PLID_VOTE		"__VOTE__"
//#define REM_PLOBLIST_NAME_VOTE		"Vote"

///////////////////////////////////////////////////////////////////////////////
//
// creating and destroying a ploblist
//
///////////////////////////////////////////////////////////////////////////////

rem_ploblist_t*
rem_ploblist_new(gchar *plid, gchar *title);

void
rem_ploblist_destroy(rem_ploblist_t *pl);

/**
 * Clears the ploblist. Removes all plobs and all sub ploblists, but keeps plid
 * and name!
 */
void
rem_ploblist_clear(rem_ploblist_t *pl);

///////////////////////////////////////////////////////////////////////////////
//
// working with plobs in a ploblist
//
///////////////////////////////////////////////////////////////////////////////

#define rem_ploblist_len(_pl) ((_pl)->plobs->l / 2)

void
rem_ploblist_append(rem_ploblist_t *pl, gchar* pid, gchar* title);

void
rem_ploblist_append_intpid(rem_ploblist_t *pl, gint pid, gchar* title);

#define rem_ploblist_get_pid(_pl, _n) ((_pl)->plobs->v[(_n) * 2])

gint
rem_ploblist_get_pid_int(const rem_ploblist_t *pl, guint n);

rem_sv_t*
rem_ploblist_get_pids(const rem_ploblist_t *pl);

#define rem_ploblist_get_title(_pl, _n) (_pl)->plobs->v[(_n) * 2 + 1]

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

#ifdef REM_NEED_SERIALIZATION_FUNCTIONS

GByteArray*
rem_ploblist_serialize(const rem_ploblist_t *pl,
		       const gchar *se,
		       const rem_sv_t *pte);

rem_ploblist_t*
rem_ploblist_unserialize(const GByteArray *ba, const gchar *te);

#endif

///////////////////////////////////////////////////////////////////////////////
//
// debug
//
///////////////////////////////////////////////////////////////////////////////

void
rem_ploblist_dump(const rem_ploblist_t *pl);

#endif /*REMPLOBLIST_H_*/
