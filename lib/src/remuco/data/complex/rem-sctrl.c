#include "rem-sctrl.h"
#include "../basic/rem-bin.h"

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

static const guint REM_SIMPLE_CONTROL_BFV[] = {
	REM_BIN_DT_INT,	2,
	REM_BIN_DT_NONE
};

GByteArray*
rem_simple_control_serialize(const RemSimpleControl *sctrl,
							 const gchar *charset_from,
							 const RemStringList *charsets_to)
{
	return rem_bin_serialize(
				sctrl, REM_SIMPLE_CONTROL_BFV, charset_from, charsets_to);
}

RemSimpleControl*
rem_simple_control_unserialize(const GByteArray *ba, const gchar *charset_to)
{
	RemSimpleControl *sctrl;
	guint ret;
	
	sctrl = NULL;
	ret = rem_bin_unserialize(ba, sizeof(RemSimpleControl),
				REM_SIMPLE_CONTROL_BFV,	(gpointer) &sctrl, charset_to);

	if (ret < 0 && sctrl) {
		rem_simple_control_destroy(sctrl);
		sctrl = NULL;
	}
	
	return sctrl;
}
