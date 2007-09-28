#include "rem-library.h"
#include "../basic/rem-bin.h"

rem_library_t*
rem_library_new(void)
{
	rem_library_t *pls;
	
	pls = g_malloc0(sizeof(rem_library_t));
	
	pls->plids = rem_sv_new();
	pls->names = rem_sv_new();
	pls->flags = rem_iv_new();
	
	return pls;
}

void
rem_library_destroy(rem_library_t *pls)
{
	if (!pls) return;
	
	rem_sv_destroy(pls->plids);
	rem_sv_destroy(pls->names);
	rem_iv_destroy(pls->flags);
	
	g_free(pls);
}

void
rem_library_clear(rem_library_t *pls)
{
	g_assert(pls);
	
	rem_sv_clear(pls->plids);
	rem_sv_clear(pls->names);
	rem_iv_clear(pls->flags);
}

/**
 * Get the position of a specific ploblist in the library (the library is just a
 * list of ploblists).
 * 
 * @param pls the library
 * @param plid the ploblist to get the position of
 * 
 * @return the position
 */
gint
rem_library_get_pos(const rem_library_t *pls, const gchar *plid)
{
	guint	u;
	
	if (!pls) return -1;
	
	for (u = 0; u < pls->plids->l; u++) {
		if (g_str_equal(pls->plids->v[u], plid))
			return u;
	}
	
	return -1;
}

void
rem_library_append(rem_library_t *pls, gchar *plid, gchar *name, gint flags)
{
	g_assert(pls && plid && name);
	
	rem_sv_append(pls->plids, plid);
	rem_sv_append(pls->names, name);
	rem_iv_append(pls->flags, flags);
}

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

static const guint rem_library_t_bfv[] = {
	REM_BIN_DT_SV, 2,		// plids and names
	REM_BIN_DT_IV, 1,		// types
	REM_BIN_DT_NONE
};

GByteArray*
rem_library_serialize(const rem_library_t *pls,
			const gchar *se,
			const rem_sv_t *pte)
{
	return rem_bin_serialize(pls, rem_library_t_bfv, se, pte);
}
		
rem_library_t*
rem_library_unserialize(const GByteArray *ba, const gchar *te)
{
	rem_library_t *pls = NULL;
	gint ret;
	
	ret = rem_bin_unserialize(ba, sizeof(rem_library_t),
				rem_library_t_bfv, (gpointer) &pls, te);
		
	if (ret < 0 && pls) {
		rem_library_destroy(pls);
		pls = NULL;
	}
	
	return pls;
}
