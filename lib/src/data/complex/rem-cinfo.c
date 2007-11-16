///////////////////////////////////////////////////////////////////////////////
//
// Includes
//
///////////////////////////////////////////////////////////////////////////////

#include "rem-cinfo.h"
#include "../basic/rem-bin.h"

///////////////////////////////////////////////////////////////////////////////
//
// public functions
//
///////////////////////////////////////////////////////////////////////////////

RemClientInfo*
rem_client_info_new(void)
{
	RemClientInfo *ci;
	
	ci = g_slice_new0(RemClientInfo);
	
	return ci;
}

void
rem_client_info_destroy(RemClientInfo *ci)
{
	if (!ci) return;
	
	if (ci->charsets) rem_sl_destroy(ci->charsets);
	
	g_slice_free(RemClientInfo, ci);
}

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

static const guint REM_CLIENT_INFO_BFV[] = {
	REM_BIN_DT_INT,	3,
	REM_BIN_DT_SV,	1,
	REM_BIN_DT_NONE
};

GByteArray*
rem_client_info_serialize(const RemClientInfo *ci,
						  const gchar *se,
						  const RemStringList *pte)
{
	return rem_bin_serialize(ci, REM_CLIENT_INFO_BFV, se, pte);
}

RemClientInfo*
rem_client_info_unserialize(const GByteArray *ba, const gchar *te)
{
	RemClientInfo *cinfo = NULL;
	gint ret;
	
	ret = rem_bin_unserialize(ba, sizeof(RemClientInfo),
				REM_CLIENT_INFO_BFV, (gpointer) &cinfo, te);
		
	if (ret < 0 && cinfo) {
		rem_client_info_destroy(cinfo);
		cinfo = NULL;
	}
	
	return cinfo;
}

///////////////////////////////////////////////////////////////////////////////
//
// debug
//
///////////////////////////////////////////////////////////////////////////////

void
rem_client_info_dump(const RemClientInfo *ci)
{
	REM_DATA_DUMP_HDR("rem_cinfo_t", ci);
	
	LOG("client info at %p:\n", ci);
	
	LOG("memlimit, img_width, img_height = %i, %i, %i\n",
		ci->mem_limit, ci->img_width, ci->img_height);
		
	rem_sl_dump(ci->charsets);
	
	REM_DATA_DUMP_FTR;
}

