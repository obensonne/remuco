#ifndef REMPL_H_
#define REMPL_H_

#include <remuco.h>

GByteArray*
rem_sl_serialize(const RemStringList *sl, const gchar *ef, const RemStringList *pte);

RemStringList*
rem_sl_unserialize(const GByteArray *ba, const gchar *et);

void
rem_sl_destroy_keep_strings(RemStringList *sl);

#endif /*REMPL_H_*/
