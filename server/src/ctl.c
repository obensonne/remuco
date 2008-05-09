#include "dbus.h"
#include "server-glue-c.h"
#include "shell-glue-c.h"

#define HELP_SUMMARY \
	"Tool to control the Remuco server. If no arguments are given, the server" \
	" gets started (if not running already)."


static gboolean	stop = FALSE; 

static const GOptionEntry entries[] = 
{
  { "stop", 0, 0, G_OPTION_ARG_NONE, &stop, "stop the server", NULL },
  { NULL }
};

int main(int argc, char **argv) {
	
	GError				*err;
	GOptionContext		*context;
	DBusGProxy			*proxy;
	DBusGConnection		*conn;
	gboolean			running;
	
	g_type_init();
	
	////////// parse command line options //////////
	
	context = g_option_context_new ("- Remuco server control");
	g_option_context_set_summary(context, HELP_SUMMARY);
	g_option_context_add_main_entries (context, entries, NULL);
	
	err = NULL;
	g_option_context_parse (context, &argc, &argv, &err);
	
	if (err) {
		g_printerr("%s (try option --help for valid options)\n", err->message);
		g_error_free(err);
		return 1;
	}
	
	////////// connect to dbus //////////
	
	err = NULL;
	conn = rem_dbus_connect(&err);
	if (err) {
		g_printerr("failed to connect to dbus (%s)\n", err->message);
		g_error_free(err);
		return 1;
	}
	
	
	////////// start/stop the server //////////
	
	proxy = rem_dbus_proxy(conn, "Shell");

	err = NULL;
	net_sf_remuco_Shell_ping(proxy, &err);
	
	g_object_unref(proxy);
	
	if (err && err->domain == DBUS_GERROR &&
		err->code == DBUS_GERROR_SERVICE_UNKNOWN) {
		
		running = FALSE;
		g_error_free(err);
		
	} else if (err && err->domain == DBUS_GERROR &&
			   err->code == DBUS_GERROR_NO_REPLY) {
		
		g_print(".. server is running and seems to be very busy\n");
		running = TRUE;
		g_error_free(err);
		
	} else if (err) {
		
		g_printerr("Failed to contact server - cannot say if server is "
				   "running (%s).\n", err->message);
		g_error_free(err);
		return 1;
		
	} else {
		
		running = TRUE;
	}

	if (stop) {	
		
		if (!running) {
			g_print("Server is not running.\n");
			return 0;			
		}
		
		proxy = rem_dbus_proxy(conn, "Shell");
		
		err = NULL;
		net_sf_remuco_Shell_shutdown(proxy, &err);
		
		g_object_unref(proxy);
		
		if (err) {
			
			g_printerr("Failed to stop server (%s).\n", err->message);
			g_error_free(err);
			return 1;
			
		} else {
			
			g_print("Ok, server stopped.\n");
			return 0;			
		}
		
	} else {
		
		if (running) {
			g_print("Server is running.\n");
			return 0;			
		}
		
		proxy = rem_dbus_proxy(conn, "Server");
		
		err = NULL;
		net_sf_remuco_Server_check(proxy, REM_SERVER_PP_PROTO_VERSION, &err);
		
		g_object_unref(proxy);
		
		if (err) {
			
			g_printerr("Failed to start server (%s).\n", err->message);
			g_error_free(err);
			return 1;
			
		} else {
			
			g_print("Ok, server started.\n");
			return 0;			
		}
	}

	g_assert_not_reached();
}
