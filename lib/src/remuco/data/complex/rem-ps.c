#include "rem-ps.h"
#include "../basic/rem-bin.h"

static const guint rem_ps_t_bfv[] = {
	REM_BIN_DT_INT, 5,
	REM_BIN_DT_NONE
};

GByteArray*
rem_ps_serialize(const rem_ps_t *ps, const gchar *se, const rem_sv_t *pte)
{
	return rem_bin_serialize(ps, rem_ps_t_bfv, se, pte);
}

rem_ps_t*
rem_ps_unserialize(const GByteArray *ba, const gchar *te)
{
	rem_ps_t *ps = NULL;
	guint ret;

	ret = rem_bin_unserialize(ba, sizeof(rem_ps_t),
				rem_ps_t_bfv, (gpointer) &ps, te);

	if (ret < 0 && ps) {
		rem_ps_destroy(ps);
		ps = NULL;
	}
	
	return ps;
}

void
rem_ps_dump(const rem_ps_t *ps)
{
	DUMP_HDR("rem_ps_t", ps);
	LOG_DEBUG("%i, %i, %i, %i, %i\n", ps->state, ps->volume, ps->pl_pos,
					ps->repeat_mode, ps->shuffle_mode);
	DUMP_FTR;
}

