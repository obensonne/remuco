#ifndef REMPSTATUSPRIV_H_
#define REMPSTATUSPRIV_H_

#include <remuco.h>

#define REM_PS_STATE_ERROR	(REM_PS_STATE_COUNT + 0)
#define REM_PS_STATE_SRVOFF	(REM_PS_STATE_COUNT + 1)

/** Player status as transfered to client. */
typedef struct {
	gint32	state,
			volume,
			repeat,
			shuffle,
			cap_pos;
} RemPlayerStatusPriv;

gboolean
rem_player_status_priv_assign(RemPlayerStatusPriv *dst, RemPlayerStatus *src);

GByteArray*
rem_player_status_priv_serialize(const RemPlayerStatusPriv *psi,
								 const gchar *se,
								 const RemStringList *pte);

#ifdef TEST

static RemPlayerStatusPriv*
rem_player_status_priv_unserialize(const GByteArray *ba, const gchar *te);

#endif /* TEST */


#endif /*REMPSTATUSPRIV_H_*/
