///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include <remuco.h>
#include "rem-pinfo.h"
#include "../basic/rem-bin.h"

///////////////////////////////////////////////////////////////////////////////
//
// working with mai
//
///////////////////////////////////////////////////////////////////////////////

RemPlayerInfo*
rem_player_info_new(const gchar* name)
{
	RemPlayerInfo *pinfo;
	
	pinfo = g_slice_new0(RemPlayerInfo);
	
	pinfo->name = g_strdup(name);
	
	return pinfo;
}

void
rem_player_info_destroy(RemPlayerInfo *pinfo)
{
	if (!pinfo) return;
	
	if (pinfo->name) g_free(pinfo->name);

	if (pinfo->icon) g_byte_array_free(pinfo->icon, TRUE);
	
	g_slice_free(RemPlayerInfo, pinfo);
}

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

static const guint REM_PLAYER_INFO_BFV[] = {
	REM_BIN_DT_STR, 1,
	REM_BIN_DT_INT, 2,
	REM_BIN_DT_BA, 1,
	REM_BIN_DT_NONE
};

GByteArray*
rem_player_info_serialize(const RemPlayerInfo *pinfo,
						  const gchar *se,
						  const RemStringList *pte)
{
	return rem_bin_serialize(pinfo, REM_PLAYER_INFO_BFV, se, pte);
}

RemPlayerInfo*
rem_player_info_unserialize(const GByteArray *ba, const gchar *te)
{
	RemPlayerInfo *pinfo;
	guint ret;

	pinfo = NULL;
	ret = rem_bin_unserialize(ba, sizeof(RemPlayerInfo),
				REM_PLAYER_INFO_BFV, (gpointer) &pinfo, te);

	if (ret < 0 && pinfo) {
		rem_player_info_destroy(pinfo);
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
rem_player_info_dump(const RemPlayerInfo *pi)
{
	
	g_assert_debug(pi->name);
	
	REM_DATA_DUMP_HDR("RemPlayerInfo", pi);

	if (pi) {
	REM_DATA_DUMP_FS("name = %s\nfeautres = %X, rating_max= %i\n",
		pi->name, pi->features, pi->rating_max);
		
	if (pi->icon)
		rem_dump_ba(pi->icon);
	else
		REM_DATA_DUMP_FS("no icon");
	}
	
	REM_DATA_DUMP_FTR;
}

