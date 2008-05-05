#include "config.h"
#include "util.h"

static GKeyFile*
load(const gchar *name, const gchar* type, gboolean required)
{
	gchar		*file;
	gboolean 	ok;
	GKeyFile	*kf;
	GError		*err;
	gchar		*name_type;
	
	name_type = g_strdup_printf("%s.%s", name, type);
	
	file = g_build_filename(g_get_user_config_dir(), "remuco", name_type, NULL);
	
	g_free(name_type);
	
	ok = g_file_test(file, G_FILE_TEST_IS_REGULAR);
	
	if (!ok) {
		if (required) {
			LOG_ERROR("config '%s' does not exist", file);
			kf = NULL;
		} else {
			LOG_INFO("config '%s' does not exist -> using defaults", file);
			kf = g_key_file_new();
		}
		g_free(file);
		return kf;
	}

	kf = g_key_file_new();

	err = NULL;
	ok = g_key_file_load_from_file(kf, file, G_KEY_FILE_NONE, &err);
	if (!ok) {
		LOG_ERROR_GERR(err, "failed to load '%s'", file);
		g_key_file_free(kf);
		kf = NULL;
	}
	
	g_free(file);

	return kf;
}

gboolean
rem_config_load(const gchar *name, const gchar *type, gboolean required,
				const RemConfigEntry *entries)
{
	guint		u;
	GKeyFile	*kf;
	GError		*err;
	gboolean	ok;
	gboolean	b;
	gint		i;
	gchar		*s;

	kf = load(name, type, required);
	if (!kf)
		return FALSE;
	
	for (u = 0, ok = TRUE; entries[u].type != G_TYPE_INVALID && ok; u++) {

		g_assert(entries[u].group && entries[u].key && entries[u].val);
		
		LOG_DEBUG("process config %s.%s", entries[u].group, entries[u].key);
		
		switch (entries[u].type) {
		
			case G_TYPE_BOOLEAN:
				
				err = NULL;
				b = g_key_file_get_boolean(kf, entries[u].group, entries[u].key,
										   &err);
				
				if (err) {
					
					if (err->code == G_KEY_FILE_ERROR_KEY_NOT_FOUND ||
						err->code == G_KEY_FILE_ERROR_GROUP_NOT_FOUND) {
						
						g_error_free(err);

						if (entries[u].required) {
							LOG_ERROR("missing required option %s in group %s",
									  entries[u].key, entries[u].group);
							ok = FALSE;
							
						} else {
							LOG_DEBUG("use default (%u)", *((gboolean*) entries[u].val));
						}
						
					} else {
						
						LOG_ERROR_GERR(err, "failed to parse config");
						ok = FALSE;
					}
					
				} else {
					
					LOG_DEBUG("set to (%u)", b);
					*((gboolean*) entries[u].val) = b;
				}
								
			break;
			case G_TYPE_INT:
				
				err = NULL;
				i = g_key_file_get_integer(kf, entries[u].group, entries[u].key,
										   &err);
				
				if (err) {
					
					if (err->code == G_KEY_FILE_ERROR_KEY_NOT_FOUND ||
						err->code == G_KEY_FILE_ERROR_GROUP_NOT_FOUND) {
						
						g_error_free(err);

						if (entries[u].required) {
							LOG_ERROR("missing required option %s in group %s",
									  entries[u].key, entries[u].group);
							ok = FALSE;
							
						} else {
							LOG_DEBUG("use default (%i)", *((gint*) entries[u].val));
						}
						
					} else {
						
						LOG_ERROR_GERR(err, "failed to parse config");
						ok = FALSE;
					}
					
				} else {
					
					LOG_DEBUG("set to (%i)", i);
					*((gint*) entries[u].val) = i;
				}
								
			break;
			case G_TYPE_STRING:
				
				err = NULL;
				s = g_key_file_get_string(kf, entries[u].group, entries[u].key,
										  &err);
				
				if (err) {
					
					if (err->code == G_KEY_FILE_ERROR_KEY_NOT_FOUND ||
						err->code == G_KEY_FILE_ERROR_GROUP_NOT_FOUND) {
						
						g_error_free(err);

						if (entries[u].required) {
							LOG_ERROR("missing required option %s in group %s",
									  entries[u].key, entries[u].group);
							ok = FALSE;
							
						} else {
							LOG_DEBUG("use default (%s)", *((gchar**) entries[u].val));
						}
						
					} else {
						
						LOG_ERROR_GERR(err, "failed to parse config");
						ok = FALSE;
					}
					
				} else {
					
					LOG_DEBUG("set to (%s)", s);
					*((gchar**) entries[u].val) = s;
				}
								
			break;
			default:
				g_assert_not_reached();
			break;
		}
	}
	
	g_key_file_free(kf);
	
	return ok;
}
