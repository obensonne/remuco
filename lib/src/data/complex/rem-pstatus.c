#include "rem-pstatus.h"
#include "../../util/rem-util.h"

RemPlayerStatus*
rem_player_status_new(void)
{
	RemPlayerStatus	*ps;
	
	ps = g_slice_new0(RemPlayerStatus);
	
	ps->cap_pid = g_string_sized_new(100);
	
	return ps;
}

void
rem_player_status_destroy(RemPlayerStatus *ps)
{
	if (ps->cap_pid) g_string_free(ps->cap_pid, TRUE);
	
	g_slice_free(RemPlayerStatus, ps);
}

void
rem_player_status_set_current_plob(RemPlayerStatus *ps, const gchar* pid)
{
	g_return_if_fail(ps);
	
	if (!pid) {
		g_string_truncate(ps->cap_pid, 0);
	} else {
		g_string_assign(ps->cap_pid, pid);
	}
}

const gchar*
rem_player_status_get_cap(RemPlayerStatus *ps)
{
	return ps->cap_pid->len ? ps->cap_pid->str : NULL;
}

void
rem_player_status_copy(RemPlayerStatus *src, RemPlayerStatus *dst)
{
	g_return_if_fail(src && dst);
	g_return_if_fail(src->cap_pid && dst->cap_pid);
	
	dst->state = src->state;
	dst->volume = src->volume;
	dst->repeat = src->repeat;
	dst->shuffle = src->shuffle;
	dst->cap_pos = src->cap_pos;
	g_string_assign(dst->cap_pid, src->cap_pid->str);
	
}

RemPlayerStatusCompareResult
rem_player_status_compare(RemPlayerStatus *one, RemPlayerStatus *two)
{
	RemPlayerStatusCompareResult	result;
	
	result = 0;
	
	if ((one->state != two->state) ||
		(one->volume != two->volume) ||
		(one->repeat != two->repeat) ||
		(one->shuffle != two->shuffle) ||
		(one->cap_pos != two->cap_pos))
		result |= REM_PS_DIFF_SVRSP;
	
	if (!g_string_equal(one->cap_pid, two->cap_pid))
		result |= REM_PS_DIFF_PID;
	
	return result;
}	



