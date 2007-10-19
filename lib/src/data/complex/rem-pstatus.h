#ifndef REMPS_H_
#define REMPS_H_

#include <remuco.h>

///////////////////////////////////////////////////////////////////////////////
//
// Player Status Basic
//
///////////////////////////////////////////////////////////////////////////////

#define REM_PS_STATE_ERROR	(REM_PBS_COUNT + 0)
#define REM_PS_STATE_SRVOFF	(REM_PBS_COUNT + 1)

/** Player status as transfered to client. */
typedef struct {
	gint32	state,
			volume,
			repeat,
			shuffle,
			cap_pos;
} RemPlayerStatusBasic;

GByteArray*
rem_player_status_basic_serialize(const RemPlayerStatusBasic *psb,
								 const gchar *charset_from,
								 const RemStringList *charsets_to);

#ifdef TEST

static RemPlayerStatusBasic*
rem_player_status_basic_unserialize(const GByteArray *ba,
								   const gchar *charset_to);

#endif /* TEST */

///////////////////////////////////////////////////////////////////////////////
//
// Player Status Fingerprint
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Flags describing differences between two RemPlayerStatus.
 * 
 * @see rem_player_status_compare()
 */
typedef enum {
	/** No difference. */
	REM_PS_DIFF_NONE = 0,
	/** @a state, @a volume, @a repeat, @a shuffle and/or @a cap_pos differs. */
	REM_PS_DIFF_SVRSP = 1 << 0,
	/** @a cap_pid differs. */
	REM_PS_DIFF_PID = 1 << 2,
	/** @a playlist differs. */
	REM_PS_DIFF_PLAYLIST = 1 << 3,
	/** @a queue differs. */
	REM_PS_DIFF_QUEUE = 1 << 4,
	/** All differ. */
	REM_PS_DIFF_ALL = 0xFFFF
} RemPlayerStatusDiff;

/** Fingerprint */
typedef struct {
	RemPlayerStatusBasic	priv;
	GString				*cap_pid;
	guint				playlist_hash;
	guint				queue_hash;
	
} RemPlayerStatusFP;

RemPlayerStatusFP*
rem_player_status_fp_new(void);

void
rem_player_status_fp_destroy(RemPlayerStatusFP *ps_fp);

RemPlayerStatusDiff
rem_player_status_fp_update(RemPlayerStatus *ps, RemPlayerStatusFP *ps_fp);

#endif /*REMPS_H_*/
