#ifndef REM_LOG_H_
#define REM_LOG_H_

#include <glib.h>
#include <errno.h>
#include <string.h> // for strerror()

/**
 * @defgroup dx_log Remuco Logging System
 */

/*@{*/

/**
 * The Remuco logging system uses the GLib logging system. This is the log
 * domain used for all Remuco loggings.
 */
#define REM_LOG_DOMAIN		"Remuco"

G_BEGIN_DECLS

typedef enum {
	REM_LL_ERROR = G_LOG_FLAG_RECURSION | G_LOG_FLAG_FATAL |
				   G_LOG_LEVEL_ERROR | G_LOG_LEVEL_CRITICAL,
	REM_LL_WARN = REM_LL_ERROR | G_LOG_LEVEL_WARNING,
	REM_LL_INFO = REM_LL_WARN | G_LOG_LEVEL_INFO,
	REM_LL_DEBUG = REM_LL_INFO | G_LOG_LEVEL_DEBUG
} RemLogLevel;

/**
 * Initialize the Remuco logging system. Creates a log file in
 * @p $XDG_CACHE_HOME/remuco named @p &lt;name&gt;.log. See
 * http://standards.freedesktop.org/basedir-spec/basedir-spec-0.6.html
 * for a description of @p $XDG_CACHE_HOME.
 * 
 * @param level specifies which messages to log
 * @param name used for the log file name
 */
void
rem_log_init(const gchar *component);

G_END_DECLS

/**
 * Log a debug message.
 * 
 * @see RemLogLevel::REM_LL_DEBUG
 */ 
#define LOG_DEBUG(x, args...) g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_DEBUG, \
	"[DEBUG] %-18s %-30s: " x, G_STRLOC, G_STRFUNC, ##args)

/**
 * Log an informative message.
 * 
 * @see RemLogLevel::REM_LL_INFO
 */ 
#define LOG_INFO(x, args...) g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_INFO, \
	"[INFO ] %-18s %-30s: " x, G_STRLOC, G_STRFUNC, ##args)

/**
 * Log a warning message.
 * 
 * @see RemLogLevel::REM_LL_WARN
 */ 
#define LOG_WARN(x, args...) g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_WARNING, \
	"[WARN ] %-18s %-30s: " x, G_STRLOC, G_STRFUNC, ##args)

/**
 * Log a warning related to an GError. The first param is the GError which gets
 * freed and set to NULL by this macro.
 * 
 * @see RemLogLevel::REM_LL_ERROR
 */ 
#define LOG_WARN_GERR(_err, x, args...) G_STMT_START {						\
	g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_WARNING,								\
		"[WARN ] %-18s %-30s: " x " (%s)", G_STRLOC, G_STRFUNC, ##args,	\
		_err ? (_err)->message : "");										\
	g_error_free(_err);														\
	_err = NULL;															\
} G_STMT_END

/**
 * Log an error message. Use this for expected errors. For unexpected errors
 * use LOG_BUG().
 * 
 * @see RemLogLevel::REM_LL_ERROR
 */ 
#define LOG_ERROR(x, args...) g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_CRITICAL, \
	"[ERROR] %-18s %-30s: " x, G_STRLOC, G_STRFUNC, ##args)

/**
 * Log an error related to an GError. The first param is the GError which gets
 * freed and set to NULL by this macro. Use this for expected errors. For
 * unexpected errors use LOG_BUG().
 * 
 * @see RemLogLevel::REM_LL_ERROR
 */ 
#define LOG_ERROR_GERR(_err, x, args...) G_STMT_START {						\
	g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_CRITICAL,								\
		"[ERROR] %-18s %-30s: " x " (%s)", G_STRLOC, G_STRFUNC, ##args,		\
		_err ? (_err)->message : "");										\
	g_error_free(_err);														\
	_err = NULL;															\
} G_STMT_END

/**
 * Log an error message. Output includes an error message derived from the
 * current value or @p errno. Use this for expected errors. For unexpected
 * errors use LOG_BUG().
 * 
 * @see RemLogLevel::REM_LL_ERROR
 */ 
#define LOG_ERRNO(x, args...) g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_CRITICAL, \
	"[ERROR] %-18s %-30s: " x " (%s)", G_STRLOC, G_STRFUNC, ##args, strerror(errno))

/**
 * Logs a bug message and aborts the program.
 * Output includes the file name, function name and line number where this
 * macro gets called. Use this for bugs as you would use @p g_assert() and
 * friends.
 * Param is a format string like in @p printf().
 */
#define LOG_BUG(x, args...) g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_ERROR, \
	"* BUG * in %s (func. %s): " x, G_STRLOC, G_STRFUNC, ##args);

/*@}*/

#endif /*REM_LOG_H_*/
