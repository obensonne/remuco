///////////////////////////////////////////////////////////////////////////////
//
// Includes
//
///////////////////////////////////////////////////////////////////////////////

#include "rem-cinfo.h"
#include "../basic/rem-bin.h"

///////////////////////////////////////////////////////////////////////////////
//
// constants
//
///////////////////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////////////////////////
//
// working with dictionaries
//
///////////////////////////////////////////////////////////////////////////////

rem_cinfo_t*
rem_cinfo_new(void)
{
	rem_cinfo_t *ci;
	
	ci = g_malloc0(sizeof(rem_cinfo_t));
	
	return ci;
}

void
rem_cinfo_destroy(rem_cinfo_t *ci)
{
	g_assert_debug(ci);
	
	rem_sv_destroy(ci->encodings);
	
	g_free(ci);
}

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

static const guint rem_cinfo_t_bfv[] = {
	REM_BIN_DT_INT,	3,
	REM_BIN_DT_SV,	1,
	REM_BIN_DT_NONE
};

GByteArray*
rem_cinfo_serialize(	const rem_cinfo_t *ci,
			const gchar *se,
			const rem_sv_t *pte)
{
	return rem_bin_serialize(ci, rem_cinfo_t_bfv, se, pte);
}

rem_cinfo_t*
rem_cinfo_unserialize(const GByteArray *ba, const gchar *te)
{
	rem_cinfo_t *cinfo = NULL;
	gint ret;
	
	ret = rem_bin_unserialize(ba, sizeof(rem_cinfo_t),
				rem_cinfo_t_bfv, (gpointer) &cinfo, te);
		
	if (ret < 0 && cinfo) {
		rem_cinfo_destroy(cinfo);
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
rem_cinfo_dump(const rem_cinfo_t *ci)
{
	DUMP_HDR("rem_cinfo_t", ci);
	
	LOG("client info at %p:\n", ci);
	
	LOG("memlimit, img_width, img_height = %i, %i, %i\n",
		ci->mem_limit, ci->img_width, ci->img_height);
		
	rem_sv_dump(ci->encodings);
	
	DUMP_FTR;
}

