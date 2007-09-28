#ifndef REMPS_H
#define REMPS_H

///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include "../../util/rem-common.h"
#include "../basic/rem-sv.h"

///////////////////////////////////////////////////////////////////////////////
//
// types
//
///////////////////////////////////////////////////////////////////////////////

typedef struct {
	gint32	state,
		volume,
		pl_pos,	// < 0 : position within queue
			// = 0 : current plob is neither in queue nor in playlist
			// > 0 : position within playlist
			// Note: Obvisously, the first position is 1 (or -1)
		repeat_mode,
		shuffle_mode;
} rem_ps_t;

///////////////////////////////////////////////////////////////////////////////
//
// constants
//
///////////////////////////////////////////////////////////////////////////////

#define REM_PS_STATE_STOP 0
#define REM_PS_STATE_PLAY 1
#define REM_PS_STATE_PAUSE 2
#define REM_PS_STATE_PROBLEM 3
#define REM_PS_STATE_OFF 4
#define REM_PS_STATE_ERROR 5
#define REM_PS_STATE_SRVOFF 6
#define REM_PS_STATE_COUNT 7

#define REM_PS_SHUFFLE_MODE_OFF 0
#define REM_PS_SHUFFLE_MODE_ON 1

#define REM_PS_REPEAT_MODE_NONE 0
#define REM_PS_REPEAT_MODE_PLOB 1
#define REM_PS_REPEAT_MODE_ALBUM 2
#define REM_PS_REPEAT_MODE_PL 3

///////////////////////////////////////////////////////////////////////////////
//
// working with mai
//
///////////////////////////////////////////////////////////////////////////////

#define rem_ps_new() g_malloc0(sizeof(rem_ps_t))

#define rem_ps_destroy(_ps) if (_ps) g_free(_ps)

#define rem_ps_hash(_ps) ((_ps)->state + \
			 ((_ps)->volume * 0x100) + \
			 ((_ps)->pl_pos * 0x10000) + \
			 ((_ps)->repeat_mode * 0x100000) + \
			 ((_ps)->shuffle_mode * 0x1000000))
		
#define rem_ps_equal(_ps1, _ps2) (rem_ps_hash(_ps1) == rem_ps_hash(_ps2))

#define rem_ps_copy(_ps_src, _ps_dest) do {			\
	(_ps_dest)->state = (_ps_src)->state;			\
	(_ps_dest)->volume = (_ps_src)->volume;			\
	(_ps_dest)->pl_pos = (_ps_src)->pl_pos;			\
	(_ps_dest)->repeat_mode = (_ps_src)->repeat_mode;	\
	(_ps_dest)->shuffle_mode = (_ps_src)->shuffle_mode;	\
} while(0)

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

#ifdef REM_NEED_SERIALIZATION_FUNCTIONS

GByteArray*
rem_ps_serialize(const rem_ps_t *ps, const gchar *se, const rem_sv_t *pte);

rem_ps_t*
rem_ps_unserialize(const GByteArray *ba, const gchar *te);

#endif

///////////////////////////////////////////////////////////////////////////////
//
// debug
//
///////////////////////////////////////////////////////////////////////////////

void
rem_ps_dump(const rem_ps_t *ps);

#endif /*REMPS_H*/
