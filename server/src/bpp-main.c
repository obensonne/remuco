#include <signal.h>	// for sigaction() etc.

#include "bpp.h"
#include "common.h"
#include "pp-glue-s.h"
#include "pp-glue-c.h"
#include "dbus.h"

static GMainLoop		*ml;
static RemBasicProxy	*bpp;

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
	"Basic player proxies are intended to be started automatically by the " \
	"server. However, for debugging purposes it might be useful to start a " \
	"basic player proxy  manuallly. To do so, use the option --force.\n" \
	"PLAYER_NAME specifies the .bpp file to use. For instance 'Totem' would " \
	"use the file 'Totem.bpp' (usually placed in ~/.config/remuco)."

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

static void
object_info_update(const gchar *name)
{
	GError	*err;
	gchar	*replacement, *ois; 
	GRegex	*regex;
	guint	ois_len, u;
	gint	name_len_diff;
	
	DBusGObjectInfo	*oi;
	DBusGMethodInfo	*mi;
	
	oi = &dbus_glib_rem_pp_object_info;
	mi = dbus_glib_rem_pp_methods;
	
	ois = (gchar*) oi->data;
	
	////////// calculate length of object info string //////////
	
	// FIXME: assuming ois ends with four '\0'
	
	for (u = 0; ois[u] || ois[u+1] || ois[u+2] || ois[u+4]; u++);
	ois_len = u + 3;
	
	////////// update object info string //////////
	
	regex = g_regex_new("net.sf.remuco.PP", G_REGEX_RAW, 0, NULL);

	replacement = g_strdup_printf("net.sf.remuco.%s", name);
	
	// update object info string
	err = NULL;
	ois = g_regex_replace_literal(regex, ois, ois_len, 0, replacement, 0, &err);
	g_assert(ois && !err);

	g_regex_unref(regex);
	g_free(replacement);

	oi->data = ois;	
	
	////////// update offsets in method infos array //////////
	
	name_len_diff = strlen(name) - strlen("PP");	

	for (u = 1; u < oi->n_method_infos; u++)
		
		mi[u].data_offset += name_len_diff * u;	
	
	oi->method_infos = mi;
}

static void
sighandler(gint sig)
{
	LOG_INFO("received signal %s", g_strsignal(sig));
	
	rem_bpp_down(bpp);
}

int
main (int argc, char *argv[])
{
	GError				*err;
	GOptionContext		*context;
	gboolean			started_by_bppl, ok;
	const gchar			*name;
	struct sigaction	siga;
	gchar				*help;
	DBusGConnection		*dbus_conn;
	DBusGProxy			*dbus_proxy_tmp;
	gint				ret;

	////////// handle command line options //////////
	
	context = g_option_context_new("PLAYER_NAME");
	g_option_context_set_summary(context, HELP_SUMMARY);
	g_option_context_add_main_entries (context, option_entries, NULL);
	
	err = NULL;
	g_option_context_parse (context, &argc, &argv, &err);
	
	if (err) {
		g_printerr("%s (try option --help for valid options)\n", err->message);
		g_error_free(err);
		return 1;
	}

	started_by_bppl = (gboolean) g_getenv(REM_ENV_BPP_LAUNCHER);

	if ((!started_by_bppl && !force) || (argc < 2 || !argv[1])) {
		
		help = g_option_context_get_help(context, TRUE, NULL);
		
		g_printerr(help);
		
		g_free(help);
		
		return 1;
	}
	
	g_option_context_free(context);

	name = argv[1];
	
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
		ok = rem_log_init(name);
	}
	
	if (!ok) {
		return 1;
	}
	
	////////// misc init //////////
	
	g_type_init();
	
	ml = g_main_loop_new(NULL, FALSE);
	
	////////// connect to dbus //////////
	
	err = NULL;
	dbus_conn = rem_dbus_connect(&err);
	if (err) {
		LOG_ERROR_GERR(err, "failed to connect to dbus");
		return 1;
	}

	////////// check if there already is a proxy for 'name' //////////
	
	LOG_DEBUG("check if there already is a proxy for %s", name);
	
	dbus_proxy_tmp = rem_dbus_proxy(dbus_conn, name);
	
	err = NULL;
	net_sf_remuco_PP_control(dbus_proxy_tmp, 0, 0, "", &err);
	
	g_object_unref(dbus_proxy_tmp);
	
	if (err) {
		
		if (err->domain == DBUS_GERROR) {
			
			if (err->code == DBUS_GERROR_SERVICE_UNKNOWN) {
		
				LOG_DEBUG("ok, no proxy for %s running yet", name);
				g_error_free(err);

			} else if (err) {
		
				LOG_WARN_GERR(err, "failed to talk proxy for %s - however, it "
							  "looks like a proxy for %s is already running",
							  name, name);
				return 1;
			}
			
		} else {
			
			LOG_ERROR_GERR(err, "an unexpected error occured (%s)\n");
			LOG_ERROR("please file a bug\n");
			return 1;
			
		}
	
	} else {
		
		LOG_WARN("a proxy for %s is already running", name);
		return 1;
	}

	////////// set up the proxy //////////
	
	bpp = g_object_new(REM_BASIC_PROXY_TYPE, NULL);
	
	ok = rem_bpp_up(bpp, name, ml);
	
	if (!ok) {
		g_object_unref(bpp);
		return bpp->error ? 1 : 0;
	}
	
	////////// export proxy to dbus //////////
	
	object_info_update(name);
	
	err = NULL;
	rem_dbus_register(dbus_conn, &dbus_glib_rem_pp_object_info,
					  REM_BASIC_PROXY_TYPE, bpp, name, &err);
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
	
	g_main_loop_run(ml);
	
	LOG_INFO("bye");
		
	g_main_loop_unref(ml);

	ret = bpp->error ? 1 : 0;
	
	g_object_unref(bpp);
	
	return ret;
}
