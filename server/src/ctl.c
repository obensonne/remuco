#include "dbus.h"
#include "common.h"
#include "server-glue-c.h"
#include "shell-glue-c.h"

///////////////////////////////////////////////////////////////////////////////
//
// command line options
//
///////////////////////////////////////////////////////////////////////////////

#define HELP_SUMMARY \
	"Tool to control the Remuco server. If no arguments are given, status of " \
	"the server is printed."


static gboolean	version = FALSE; 
static gboolean	stop = FALSE; 
static gboolean start = FALSE;
static gchar	*proxy = NULL;

static const GOptionEntry entries[] = 
{
  { "version", 'v', 0, G_OPTION_ARG_NONE, &version, "Show version", NULL },
  { "stop", 'o', 0, G_OPTION_ARG_NONE, &stop, "Stop the server", NULL },
  { "start", 'a', 0, G_OPTION_ARG_NONE, &start, "Start the server", NULL },
  { "stop-proxy", 'p', 0, G_OPTION_ARG_STRING, &proxy, "Stop player proxy NAME", "NAME" },
  { NULL }
};

///////////////////////////////////////////////////////////////////////////////
//
// vars (global)
//
///////////////////////////////////////////////////////////////////////////////

static gboolean	running = FALSE;

static DBusGConnection	*dbus_conn;

///////////////////////////////////////////////////////////////////////////////
//
// private functions
//
///////////////////////////////////////////////////////////////////////////////

static gboolean
check_running()
{
	GError				*err;
	DBusGProxy			*dbus_proxy;

	////////// connect to dbus //////////
	
	err = NULL;
	dbus_conn = rem_dbus_connect(&err);
	if (err) {
		g_printerr("Failed to connect to dbus (%s)\n", err->message);
		g_error_free(err);
		return FALSE;
	}
	
	////////// check if server is running //////////
	
	dbus_proxy = rem_dbus_proxy(dbus_conn, "Server");

	err = NULL;
	net_sf_remuco_Server_hello(dbus_proxy, "Shell", 0, 0, &err);
	
	g_object_unref(dbus_proxy);
	
	if (err && err->domain == DBUS_GERROR &&
		err->code == DBUS_GERROR_SERVICE_UNKNOWN) {
		
		g_error_free(err);
		return TRUE;
		
	} else if (err && err->domain == DBUS_GERROR &&
			   err->code == DBUS_GERROR_NO_REPLY) {
		
		running = TRUE;
		g_printerr("Failed to contact server - did not recevie a reply. "
				   "Probably it is running, but currently very busy\n");
		g_error_free(err);
		return FALSE;
		
	} else if (err) {
		
		g_printerr("Failed to contact server - cannot say if server is "
				   "running (%s).\n", err->message);
		g_error_free(err);
		return FALSE;
		
	} else {
		
		running = TRUE;
		return TRUE;
	}
	
	g_assert_not_reached();
}

static gboolean
server_stop()
{
	DBusGProxy		*dbus_proxy;
	GError			*err;

	if (!running) {
		g_print("Server is not running.\n");
		return TRUE;			
	}
	
	dbus_proxy = rem_dbus_proxy(dbus_conn, "Shell");
	
	err = NULL;
	net_sf_remuco_Shell_stop(dbus_proxy, &err);
	
	g_object_unref(dbus_proxy);
	
	if (err) {
		
		g_printerr("Failed to stop server (%s).\n", err->message);
		g_error_free(err);
		return FALSE;
		
	} else {
		
		g_print("Ok, server stopped.\n");
		return TRUE;			
	}	
}

static gboolean
server_start()
{
	DBusGProxy		*dbus_proxy;
	GError			*err;
	
	if (running) {
		g_print("Server is running.\n");
		return TRUE;			
	}
	
	dbus_proxy = rem_dbus_proxy(dbus_conn, "Shell");
	
	err = NULL;
	net_sf_remuco_Shell_start(dbus_proxy, REM_SERVER_PP_PROTO_VERSION, &err);
	
	g_object_unref(dbus_proxy);
	
	if (err) {
		
		g_printerr("Failed to start server (%s).\n", err->message);
		g_error_free(err);
		return FALSE;
		
	} else {
		
		g_print("Ok, server started.\n");
		return TRUE;			
	}
}

