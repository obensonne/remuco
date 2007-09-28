#ifndef PP_H_
#define PP_H_

///////////////////////////////////////////////////////////////////////////////
//
// functions to implement by a player proxy
//
///////////////////////////////////////////////////////////////////////////////

G_CONST_RETURN rem_ps_t*
pp_get_ps(rem_pp_t *pp);

gchar*
pp_get_current_plob_pid(rem_pp_t *pp);

rem_plob_t*
pp_get_plob(rem_pp_t *pp, const gchar *pid);

rem_sv_t*
pp_get_ploblist(rem_pp_t *pp, const gchar *plid);

rem_library_t*
pp_get_library(rem_pp_t *pp);

// FUTURE FEATURE no support on client side
rem_sv_t*
pp_search(rem_pp_t *pp, const rem_plob_t *plob);

void
pp_play_ploblist(rem_pp_t *pp, const gchar *plid);

void
pp_update_plob(rem_pp_t *pp, const rem_plob_t *plob);

// FUTURE FEATURE no support on client side
void
pp_update_ploblist(rem_pp_t *pp, const gchar *plid, const rem_sv_t* pids);

void
pp_ctrl(rem_pp_t *pp, const rem_sctrl_t *sc);

void
pp_error(rem_pp_t *pp, GError *err);



#endif /*PP_H_*/
