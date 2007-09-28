#ifndef REMDATABIN_H_
#define REMDATABIN_H_

#include "../../util/rem-common.h"
#include "rem-sv.h"

typedef struct {
	guint	*bfv;
	guint	*ds_size;
} rem_bin_bfs_t;

enum rem_bin_dt_enum {
	REM_BIN_DT_NONE,
	REM_BIN_DT_INT,
	REM_BIN_DT_STR,	// will be serialized unconverted
	REM_BIN_DT_BA,
	REM_BIN_DT_IV,
	REM_BIN_DT_SV,
	REM_BIN_DT_IGNORE,
	REM_BIN_DT_COUNT
};

GByteArray*
rem_bin_serialize(gconstpointer ds,
		  const guint *bfv,
		  const gchar *se,
		  const rem_sv_t *pte);


gint
rem_bin_unserialize(const GByteArray *ba,
		    guint tds_size,
		    const guint *bfv,
		    gpointer *tds,
		    const gchar *te);

#endif /*REMDATABIN_H_*/
