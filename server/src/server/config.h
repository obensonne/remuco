#ifndef REM_CONFIG_H_
#define REM_CONFIG_H_

#include "common.h"
#include "net.h"

typedef struct {
	
	gchar				*group;
	gchar				*key;
	GType				type;
	gboolean			required;
	gpointer			val;
	
} RemConfigEntry;

gboolean
rem_config_load(const gchar *name, const gchar *type, gboolean required,
				const RemConfigEntry *entries);

#endif /*REM_CONFIG_H_*/
