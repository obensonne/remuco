#include <signal.h>	// for sigaction() etc.

#include "daemon.h"
#include "common.h"
#include "server.h"
#include "shell.h"
#include "server-glue-s.h"
#include "shell-glue-s.h"
#include "dbus.h"
#include "util.h"

///////////////////////////////////////////////////////////////////////////////
//
// command line options
//
///////////////////////////////////////////////////////////////////////////////

#define OPTION_DESC_FORCE \
	"force manual start"

#define OPTION_DESC_LOG_HERE \
	"log to stdout/err"

#define HELP_SUMMARY \
	"The server daemon is intended to be started automatically by player " \
	"proxies or by the tool 'remuco'. However, for debugging purposes it " \
	"might be useful to start the server manuallly. To do so, use the option " \
	"--force."

static gboolean		force = FALSE;
static gboolean		log_here = FALSE;

static const GOptionEntry option_entries[] = 
{
  { "force", 'f', 0, G_OPTION_ARG_NONE, &force, OPTION_DESC_FORCE, NULL },
  { "log-here", 'l', 0, G_OPTION_ARG_NONE, &log_here, OPTION_DESC_LOG_HERE, NULL },
  { NULL }
};

///////////////////////////////////////////////////////////////////////////////
//
///////////////////////////////////////////////////////////////////////////////

static GMainLoop	*ml;
static RemServer	*server;
static RemShell		*shell;

void
rem_daemon_stop()
{
	g_assert(ml);
	
	g_main_loop_quit(ml);
}

static void
sighandler(gint sig)
{
	LOG_INFO("received signal %s", g_strsignal(sig));
	if (ml)
		rem_daemon_stop();
}

int 
main (int argc, char *argv[])
{
	GError				*err;
	GOptionContext		*context;
	gboolean			started_by_dbus, ok;
	const gchar			*logname;
	gchar				*help;
	DBusGConnection		*conn;
	struct sigaction	siga;
	
	////////// handle command line options //////////
	
	context = g_option_context_new("- Remuco server daemon");
	g_option_context_set_summary(context, HELP_SUMMARY);
	g_option_context_add_main_entries (context, option_entries, NULL);
	
	err = NULL;
	g_option_context_parse (context, &argc, &argv, &err);
	
	if (err) {
		g_printerr("%s (try option --help for valid options)\n", err->message);
		g_error_free(err);
		return 1;
	}

	started_by_dbus = (gboolean) g_getenv("DBUS_STARTER_ADDRESS");

	if (!started_by_dbus && !force) {
		
		help = g_option_context_get_help(context, TRUE, NULL);
		
		g_printerr(help);
		
		g_free(help);
		
		return 1;
	}
	
	g_option_context_free(context);
	
	////////// set up logging //////////
	
	err = NULL;
	rem_util_create_cache_dir(&err);
	if (err) {
		g_printerr("failed to create cache/log dir (%s)", err->message);
		return 1;
	}

	if (log_here) {
		ok = rem_log_init(NULL);
	} else {
		ok = rem_log_init("Server");
	}
	
	if (!ok) {
		return 1;
	}
	
	////////// misc init //////////
	
	g_type_init();
	
	////////// set up the server //////////
	
	server = g_object_new(REM_SERVER_TYPE, NULL);
	
	ok = rem_server_up(server);
	
	if (!ok) {
		return 1;
	}
	
	////////// set up shell //////////

	shell = g_object_new(REM_SHELL_TYPE, NULL);

	ok = rem_shell_up(shell, server);
	if (!ok) {
		return 1;
	}
	
	////////// export shell and server to dbus //////////
	
	err = NULL;
	conn = rem_dbus_connect(&err);
	if (err) {
		LOG_ERROR_GERR(err, "failed to connect to dbus");
		return 1;
	}
	
	err = NULL;
	rem_dbus_register(conn, &dbus_glib_rem_server_object_info,
					  REM_SERVER_TYPE, server, "Server", &err);
	if (err) {
		LOG_ERROR_GERR(err, "failed to register dbus service");
		return 1;
	}

	err = NULL;
	rem_dbus_register(conn, &dbus_glib_rem_shell_object_info,
					  REM_SHELL_TYPE, shell, "Shell", &err);
	if (err) {
		LOG_ERROR_GERR(err, "failed to register dbus service");
		return 1;
	}

	////////// signal handling //////////
	
	memclr(struct sigaction, &siga);
	siga.sa_handler = &sighandler;

	ok = sigaction(SIGINT, &siga, NULL) == 0;
	ok &= sigaction(SIGTERM, &siga, NULL) == 0;
	ok &= sigaction(SIGUSR1, &siga, NULL) == 0;
	ok &= sigaction(SIGUSR2, &siga, NULL) == 0;
	
	if (!ok) {
		LOG_ERROR("failed to set up signal handler");
		return 1;
	}	

	////////// start the main loop //////////
	
	LOG_INFO("here we go ..");
	
	ml = g_main_loop_new(NULL, FALSE);
	
	g_main_loop_run(ml);
	
	LOG_INFO("going down");

	g_main_loop_unref(ml);
	ml = NULL;
	
	rem_shell_down(shell);
	g_object_unref(shell);
	
	rem_server_down(server);
	g_object_unref(server);
	
	LOG_INFO("bye");

	return 0;
}
