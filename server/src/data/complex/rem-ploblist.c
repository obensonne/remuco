#include "rem-ploblist.h"
#include "../basic/rem-bin.h"

RemPloblist*
rem_ploblist_new(const gchar *plid, const gchar *title)
{
	RemPloblist *pl;
	
	pl = g_slice_new0(RemPloblist);
	
	pl->plid = g_strdup(plid);
	pl->name = g_strdup(title);
	pl->plobs = rem_sl_new();
	
	return pl;
}

void
rem_ploblist_destroy(RemPloblist *pl)
{
	if (!pl) return;
	
	if (pl->plid) g_free(pl->plid);
	if (pl->name) g_free(pl->name);
	
	rem_sl_destroy(pl->plobs);
	
	g_slice_free(RemPloblist, pl);
}

/**
 * Clears the ploblist. Removes all plobs and all sub ploblists, but keeps plid
 * and name!
 */
void
rem_ploblist_clear(RemPloblist *pl)
{
	g_assert(pl);
	
	rem_sl_clear(pl->plobs);
}

void
rem_ploblist_append(RemPloblist *pl, gchar* pid, gchar* title)
{
	g_assert(pl && pid && title);
	
	rem_sl_append(pl->plobs, pid);
	rem_sl_append(pl->plobs, title);
}

void
rem_ploblist_append_const(RemPloblist *pl,
						  const gchar* pid,
						  const gchar* title)
{
	g_return_if_fail(pl && pid && title);
	
	rem_sl_append_const(pl->plobs, pid);
	rem_sl_append_const(pl->plobs, title);
}


RemStringList*
rem_ploblist_get_pids(const RemPloblist *pl)
{
	RemStringList	*pids;
	const gchar	*s;
	
	pids = rem_sl_new();
	
	rem_sl_iterator_reset(pl->plobs);
	
	s = rem_sl_iterator_next(pl->plobs);
	while (s) {
		rem_sl_append_const(pids, s);
		rem_sl_iterator_next(pl->plobs);	// skip title
		s = rem_sl_iterator_next(pl->plobs);
	}
	
	return pids;
}

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

static const guint REM_PLOBLIST_BFV[] = {
	REM_BIN_DT_STR, 2,		// plid and name
	REM_BIN_DT_SV, 1,		// pids and titles of the plobs
	REM_BIN_DT_NONE
};

GByteArray*
rem_ploblist_serialize(const RemPloblist *pl,
					   const gchar *charset_from,
					   const RemStringList *charsets_to)
{
	return rem_bin_serialize(pl, REM_PLOBLIST_BFV, charset_from, charsets_to);
}

RemPloblist*
rem_ploblist_unserialize(const GByteArray *ba, const gchar *charset_to)
{
	RemPloblist *pl = NULL;
	gint ret;
	
	ret = rem_bin_unserialize(ba, sizeof(RemPloblist),
				REM_PLOBLIST_BFV, (gpointer) &pl, charset_to);
		
	if (ret < 0 && pl) {
		rem_ploblist_destroy(pl);
		pl = NULL;
	}
	
	return pl;
}

///////////////////////////////////////////////////////////////////////////////
//
// debug
//
///////////////////////////////////////////////////////////////////////////////

void
rem_ploblist_dump(const RemPloblist *pl)
{
	REM_DATA_DUMP_HDR("RemPloblist", pl);
	
	if (pl) {
		REM_DATA_DUMP_FS("plid: %s, name: %s\n", pl->plid, pl->name);
		REM_DATA_DUMP_FS("plobs (pid, title):\n");
		rem_sl_dump(pl->plobs);
	}
	
	REM_DATA_DUMP_FTR;
}
