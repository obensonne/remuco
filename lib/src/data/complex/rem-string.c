#include "rem-string.h"
#include "../basic/rem-bin.h"

static const guint REM_STRING_BFV[] = {
	REM_BIN_DT_STR,	1,
	REM_BIN_DT_NONE
};

GByteArray*
rem_string_serialize(const RemString *rs,
					 const gchar *charset_to,
					 const RemStringList *charsets_to)
{
	return rem_bin_serialize(rs, REM_STRING_BFV, charset_to, charsets_to);
}

RemString*
rem_string_unserialize(const GByteArray *ba, const gchar *charset_to)
{
	RemString *rs;
	guint ret;
	
	rs = NULL;
	ret = rem_bin_unserialize(ba, sizeof(RemString), REM_STRING_BFV,
				(gpointer) &rs, charset_to);

	if (ret < 0 && rs) {
		rem_string_destroy(rs);
		rs = NULL;
	}
	
	return rs;
}
