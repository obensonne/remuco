#ifndef REMSCTRL_H_
#define REMSCTRL_H_

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

#define REM_SCTRL_CMD_PLAYPAUSE	0
#define REM_SCTRL_CMD_STOP	1
#define REM_SCTRL_CMD_RESTART	2
#define REM_SCTRL_CMD_NEXT	3
#define REM_SCTRL_CMD_PREV	4
#define REM_SCTRL_CMD_JUMP	5 // jump to a plob in:
				  // - the playlist if param is > 0
				  // - the queue if param is < 0
				  // Note: first position is 1 !
#define REM_SCTRL_CMD_VOLUME	6
#define REM_SCTRL_CMD_RATE	7
#define REM_SCTRL_CMD_VOTE	8 // FUTURE FEATURE
#define REM_SCTRL_CMD_SEEK	9 // FUTURE FEATURE
#define REM_SCTRL_CMD_COUNT	10

typedef struct {
	gint32	code;
	gint32	param;
} rem_sctrl_t;

///////////////////////////////////////////////////////////////////////////////
//
// working with sctrl
//
///////////////////////////////////////////////////////////////////////////////

#define rem_sctrl_new() g_malloc0(sizeof(rem_sctrl_t))

#define rem_sctrl_destroy(_sctrl) if (_sctrl) g_free(_sctrl)

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

#ifdef REM_NEED_SERIALIZATION_FUNCTIONS

GByteArray*
rem_sctrl_serialize(const rem_sctrl_t *sctrl,
		    const gchar *se,
		    const rem_sv_t *pte);

rem_sctrl_t*
rem_sctrl_unserialize(const GByteArray *ba, const gchar *te);

#endif

#endif /*REMSCTRL_H_*/
