#include "rem-pstatus.h"
#include "../../util/rem-util.h"
#include "../basic/rem-bin.h"

///////////////////////////////////////////////////////////////////////////////
//
// Player Status Normal
//
///////////////////////////////////////////////////////////////////////////////

RemPlayerStatus*
rem_player_status_new(void)
{
	RemPlayerStatus	*ps;
	
	ps = g_slice_new0(RemPlayerStatus);
	
	ps->cap_pid = g_string_sized_new(100);
	
	ps->playlist = rem_sl_new();
	
	ps->queue = rem_sl_new();
	
	return ps;
}

void
rem_player_status_destroy(RemPlayerStatus *ps)
{
	if (ps->cap_pid) g_string_free(ps->cap_pid, TRUE);
	
	if (ps->playlist) rem_sl_destroy(ps->playlist);
	
	if (ps->queue) rem_sl_destroy(ps->queue);
	
	g_slice_free(RemPlayerStatus, ps);
}

///////////////////////////////////////////////////////////////////////////////
//
// Player Status Basic
//
///////////////////////////////////////////////////////////////////////////////

static const guint REM_PLAYER_STATUS_BASIC_BFV[] = {
	REM_BIN_DT_INT, 5,
	REM_BIN_DT_NONE
};

GByteArray*
rem_player_status_basic_serialize(const RemPlayerStatusBasic *psb,
								 const gchar *charset_from,
								 const RemStringList *charsets_to)
{
	return rem_bin_serialize(
				psb, REM_PLAYER_STATUS_BASIC_BFV, charset_from, charsets_to);
}

#ifdef TEST

RemPlayerStatusBasic*
rem_psi_unserialize(const GByteArray *ba, const gchar *te)
{
	RemPlayerStatusBasic	*psi = NULL;
	guint		ret;

	ret = rem_bin_unserialize(ba, sizeof(RemPlayerStatusBasic),
				REM_PLAYER_STATUS_BASIC_BFV, (gpointer) &psi, te);

	if (ret < 0 && psi) {
		g_slice_free(RemPlayerStatusBasic, psi);
		psi = NULL;
	}
	
	return psi;
}

#endif /* TEST */

///////////////////////////////////////////////////////////////////////////////
//
// Player Status Fingerprint
//
///////////////////////////////////////////////////////////////////////////////

RemPlayerStatusFP*
rem_player_status_fp_new(void)
{
	RemPlayerStatusFP	*ps_fp;
	
	ps_fp = g_slice_new0(RemPlayerStatusFP);
	
	ps_fp->cap_pid = g_string_sized_new(100);
	
	return ps_fp;
}

void
rem_player_status_fp_destroy(RemPlayerStatusFP *ps_fp)
{
	if (ps_fp->cap_pid) g_string_free(ps_fp->cap_pid, TRUE);
	
	g_slice_free(RemPlayerStatusFP, ps_fp);
}

RemPlayerStatusDiff
rem_player_status_fp_update(RemPlayerStatus *ps, RemPlayerStatusFP *ps_fp)
{
	RemPlayerStatusDiff	result;
	guint				hash;
	
	result = 0;
	
	if ((ps_fp->priv.pbs != ps->pbs) ||
		(ps_fp->priv.volume != ps->volume) ||
		(ps_fp->priv.repeat != ps->repeat) ||
		(ps_fp->priv.shuffle != ps->shuffle) ||
		(ps_fp->priv.cap_pos != ps->cap_pos)) {
		
		ps_fp->priv.pbs = ps->pbs;
		ps_fp->priv.volume = ps->volume;
		ps_fp->priv.repeat = ps->repeat;
		ps_fp->priv.shuffle = ps->shuffle;
		ps_fp->priv.cap_pos = ps->cap_pos;
		result |= REM_PS_DIFF_SVRSP;
	}
	
	if (!g_string_equal(ps_fp->cap_pid, ps->cap_pid)) {
		g_string_assign(ps_fp->cap_pid, ps->cap_pid->str);
		result |= REM_PS_DIFF_PID;	
	}
	
	hash = rem_sl_hash(ps->playlist);
	LOG_NOISE("playlist hash new/old: %u/%u", hash, ps_fp->playlist_hash);
	if (hash != ps_fp->playlist_hash) {
		ps_fp->playlist_hash = hash;
		result |= REM_PS_DIFF_PLAYLIST;
	}
	
	hash = rem_sl_hash(ps->queue);
	LOG_NOISE("queue hash new/old: %u/%u", hash, ps_fp->queue_hash);
	if (hash != ps_fp->queue_hash) {
		ps_fp->queue_hash = hash;
		result |= REM_PS_DIFF_QUEUE;
	}

	return result;	
}

