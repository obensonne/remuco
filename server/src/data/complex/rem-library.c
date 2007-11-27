#include "rem-library.h"
#include "../basic/rem-bin.h"

RemLibrary*
rem_library_new(void)
{
	RemLibrary *lib;
	
	lib = g_slice_new(RemLibrary);
	
	lib->plids = rem_sl_new();
	lib->names = rem_sl_new();
	lib->flags = rem_il_new();
	
	return lib;
}

void
rem_library_destroy(RemLibrary *lib)
{
	if (!lib) return;
	
	rem_sl_destroy(lib->plids);
	rem_sl_destroy(lib->names);
	rem_il_destroy(lib->flags);
	
	g_slice_free(RemLibrary, lib);
}

void
rem_library_clear(RemLibrary *lib)
{
	rem_bapiu_if_fail(lib, "RemLibrary is NULL");
	
	rem_sl_clear(lib->plids);
	rem_sl_clear(lib->names);
	rem_il_clear(lib->flags);
}

void
rem_library_append(RemLibrary *lib,
				   gchar *plid,
				   gchar *name,
				   RemPloblistFlag flags)
{
	rem_bapiu_if_fail(lib && plid && name, "at least one argument is NULL");
	
	rem_sl_append(lib->plids, plid);
	rem_sl_append(lib->names, name);
	rem_il_append(lib->flags, flags);
}

void
rem_library_append_const(RemLibrary *lib,
						 const gchar *plid,
						 const gchar *name,
						 RemPloblistFlag flags)
{
	rem_bapiu_if_fail(lib && plid && name, "at least one argument is NULL");
	
	rem_sl_append_const(lib->plids, plid);
	rem_sl_append_const(lib->names, name);
	rem_il_append(lib->flags, flags);
}

/**
 * Get the name of the playlist 'plid'.
 */
const gchar*
rem_library_get_name(const RemLibrary *pls, const gchar *plid)
{
	g_assert_debug(pls);
	g_assert_debug(plid);
	
	const gchar	*n, *p;
	
	rem_sl_iterator_reset(pls->plids);
	rem_sl_iterator_reset(pls->names);
	
	do {
		p = rem_sl_iterator_next(pls->plids);
		n = rem_sl_iterator_next(pls->names);
		
		if (g_str_equal(plid, p)) return n;
		
	} while(p);
	
	return NULL;
}

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

static const guint REM_LIBRARY_BFV[] = {
	REM_BIN_DT_SV, 2,		// plids and names
	REM_BIN_DT_IV, 1,		// types
	REM_BIN_DT_NONE
};

GByteArray*
rem_library_serialize(const RemLibrary *lib,
					  const gchar *se,
					  const RemStringList *pte)
{
	return rem_bin_serialize(lib, REM_LIBRARY_BFV, se, pte);
}
		
RemLibrary*
rem_library_unserialize(const GByteArray *ba, const gchar *te)
{
	RemLibrary *lib = NULL;
	gint ret;
	
	ret = rem_bin_unserialize(ba, sizeof(RemLibrary),
				REM_LIBRARY_BFV, (gpointer) &lib, te);
		
	if (ret < 0 && lib) {
		rem_library_destroy(lib);
		lib = NULL;
	}
	
	return lib;
}
