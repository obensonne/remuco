#ifndef REMUCO_LOG_H_
#define REMUCO_LOG_H_

#include <glib.h>
#include <errno.h>
#include <string.h> // strerror()

#ifndef G_LOG_LEVEL_NOISE
#define G_LOG_LEVEL_NOISE (1 << G_LOG_LEVEL_USER_SHIFT)
#endif

/**
 * @defgroup dx_log Remuco Logging System
 *  
 */

/*@{*/

G_BEGIN_DECLS

typedef enum {
	REM_LL_ERROR = G_LOG_FLAG_RECURSION | G_LOG_FLAG_FATAL |
				   G_LOG_LEVEL_ERROR | G_LOG_LEVEL_CRITICAL,
	REM_LL_WARN = REM_LL_ERROR | G_LOG_LEVEL_WARNING,
	REM_LL_INFO = REM_LL_WARN | G_LOG_LEVEL_INFO,
	REM_LL_DEBUG = REM_LL_INFO | G_LOG_LEVEL_DEBUG,
	REM_LL_NOISE = REM_LL_DEBUG | G_LOG_LEVEL_NOISE,
} RemLogLevel;

/**
 * Initialize the Remuco logging system. Automatically gets called by
 * rem_server_up(), but if you want to use the Remuco logging macros before
 * you call rem_server_up(), call this at first.
 * 
 * @param level specifies which messages to log
 */
void
rem_log_init(RemLogLevel level);

G_END_DECLS

/**
 * The Remuco logging system uses the GLib logging system. This is the log
 * domain used for all Remuco loggings.
 */
#define REM_LOG_DOMAIN	"Remuco"

#ifdef DO_LOG_NOISE
/**
 * Log a noisy debug message.
 * 
 * @remark	This log macro is only enabled if the server/player proxy has been
 * 			compiled with @p -DLOG_NOISE. For performance and log readability
 * 			reasons it is disabled by default.
 * 
 * @see RemLogLevel::REM_LL_NOISE
 */ 
#define LOG_NOISE(x, args...) g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_NOISE, \
	"[NOISE] %-25s: " x, __FUNCTION__, ##args)
#else
/**
 * Log a noisy debug message.
 * 
 * @remark	This log macro is only enabled if the server/player proxy has been
 * 			compiled with @p -DLOG_NOISE. For performance and log readability
 * 			reasons it is disabled by default.
 * 
 * @see RemLogLevel::REM_LL_NOISE
 */ 
#define LOG_NOISE(x, args...)
#endif

/**
 * Log a debug message.
 * 
 * @see RemLogLevel::REM_LL_DEBUG
 */ 
#define LOG_DEBUG(x, args...) g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_DEBUG, \
	"[DEBUG] %-25s: " x, __FUNCTION__, ##args)

/**
 * Log an informative message.
 * 
 * @see RemLogLevel::REM_LL_INFO
 */ 
#define LOG_INFO(x, args...) g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_INFO, \
	"[INFO ] %-25s: " x, __FUNCTION__, ##args)

/**
 * Log a warning message.
 * 
 * @see RemLogLevel::REM_LL_WARN
 */ 
#define LOG_WARN(x, args...) g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_WARNING, \
	"[WARN ] %-25s: " x, __FUNCTION__, ##args)

/**
 * Log an error message. Use this for expected errors. For unexpected errors
 * use LOG_BUG().
 * 
 * @see RemLogLevel::REM_LL_ERROR
 */ 
#define LOG_ERROR(x, args...) g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_CRITICAL, \
	"[ERROR] %-25s: " x, __FUNCTION__, ##args)

/**
 * Log an error message. Output includes an error message derived from the
 * current value or @p errno. Use this for expected errors. For unexpected
 * errors use LOG_BUG().
 * 
 * @see RemLogLevel::REM_LL_ERROR
 */ 
#define LOG_ERRNO(x, args...) g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_CRITICAL, \
	"[ERROR] %-25s: " x " (%s)", __FUNCTION__, ##args, strerror(errno))

#define LOG(x, args...)
//g_print(x, ##args)

/**
 * Logs a bug message and aborts the program.
 * Output includes the file name, function name and line number where this
 * macro gets called. Use this for bugs as you would use @p g_assert() and
 * friends.
 * Param is a format string like in @p printf().
 */
#define LOG_BUG(x, args...) g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_ERROR, \
	"* BUG * in %s, func. %s, line %d: " x, \
	__FILE__, __FUNCTION__, __LINE__, ##args);

/*@}*/

#endif /*REMUCO_LOG_H_*/
