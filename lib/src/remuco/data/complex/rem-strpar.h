#ifndef REMSTRPAR_H_
#define REMSTRPAR_H_

///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include "../../util/rem-common.h"

#include "../basic/rem-bin.h"

///////////////////////////////////////////////////////////////////////////////
//
// types
//
///////////////////////////////////////////////////////////////////////////////

typedef struct {
	gchar	*str;
} rem_strpar_t;

///////////////////////////////////////////////////////////////////////////////
//
// working with sctrl
//
///////////////////////////////////////////////////////////////////////////////

#define rem_strpar_new() g_malloc0(sizeof(rem_strpar_t))

#define rem_strpar_destroy(_sp) if (_sp) {	\
	 if ((_sp)->str)			\
	 	g_free((_sp)->str);		\
	 g_free(_sp);				\
}

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

#ifdef REM_NEED_SERIALIZATION_FUNCTIONS

static const guint rem_strpar_t_bfv[] = {
	REM_BIN_DT_STR,	1,
	REM_BIN_DT_NONE
};

#define rem_strpar_serialize(_sp, _se, _pte) \
		rem_bin_serialize(_sp, rem_strpar_t_bfv, _se, _pte)

static inline rem_strpar_t*
rem_strpar_unserialize(const GByteArray *ba, const gchar *te)
{
	rem_strpar_t *sp;
	guint ret;
	
	sp = NULL;
	ret = rem_bin_unserialize(ba, sizeof(rem_strpar_t), rem_strpar_t_bfv,
							(gpointer) &sp, te);

	if (ret < 0 && sp) {
		rem_sctrl_destroy(sp);
		sp = NULL;
	}
	
	return sp;
}

#endif

#endif /*REMSTRPAR_H_*/