static gboolean
proxy_stop()
{
	DBusGProxy		*dbus_proxy;
	GError			*err;
	gchar			**proxies;
	guint			len, u;
	
	if (!running) {
		g_print("Server is not running.\n");
		return TRUE;			
	}
	
	dbus_proxy = rem_dbus_proxy(dbus_conn, "Shell");
	
	////////// get a list of proxies to check if 'proxy' is valid //////////
	
	err = NULL;
	proxies = NULL;
	net_sf_remuco_Shell_get_proxies(dbus_proxy, &proxies, &err);

	if (err) {
		
		g_printerr("Failed to contact server (%s).\n", err->message);
		g_error_free(err);
		return FALSE;
	}
	
	len = proxies ? g_strv_length(proxies) : 0;
	
	for (u = 0; u < len; u++) {
		if (g_str_equal(proxies[u], proxy)) {
			break; // proxy is connected to the server
		}
	}
	
	g_strfreev(proxies);

	if (u == len) {
		g_printerr("A proxy named %s is not connected to the server.\n", proxy);
		g_object_unref(dbus_proxy);
		return FALSE;
	}
	
	////////// stop the proxy //////////
	
	err = NULL;
	net_sf_remuco_Shell_disable_proxy(dbus_proxy, proxy, &err);
	
	g_object_unref(dbus_proxy);
	
	if (err) {
		
		g_printerr("Failed to contact server (%s).\n", err->message);
		g_error_free(err);
		return FALSE;
		
	} else {
		
		g_print("Ok, proxy %s stopped.\n", proxy);
		return TRUE;			
	}	
	
}

static gboolean
print_status()
{
	DBusGProxy		*dbus_proxy;
	gchar			**proxies, **clients;
	guint			len, u;
	GError			*err;

	////////// running status //////////
	
	if (!running) {
		g_print("Server is not running.\n");
		return TRUE;
	}
	
	g_print("Server is running.\n");

	////////// player proxies //////////
	
	dbus_proxy = rem_dbus_proxy(dbus_conn, "Shell");

	err = NULL;
	proxies = NULL;
	net_sf_remuco_Shell_get_proxies(dbus_proxy, &proxies, &err);

	if (err) {
		
		g_printerr("Failed to contact server (%s).\n", err->message);
		g_error_free(err);
		return FALSE;
	}
	
	g_print("Connected player proxies:");
	
	len = proxies ? g_strv_length(proxies) : 0;
	
	for (u = 0; u < len; u++) {
		g_print(" %s", proxies[u]);
	}
	g_print("\n");

	g_strfreev(proxies);

	////////// clients //////////
	
	err = NULL;
	proxies = NULL;
	net_sf_remuco_Shell_get_clients(dbus_proxy, &clients, &err);

	if (err) {
		
		g_printerr("Failed to contact server (%s).\n", err->message);
		g_error_free(err);
		return FALSE;
	}
	
	g_print("Connected clients:");
	
	len = clients ? g_strv_length(clients) : 0;
	
	for (u = 0; u < len; u++) {
		g_print(" %s", clients[u]);
	}
	g_print("\n");
	
	g_strfreev(clients);
	
	return TRUE;
}

///////////////////////////////////////////////////////////////////////////////
//
// main
//
///////////////////////////////////////////////////////////////////////////////

int
main(int argc, char **argv) {
	
	GError				*err;
	GOptionContext		*context;
	gboolean			ok;
	
	////////// parse command line options //////////
	
	context = g_option_context_new ("- Remuco server control");
	g_option_context_set_summary(context, HELP_SUMMARY);
	g_option_context_add_main_entries (context, entries, NULL);
	
	err = NULL;
	g_option_context_parse (context, &argc, &argv, &err);
	
	g_option_context_free(context);
	
	if (err) {
		g_printerr("%s (try option --help for valid options)\n", err->message);
		g_error_free(err);
		return 1;
	}
	
	if (version) {
		g_print("Remuco %s\n", REM_VERSION);
		return 0;
	}
	
	if ((stop && start) || (stop && proxy) || (start && proxy)) {
		g_printerr("Only one control at the same time is possible.\n");
		return 1;
	}

	g_type_init();
	
	////////// server running ? //////////
	
	ok = check_running();
	if (!ok)
		return 1;
	
	////////// do what we have to do //////////
	
	if (stop) {	
		
		ok = server_stop();
		
	} else if (start) {
		
		ok = server_start();

	} else if (proxy) {
		
		ok = proxy_stop();

	} else {
		
		ok = print_status();
		
	}

	return ok ? 0 : 1;

}
