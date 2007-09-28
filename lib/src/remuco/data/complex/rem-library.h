#ifndef REMPLOBLISTS_H_
#define REMPLOBLISTS_H_

#include "../basic/rem-sv.h"
#include "../basic/rem-iv.h"

typedef struct {
	rem_sv_t	*plids;
	rem_sv_t	*names;
	rem_iv_t	*flags;
} rem_library_t;

#define REM_LIBRARY_PL_FLAG_EDITABLE	0x0001

///////////////////////////////////////////////////////////////////////////////
//
// creating and destroying ploblists
//
///////////////////////////////////////////////////////////////////////////////

rem_library_t*
rem_library_new(void);

void
rem_library_destroy(rem_library_t *pls);

void
rem_library_clear(rem_library_t *pls);

///////////////////////////////////////////////////////////////////////////////
//
// working with ploblist in a ploblists
//
///////////////////////////////////////////////////////////////////////////////

#define rem_library_get_plid(_pls, _pos) (_pls)->plids->v[_pos]

#define rem_library_get_name(_pls, _pos) (_pls)->names->v[_pos]

#define rem_library_get_flags(_pls, _pos) (_pls)->flags->v[_pos]

gint
rem_library_get_pos(const rem_library_t *pls, const gchar *plid);

#define rem_library_len(_pls) (_pls)->plids->l

void
rem_library_append(rem_library_t *pls, gchar *plid, gchar *name, gint flags);

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

#ifdef REM_NEED_SERIALIZATION_FUNCTIONS

GByteArray*
rem_library_serialize(const rem_library_t *pls,
			const gchar *se,
			const rem_sv_t *pte);
		
rem_library_t*
rem_library_unserialize(const GByteArray *ba, const gchar *te);

#endif

///////////////////////////////////////////////////////////////////////////////
//
// debug
//
///////////////////////////////////////////////////////////////////////////////

#define rem_library_dump(_pls)	LOG_WARN("pls dump not implemented\n")

#endif /*REMPLOBLISTS_H_*/
