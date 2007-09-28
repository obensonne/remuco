#ifndef REMDATASV_H_
#define REMDATASV_H_

///////////////////////////////////////////////////////////////////////////////
//
// Includes
//
///////////////////////////////////////////////////////////////////////////////

#include "../../util/rem-common.h"

///////////////////////////////////////////////////////////////////////////////
//
// type definitions
//
///////////////////////////////////////////////////////////////////////////////

typedef struct {
	guint l;	// number of strings
	guint size_bin;	// size of the strings (in bytes, including
			// terminationg null byte of each string)
	gchar **v;	// the strings
	guint hash;
} rem_sv_t;

///////////////////////////////////////////////////////////////////////////////
//
// working with dicts
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Creates a new string vector
 * 
 * @param sv
 * 	the new string vector
 * 
 */
#define rem_sv_new()	g_malloc0(sizeof(rem_sv_t))

/**
 * Removes and frees all strings from the vector and frees tzhe vecotr itself.
 * 
 * @param sv
 * 	the string vector
 * 
 */
static inline void
rem_sv_destroy(rem_sv_t *sv)
{
	guint u;
	
	if (!sv) return;
	
	for (u = 0; u < sv->l; u++) {
		if (sv->v[u]) g_free(sv->v[u]);
	}
	
	g_free(sv->v);
	g_free(sv);
}

static inline void
rem_sv_destroy_body(rem_sv_t *sv)
{
	if (!sv) return;
	
	g_free(sv->v);
	g_free(sv);
}

/**
 * Appends a string to a string vector.
 * 
 * @param sv
 * 	the string vector to append a string to
 * @param s
 * 	the string to append (the string does not get duplicated, so make sure
 * 	it won't be freed elsewhere than via rem_sv_clear() or
 * 	rem_sv_destroy() !)
 * 
 */
static inline void
rem_sv_append(rem_sv_t *sv, gchar* s)
{
	g_assert_debug(sv);
	
	sv->v = g_realloc(sv->v, (sv->l + 1) * sizeof(gchar*));
	
	sv->v[sv->l] = s;
	sv->size_bin += 1 + (s ? strlen(s) + 1 : 0);
	
	//LOG_NOISE("sv hash %p from %08X tp %08X\n", sv, sv->hash,
	//	(sv->hash ^ ((s ? g_str_hash(s) : 0) + sv->l)));
	sv->hash = sv->hash ^ ((s ? g_str_hash(s) : 0) + sv->l);

	sv->l++;
}

//static inline gint
//rem_sv_get_pos_of(rem_sv_t *sv, gchar *val)
//{
//	g_assert(sv);
//	g_assert(pos < sv->l);
//	
//	guint u;
//	
//	for (u = 0; u < sv->l; u++) {
//		if (g_str_equal(val, sv->v[u])) return u;
//	}
//	
//	
//}

/**
 * Removes and frees all strings from the vector.
 * 
 * @param sv
 * 	the string vector
 * 
 */
static inline void
rem_sv_clear(rem_sv_t *sv)
{
	g_assert_debug(sv);

	guint u;

	for (u = 0; u < sv->l; u++) {
		g_free(sv->v[u]);
	}
	
	g_free(sv->v);
	sv->v = NULL;
	sv->l = 0;
	sv->size_bin = 0;
	
	sv->hash = 0;
}

#define rem_sv_equal(_sv1, _sv2) \
	((_sv1) == (_sv2) || ((_sv1) && (_sv2) && (_sv1)->hash == (_sv2)->hash))

//static inline gboolean
//rem_sv_equals(rem_sv_t *sv1, rem_sv_t *sv2)
//{
//	guint u;
//	
//	if (!sv1 && !sv2)
//		return TRUE;
//		
//	if (!sv1 || !sv2)
//		return FALSE;
//	
//	if (sv1->l != sv2->l) {
//		return FALSE;
//	}
//	for (u = 0 ; u < sv1->l; u++) {
//		if (!sv1->v[u] && !sv2->v[u])
//			continue;
//		if (!sv1->v[u] || !sv2->v[u]) {
//			return FALSE;
//		}
//		if (!g_str_equal(sv1->v[u], sv2->v[u])) {
//			return FALSE;
//		}
//	}
//	return TRUE;
//}

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

#ifdef REM_NEED_SERIALIZATION_FUNCTIONS

GByteArray*
rem_sv_serialize(const rem_sv_t *sv,
		      const gchar *se,
		      const rem_sv_t *pte);

rem_sv_t*
rem_sv_unserialize(const GByteArray *ba, const gchar *te);

#endif

///////////////////////////////////////////////////////////////////////////////
//
// debug
//
///////////////////////////////////////////////////////////////////////////////

void
rem_sv_dump(const rem_sv_t *sv);

#endif /*REMDATASV_H_*/
