#include <remuco.h>
#include <sys/stat.h>

static gboolean initialized = FALSE;
static GIOChannel *channel_global; 

static void
gcb_log_handler(const gchar *domain,
				GLogLevelFlags level,
				const gchar *message,
				gpointer data)
{
	GIOChannel *channel = (GIOChannel*) data;
	gsize	written;
	
	g_io_channel_write_chars(channel, message, -1, &written, NULL);
	g_io_channel_flush(channel, NULL);
	
	// errors go also to std error:
	if (level & (G_LOG_FATAL_MASK | G_LOG_LEVEL_CRITICAL)) {
		g_printerr("%s %s", (domain ? domain : ""), message);
	}
}

static void
priv_glog_handler_devnull(const gchar *domain,
						  GLogLevelFlags level,
						  const gchar *message,
						  gpointer data)
{
	return;
}

/**
 * Get an GIOChannel to write logging messages into.
 * 
 * @return the channel or @p NULL if an error occured
 */ 
static GIOChannel*
priv_get_log_channel()
{
	GIOChannel	*channel = NULL;
	GError 		*err;
	gchar 		*log_file, *log_dir;
	const gchar	*env;
	
	env = g_getenv("XDG_CONFIG_HOME");
	
	////////// detect the dir of the log file //////////
	
	if (env) {
		log_dir = g_strdup_printf("%s/remuco", env);
	} else {
		env = g_getenv("HOME");
		if (!env) {
			g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_CRITICAL,
					"environent variable HOME is not set");
			return NULL;
		}
		log_dir = g_strdup_printf("%s/.config/remuco", env);
	}
	
	////////// create the dir (if needed) //////////
	
	if (g_mkdir_with_parents(log_dir, S_IRWXU) < 0) {
		g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_CRITICAL,
				"could not create log directory (%s)", strerror(errno));
		g_free(log_dir);
		return NULL;
	}
	
	////////// build the log file name //////////
	
	log_file = g_strdup_printf("%s/log", log_dir);
	
	g_free(log_dir);
	
	////////// open an IO channel //////////
	
	err = NULL;
	channel = g_io_channel_new_file(log_file, "w", &err);
	
	if (err) {
		g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_CRITICAL,
				"failed to open log file '%s' (%s)", log_file, err->message);
		g_error_free(err);
	} else {
		g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_INFO,
				"logging goes to %s\n", log_file);
	}
	
	g_free(log_file);
	
	channel_global = channel;
	
	return channel;
}

void
rem_log_init(RemLogLevel level)
{
	gchar 		*timestamp_str, *log_header;
	GIOChannel	*sink;
	GTimeVal	timestamp;
	gsize		written;
	
	if (initialized) return;
		
	initialized = TRUE;
	
	sink = priv_get_log_channel();
	
	if (!sink) return;
	
	////////// print a log header //////////
	
	g_get_current_time(&timestamp);
	timestamp_str = g_time_val_to_iso8601(&timestamp);
	log_header = g_strdup_printf("--- Remuco Log (%s) ---\n", timestamp_str);
	g_io_channel_write_chars(sink, log_header, -1, &written, NULL);
	g_io_channel_flush(sink, NULL);
	
	g_free(timestamp_str);
	g_free(log_header);
	
	////////// set up the handler //////////
	
	level = CLAMP(level, REM_LL_ERROR, REM_LL_NOISE);
	
	// by default, suppress everything but errors:
	g_log_set_handler(REM_LOG_DOMAIN,
					  G_LOG_LEVEL_CRITICAL | G_LOG_LEVEL_DEBUG |
					  G_LOG_LEVEL_INFO | G_LOG_LEVEL_MESSAGE |
					  G_LOG_LEVEL_WARNING | G_LOG_LEVEL_NOISE,
					  priv_glog_handler_devnull, NULL);
	
	// now enable log output as set by 'level'
	g_log_set_handler(REM_LOG_DOMAIN,
					  level,
					  gcb_log_handler, sink);
	
}
