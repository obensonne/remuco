///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include <glib/gstdio.h>	// for stats()

#include "log.h"

///////////////////////////////////////////////////////////////////////////////
//
// constants
//
///////////////////////////////////////////////////////////////////////////////

#define REM_LOG_MAX_FILE_SIZE	1048576 // 1 MB

///////////////////////////////////////////////////////////////////////////////
//
// global vars
//
///////////////////////////////////////////////////////////////////////////////

static gboolean		initialized = FALSE;

///////////////////////////////////////////////////////////////////////////////
//
// private functions -log handler
//
///////////////////////////////////////////////////////////////////////////////

static GTimeVal tv_start, tv;
static GString	*ts = NULL;

#define REM_LOG_UPDATE_TS G_STMT_START {								\
	g_get_current_time(&tv);											\
	g_string_truncate(ts, 0);											\
	tv.tv_sec -= tv_start.tv_sec;										\
	tv.tv_usec /= 1000;													\
	g_string_append_printf(ts, "%5lu.%3lu ", tv.tv_sec, tv.tv_usec);	\
} G_STMT_END

static void
gcb_log_handler_file(const gchar *domain,
					 GLogLevelFlags level,
					 const gchar *message,
					 gpointer data)
{
	GIOChannel *channel = (GIOChannel*) data;
	gsize		written;
	
	REM_LOG_UPDATE_TS;
	
	g_io_channel_write_chars(channel, ts->str, -1, &written, NULL);
	
	g_io_channel_write_chars(channel, message, -1, &written, NULL);
	g_io_channel_write_chars(channel, "\n", -1, &written, NULL);
	g_io_channel_flush(channel, NULL);
	
	// errors also go to std error:
	if (level & (G_LOG_FATAL_MASK | G_LOG_LEVEL_CRITICAL)) {
		g_printerr("%s %s\n", (domain ? domain : ""), message);
	}
}

static void
gcb_log_handler_std(const gchar *domain,
					GLogLevelFlags level,
					const gchar *message,
					gpointer data)
{
	REM_LOG_UPDATE_TS;

	if (level & (G_LOG_FATAL_MASK | G_LOG_LEVEL_CRITICAL)) {
		g_printerr("%s%s %s\n", ts->str, (domain ? domain : ""), message);
	} else {
		g_print("%s%s %s\n", ts->str, (domain ? domain : ""), message);		
	}
}

static void
gcb_log_handler_devnull(const gchar *domain,
						  GLogLevelFlags level,
						  const gchar *message,
						  gpointer data)
{
	return;
}

///////////////////////////////////////////////////////////////////////////////
//
// private functions - util
//
///////////////////////////////////////////////////////////////////////////////

static gchar*
get_file(const gchar *name)
{
	gchar		*basename, *file;
	gboolean	ok;
	
	if (!name)
		return NULL;
	
	g_assert(name[0]);
	
	ok = TRUE;
	
	////////// get log file name //////////
	
	basename = g_strdup_printf("%s.log", name);
	
	file = g_build_filename(g_get_user_cache_dir(), "remuco", basename, NULL);
	
	g_free(basename);
	
	LOG_INFO("logging goes to %s", file);
	
	return file;
}

static RemLogLevel
get_level()
{
	gchar			*file;
	RemLogLevel		level;
	
	level = REM_LL_INFO;
	
	file = g_build_filename(g_get_user_config_dir(), "remuco", "debug", NULL);

	if (g_file_test(file, G_FILE_TEST_IS_REGULAR))
		level = REM_LL_DEBUG;

	g_free(file);
	
	return level;
}

///////////////////////////////////////////////////////////////////////////////
//
// public functions
//
///////////////////////////////////////////////////////////////////////////////

void
rem_log_init(const gchar *name)
{
	GIOChannel		*channel;
	GError 			*err;
	gchar	 		*ts_start, *log_header, *log_file, *mode;
	gsize			written;
	gint			ret;
	RemLogLevel		level;
	struct stat		stats;
	
	if (initialized) return;
	
	initialized = TRUE;
	
	g_get_current_time(&tv_start);
	ts = g_string_new("12345.789");
	
	// at first, suppress everything:
	g_log_set_handler(REM_LOG_DOMAIN, REM_LL_DEBUG, gcb_log_handler_devnull, NULL);
	
	// by default, log all with severity <= INFO to stdout 
	g_log_set_handler(REM_LOG_DOMAIN, REM_LL_INFO, gcb_log_handler_std, NULL);
	
	level = get_level();
	
	LOG_INFO("debug log is %s", level == REM_LL_DEBUG ? "enabled" : "disabled");
	
	log_file = get_file(name);
	
	channel = NULL;
	
	if (log_file) { // create io channel for log file
		
		////////// check log file size //////////
		
		mode = "w";
		ret = g_stat(log_file, &stats);
		if (ret == 0) {
			if (stats.st_size < REM_LOG_MAX_FILE_SIZE) {
				mode = "a";
			}
		}
		
		////////// create channel //////////
		
		err = NULL;
		channel = g_io_channel_new_file(log_file, mode, &err);
		
		g_free(log_file);

		if (!channel)
			LOG_ERROR_GERR(err, "failed to open log file");
	}
	
	if (channel) { // print a log header
			
		ts_start = g_time_val_to_iso8601(&tv_start);
		log_header = g_strdup_printf("--- Remuco %s Log (%s) ---\n", name, ts_start);
		g_free(ts_start);
		g_io_channel_write_chars(channel, log_header, -1, &written, NULL);
		g_free(log_header);
		g_io_channel_flush(channel, NULL);		
	}

	////////// final set up of the handler //////////
	
	// now enable log output as set by 'level'
	if (channel) {
		g_log_set_handler(REM_LOG_DOMAIN, level, gcb_log_handler_file, channel);
	} else {
		g_log_set_handler(REM_LOG_DOMAIN, level, gcb_log_handler_std, NULL);
	}
	
	////////// just to know .. //////////
	
	gboolean	fne_is_utf8;
	const gchar **filename_enc_list, *locale_enc;
	guint		u;
	
	locale_enc = NULL;
	g_get_charset(&locale_enc);
	LOG_DEBUG("locale encoding: %s", locale_enc);
	
	filename_enc_list = NULL;
	fne_is_utf8 = g_get_filename_charsets(&filename_enc_list);
	if (filename_enc_list) {
		LOG_DEBUG("filename encodings:");
		for (u = 0; filename_enc_list[u]; u++)
			LOG_DEBUG("%s", filename_enc_list[u]);
	} else {
		LOG_WARN("no filename encodings specifed");		
	}
	if (!fne_is_utf8) {
		LOG_WARN("it looks like your filename encoding is not UTF-8 - "
				 "this may cause problems.");
	}
	
}

