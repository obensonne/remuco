#include "rem-pstatus-priv.h"
#include "../basic/rem-bin.h"

static const guint rem_psi_t_bfv[] = {
	REM_BIN_DT_INT, 5,
	REM_BIN_DT_NONE
};

/**
 * Assigns the common values from 'dst' to 'src' and returns TRUE if 
 * one or more values have been different.
 */
gboolean
rem_player_status_priv_assign(RemPlayerStatusPriv *dst, RemPlayerStatus *src)
{
	gboolean	differ = FALSE;
	
	if (src->cap_pos != dst->cap_pos ||
		src->repeat != dst->repeat ||
		src->shuffle != dst->shuffle ||
		src->state != dst->state ||
		src->volume != dst->volume)
		
		differ = TRUE;
	
	dst->cap_pos = src->cap_pos;
	dst->repeat = src->repeat;
	dst->shuffle = src->shuffle;
	dst->state = src->state;
	dst->volume = src->volume;
	
	return differ;
}

GByteArray*
rem_player_status_priv_serialize(const RemPlayerStatusPriv *psi,
								 const gchar *se,
								 const RemStringList *pte)
{
	return rem_bin_serialize(psi, rem_psi_t_bfv, se, pte);
}

#ifdef TEST

RemPlayerStatusPriv*
rem_psi_unserialize(const GByteArray *ba, const gchar *te)
{
	RemPlayerStatusPriv	*psi = NULL;
	guint		ret;

	ret = rem_bin_unserialize(
			ba, sizeof(RemPlayerStatusPriv), rem_psi_t_bfv, (gpointer) &psi, te);

	if (ret < 0 && psi) {
		g_slice_free(RemPlayerStatusPriv, psi);
		psi = NULL;
	}
	
	return psi;
}

#endif /* TEST */
