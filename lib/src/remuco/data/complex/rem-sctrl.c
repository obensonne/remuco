#include "rem-sctrl.h"
#include "../basic/rem-bin.h"

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

static const guint rem_sctrl_t_bfv[] = {
	REM_BIN_DT_INT,	2,
	REM_BIN_DT_NONE
};

GByteArray*
rem_sctrl_serialize(const rem_sctrl_t *sctrl,
		    const gchar *se,
		    const rem_sv_t *pte)
{
	return rem_bin_serialize(sctrl, rem_sctrl_t_bfv, se, pte);
}

rem_sctrl_t*
rem_sctrl_unserialize(const GByteArray *ba, const gchar *te)
{
	rem_sctrl_t *sctrl;
	guint ret;
	
	sctrl = NULL;
	ret = rem_bin_unserialize(ba, sizeof(rem_sctrl_t), rem_sctrl_t_bfv,
							(gpointer) &sctrl, te);

	if (ret < 0 && sctrl) {
		rem_sctrl_destroy(sctrl);
		sctrl = NULL;
	}
	
	return sctrl;
}
