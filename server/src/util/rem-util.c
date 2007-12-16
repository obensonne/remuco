#include <remuco.h>
#include "rem-util.h"
#include <sys/stat.h> // for S_IRWXU

gboolean
rem_create_needed_dirs()
{
	gchar	*cache_dir;
	gint	ret;
	
	cache_dir = g_strdup_printf("%s/remuco", g_get_user_cache_dir());

	ret = g_mkdir_with_parents(cache_dir, S_IRWXU);
	
	if (ret < 0) {
		g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_CRITICAL,
				"could not create cache directory '%s' (%s)",
				cache_dir, strerror(errno));
		g_free(cache_dir);
		return FALSE;
	}
	
	g_free(cache_dir);
	return TRUE;
}	
