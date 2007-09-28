///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include "rem-pinfo.h"
#include "../basic/rem-bin.h"

///////////////////////////////////////////////////////////////////////////////
//
// working with mai
//
///////////////////////////////////////////////////////////////////////////////

rem_pinfo_t*
rem_pinfo_new()
{
	rem_pinfo_t *pinfo;
	
	pinfo = g_malloc0(sizeof(rem_pinfo_t));
	
	return pinfo;
}

void
rem_pinfo_destroy(rem_pinfo_t *pinfo)
{
	if (!pinfo) return;
	
	if (pinfo->name) g_free(pinfo->name);

	if (pinfo->icon) g_byte_array_free(pinfo->icon, TRUE);
	
	g_free(pinfo);
}

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

static const guint rem_pinfo_t_bfv[] = {
	REM_BIN_DT_STR, 1,
	REM_BIN_DT_INT, 3,
	REM_BIN_DT_BA, 1,
	REM_BIN_DT_NONE
};

GByteArray*
rem_pinfo_serialize(const rem_pinfo_t *pinfo,
		    const gchar *se,
		    const rem_sv_t *pte)
{
	return rem_bin_serialize(pinfo, rem_pinfo_t_bfv, se, pte);
}

rem_pinfo_t*
rem_pinfo_unserialize(const GByteArray *ba, const gchar *te)
{
	rem_pinfo_t *pinfo;
	guint ret;

	pinfo = NULL;
	ret = rem_bin_unserialize(ba, sizeof(rem_pinfo_t),
				rem_pinfo_t_bfv, (gpointer) &pinfo, te);

	if (ret < 0 && pinfo) {
		rem_pinfo_destroy(pinfo);
		pinfo = NULL;
	}
	
	return pinfo;
}

///////////////////////////////////////////////////////////////////////////////
//
// debug
//
///////////////////////////////////////////////////////////////////////////////

void
rem_pinfo_dump(const rem_pinfo_t *pi)
{
	
	g_assert_debug(pi->name);
	
	DUMP_HDR("rem_pinfo_t", pi);

	LOG("name = %s\nfeautres = %X, rating_none = %i, rating_max= %i\n",
		pi->name, pi->features, pi->rating_none, pi->rating_max);
		
	if (pi->icon)
		dump_gba(pi->icon);
	else
		LOG("no icon\n");
	
	DUMP_FTR;
}

