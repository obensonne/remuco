#include "rem-ploblist.h"
#include "../basic/rem-bin.h"

rem_ploblist_t*
rem_ploblist_new(gchar *plid, gchar *title)
{
	rem_ploblist_t *pl;
	
	pl = g_malloc0(sizeof(rem_ploblist_t));
	
	pl->plid = plid;
	pl->name = title;
	pl->plobs = rem_sv_new();
	
	return pl;
}

void
rem_ploblist_destroy(rem_ploblist_t *pl)
{
	if (!pl) return;
	
	if (pl->plid) g_free(pl->plid);
	if (pl->name) g_free(pl->name);
	rem_sv_destroy(pl->plobs);
	
	g_free(pl);
}

/**
 * Clears the ploblist. Removes all plobs and all sub ploblists, but keeps plid
 * and name!
 */
void
rem_ploblist_clear(rem_ploblist_t *pl)
{
	g_assert(pl);
	
	rem_sv_clear(pl->plobs);
}

void
rem_ploblist_append(rem_ploblist_t *pl, gchar* pid, gchar* title)
{
	g_assert(pl && pid && title);
	
	rem_sv_append(pl->plobs, pid);
	rem_sv_append(pl->plobs, title);
}

void
rem_ploblist_append_intpid(rem_ploblist_t *pl, gint pid, gchar* title)
{
	g_assert(pl && title);

	gchar *pid_s;
	
	pid_s = g_strdup_printf("%i", pid);
	
	rem_sv_append(pl->plobs, pid_s);
	rem_sv_append(pl->plobs, title);
}

gint
rem_ploblist_get_pid_int(const rem_ploblist_t *pl, guint n)
{
	gint i, ret;
	
	g_assert(pl && n < pl->plobs->l * 2);
	
	ret = sscanf(pl->plobs->v[n * 2], "%i", &i);
	
	if (ret != 1) {
		LOG_WARN("tried to read a pid as int which is no int\n");
		i = -1;
	} 
	
	return i;
}

rem_sv_t*
rem_ploblist_get_pids(const rem_ploblist_t *pl)
{
	rem_sv_t	*pids;
	guint		u, len;
	
	pids = rem_sv_new();
	
	len = pl->plobs->l / 2;
	
	for (u = 0; u < len; u+=2) {
		rem_sv_append(pids, g_strdup(pl->plobs->v[u]));	
	}
	
	return pids;
}

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

static const guint rem_ploblist_t_bfv[] = {
	REM_BIN_DT_STR, 2,		// plid and name
	REM_BIN_DT_SV, 1,		// pids and titles of the plobs
	REM_BIN_DT_NONE
};

GByteArray*
rem_ploblist_serialize(const rem_ploblist_t *pl,
		       const gchar *se,
		       const rem_sv_t *pte)
{
	return rem_bin_serialize(pl, rem_ploblist_t_bfv, se, pte);
}

rem_ploblist_t*
rem_ploblist_unserialize(const GByteArray *ba, const gchar *te)
{
	rem_ploblist_t *pl = NULL;
	gint ret;
	
	ret = rem_bin_unserialize(ba, sizeof(rem_ploblist_t),
				rem_ploblist_t_bfv, (gpointer) &pl, te);
		
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
rem_ploblist_dump(const rem_ploblist_t *pl)
{
	g_assert_debug(pl && pl->plid && pl->name);
	g_assert_debug(pl->plobs->l % 2 == 0);

	guint u, l;
	const gchar *s1, *s2;
	
	DUMP_HDR("rem_ploblist_t", pl);
	LOG("plid: %s, name: %s\n", pl->plid, pl->name);
	
	LOG("plobs:\n");
	l = rem_ploblist_len(pl);
	for (u = 0; u < l; u++) {
		s1 = rem_ploblist_get_pid(pl, u);
		s2 = rem_ploblist_get_title(pl, u);
		LOG("%s, %s\n", s1, s2);
	}
	DUMP_FTR;

}
