#include <signal.h>	// for sigaction() etc.
#include <stdio.h>	// for sscanf()

#include "common.h"
#include "config.h"
#include "util.h"
#include "dbus.h"
#include "bpp-dbus-server-glue.h"

///////////////////////////////////////////////////////////////////////////////
//
// macros and constants
//
///////////////////////////////////////////////////////////////////////////////

#define REM_BPP_CTL_PARAM_ENVKEY		"PARAM"

#define REM_FLAG_BPP					(1 << 16)
#define REM_FLAG_VOLUME_UNKNOWN			(1 << 17)
#define REM_FLAG_PLAYBACK_UNKNOWN		(1 << 18)

///////////////////////////////////////////////////////////////////////////////

#define REM_PLOB_META_ABSTRACT			"__abstract__"

#define REM_PLAYLIST_ID					"__PLAYLIST__"
#define REM_QUEUE_ID					"__QUEUE__"

#define REM_PLAYBACK_STOP				0
#define	REM_PLAYBACK_PAUSE				1
#define REM_PLAYBACK_PLAY				2

#define REM_CTL_IGNORE					0
#define REM_CTL_PLAYPAUSE				1
#define REM_CTL_STOP					2
#define REM_CTL_NEXT					3
#define REM_CTL_PREV					4
#define REM_CTL_JUMP					5
#define REM_CTL_SEEK_FWD				6
#define REM_CTL_SEEK_BWD				7
#define REM_CTL_VOLUME					8
#define REM_CTL_RATE					9
#define REM_CTL_PLAYNEXT				10
#define REM_CTL_SETTAGS					12
#define REM_CTL_REPEAT					13
#define REM_CTL_SHUFFLE					14

///////////////////////////////////////////////////////////////////////////////
//
// type defs
//
///////////////////////////////////////////////////////////////////////////////

typedef struct {
	
	gchar			*shell;
	gchar			*cmd_running, *cmd_playpause, *cmd_next,
					*cmd_prev, *cmd_jump, *cmd_stop,
					*cmd_seek_fwd,	*cmd_seek_bwd, *cmd_volume_set,
					*cmd_playing, *cmd_volume, *cmd_plob, *cmd_playlist;

	guint			tick;
	
} Config;

///////////////////////////////////////////////////////////////////////////////
//
// pp type exported via dbus
//
///////////////////////////////////////////////////////////////////////////////

typedef struct {
	
	GObject			parent;
	
	gchar			*name;
	guint			flags;
	gboolean		error_shutdown;
	Config			*config;
	GMainLoop		*ml;
	gchar			*argv[4]; // { shell, '-c', cmd, NULL }
	DBusGConnection	*dbus_conn;
	DBusGProxy		*dbus_proxy;
	GHashTable		*plob;
	gboolean		idle_sync_triggered;
	
} RemPP;

typedef struct
{
	GObjectClass parent_class;
	
} RemPPClass;

G_DEFINE_TYPE(RemPP, rem_pp, G_TYPE_OBJECT);

static void
rem_pp_class_init(RemPPClass *class) {
	// nothing to do
}

static void
rem_pp_init(RemPP *pp) {
	// nothing to do
}

///////////////////////////////////////////////////////////////////////////////
//
// DBUS interface - function prototypes
//
///////////////////////////////////////////////////////////////////////////////

static gboolean
rem_pp_control(RemPP *pp,
			   guint control, gint paramI, gchar *paramS,
			   GError **err);

static gboolean
rem_pp_request_plob(RemPP *pp,
					gchar *id, GHashTable **meta,
					GError **err);

static gboolean
rem_pp_request_ploblist(RemPP *pp,
						gchar *id,
						gchar ***nested_ids, gchar ***nested_names,
						gchar ***ids, gchar ***names,
						GError **err);

static gboolean
rem_pp_bye(RemPP *pp, GError **err);

#include "bpp-dbus-pp-glue.h"

///////////////////////////////////////////////////////////////////////////////
//
// private functions
//
///////////////////////////////////////////////////////////////////////////////

static gboolean
check_config(Config *config) {
	
	gboolean ok, cmd;
	
	ok = TRUE;
	
	if (config->tick < 1) {
		LOG_ERROR("tick must be > 0");
		ok = FALSE;
	}
	
	cmd = config->cmd_running || config->cmd_playpause || config->cmd_next ||
		 config->cmd_prev || config->cmd_jump || config->cmd_stop ||
		 config->cmd_seek_fwd || config->cmd_seek_bwd ||
		 config->cmd_volume_set ||
		 config->cmd_volume || config->cmd_playing ||
		 config->cmd_plob || config->cmd_playlist;
	
	if (!cmd) {
		LOG_ERROR("at least one command must be set");
		ok = FALSE;
	}
	
	if (config->cmd_volume_set && !config->cmd_volume) {
			
		LOG_ERROR("command 'volume-set' requires command 'volume'");
		ok = FALSE;
	}
	
	return ok;
}

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

/**
 * Execute a command.
 * If command cannot be executed, the main loop will be stopped and
 * 'pp->error_shutdown' is set TRUE. 
 * 
 * @param[out]
 * 		out where to store standard output into (*out will never be the empty
 * 		string - it is either NULL or a string with some content)
 * 
 * @return
 * 		TRUE if 'cmd' is NULL or if the command exited with 0
 * 		FALSE if the command exited with other than 0
 */
static gboolean
execute(RemPP *pp, const gchar *cmd, gchar **out)
{
	gchar	*stdout, *stderr;
	gint	exit;
	GError	*err;
	guint	l;
	
	g_assert(!out || (out && !*out));
	
	if (!cmd)
		return TRUE;
	
	pp->argv[2] = (gchar*) cmd;
	
	err = NULL, stderr = NULL, exit = 0;
	g_spawn_sync(NULL, pp->argv, NULL, 0, NULL, NULL, &stdout, &stderr, &exit,
				 &err);
	
	if (err) { // serious error :(
		LOG_ERROR_GERR(err, "failed to run '%s'", cmd);
		g_free(stdout);
		g_free(stderr);
		g_main_loop_quit(pp->ml);
		pp->error_shutdown = TRUE;
		return FALSE;
	}
	
	g_assert(stdout && stderr);
	
	if (exit) {	// acceptable error :|
		LOG_DEBUG("command '%s' returned with error:\n"
				  "--------------- ERROR OUTPUT ---------------\n"
				  "%s"
				  "--------------------------------------------", cmd, stderr);
		g_free(stdout);
		g_free(stderr);
		return FALSE;
	}

	g_free(stderr);

	
	// cut the trailing new line:
	if (out) {
		l = strlen(stdout);
		if (l && stdout[l - 1] == '\n')
			stdout[l - 1] = '\0';
		if (strlen(stdout))
			*out = stdout;
		else
			g_free(stdout);
	} else {
		g_free(stdout);		
	}
	
	return TRUE;
}

/** Check if our player is running. */
static gboolean
pp_running(RemPP *pp)
{
	gboolean running;
	
	running = execute(pp, pp->config->cmd_running, NULL);
	
	if (pp->error_shutdown)
		return FALSE;

	if (!running) {
		LOG_INFO("player %s is down", pp->name);
		g_main_loop_quit(pp->ml);
		return FALSE;
	}

	return TRUE;
}

/** Handle a possible error in a reply from the server. */
static gboolean
handle_server_error(RemPP *pp, GError *err)
{
	if (!err)
		return TRUE;
	
	if (err->domain = DBUS_GERROR && err->code == DBUS_GERROR_NO_REPLY) {
			
		LOG_WARN_GERR(err, "no reply from server, probably it is busy");
		return TRUE;
	}
	
	if (err->domain = REM_SERVER_ERR_DOMAIN) {
		
		if (g_str_equal(err->message, REM_SERVER_ERR_UNKNOWN_PLAYER)) {
			g_error_free(err);
			LOG_INFO("have to reconnect to server");
			err = NULL;
			rem_server_hello(pp->dbus_proxy, pp->name, pp->flags, 0, &err);
			if (err) {
				LOG_ERROR_GERR(err, "failed to say hello to server");
				g_main_loop_quit(pp->ml);
				pp->error_shutdown = TRUE;
				return FALSE;
			} else {
				return TRUE;
			}
			
		} else {
			
			LOG_ERROR_GERR(err, "unexpected server error (looks like a bug)");
			g_main_loop_quit(pp->ml);
			pp->error_shutdown = TRUE;
			return FALSE;			
		}
		
	}
		
	LOG_ERROR_GERR(err, "failed to talk to server");
	g_main_loop_quit(pp->ml);
	pp->error_shutdown = TRUE;
	return FALSE;			
}

/** Get the current player state. */
static gboolean
sync(RemPP *pp)
{
	gchar		*out, **entries;
	GError		*err;
	gboolean	have_plob, ok;
	
	guint		volume;
	guint		playing;
	gint		ret;

	LOG_DEBUG("pp sync");
	
	if (!pp_running(pp))
		return FALSE;
	
	////////// plob //////////

	have_plob = FALSE;
	
	if (pp->config->cmd_plob) {
	
		out = NULL;
		execute(pp, pp->config->cmd_plob, &out);
		
		LOG_DEBUG("plob: '%s'", out ? out : "");
		
		have_plob = out ? TRUE : FALSE;
		
		g_hash_table_insert(pp->plob, REM_PLOB_META_ABSTRACT, out);

		err = NULL;
		rem_server_update_plob(pp->dbus_proxy, pp->name, out, NULL, pp->plob,
							   &err);

		ok = handle_server_error(pp, err);
		if (!ok)
			return FALSE;
	}
	
	////////// state //////////
	
	// we only can say sth. about player state if one of these commands is set
	if (pp->config->cmd_plob || pp->config->cmd_volume || pp->config->cmd_playing) {
		
		volume = -1;
		if (pp->config->cmd_volume) {
			out = NULL;
			execute(pp, pp->config->cmd_volume, &out);
			LOG_DEBUG("volume: '%s'", out ? out : "");
			ret = sscanf(out ? out : "", "%u", &volume);
			if (ret != 1) {
				LOG_DEBUG("output '%s' of command '%s' is not valid",
						 out, pp->config->cmd_volume);
				volume = 0;
			}
			g_free(out);
		}
			
		playing = have_plob ? REM_PLAYBACK_PLAY : REM_PLAYBACK_STOP; 
		if (pp->config->cmd_playing) {
			out = NULL;
			execute(pp, pp->config->cmd_playing, &out);
			LOG_DEBUG("playing: '%s'", out ? out : "");
			if (rem_util_s2b(out))
				playing = REM_PLAYBACK_PLAY;
			else if (have_plob)
				playing = REM_PLAYBACK_PAUSE;
			else
				playing = REM_PLAYBACK_STOP;
			g_free(out);
		}
			
		err = NULL;
		rem_server_update_state(pp->dbus_proxy, pp->name, playing, volume,
								FALSE, FALSE, 0, FALSE, &err);
	
		ok = handle_server_error(pp, err);
		if (!ok)
			return FALSE;
	}
	
	////////// playlist //////////
	
	if (pp->config->cmd_playlist) {
	
		out = NULL; entries = NULL;
		execute(pp, pp->config->cmd_playlist, &out);
		
		entries = out ? g_strsplit(out, "\n", 0) : NULL;
	
		rem_util_dump_sv("playlist", entries);
		
		g_free(out);
	
		err = NULL;
		rem_server_update_playlist(pp->dbus_proxy, pp->name,
								   (const gchar**) entries, // id equals name
								   (const gchar**) entries, &err);
		
		g_strfreev(entries);
		
		ok = handle_server_error(pp, err);
		if (!ok)
			return FALSE;
	}
	
	return TRUE;
}

static gboolean
sync_once(RemPP *pp)
{
	sync(pp);
	pp->idle_sync_triggered = FALSE;
	return FALSE;
}

static void
pp_destroy(RemPP *pp)
{
	if (!pp)
		return;
	
	g_free(pp->name);
	
	if (pp->plob)
		g_hash_table_destroy(pp->plob);
	
	pp->config = NULL;
	
	if (pp->ml)
		g_main_loop_unref(pp->ml);
	
	g_object_unref(pp);
}

static RemPP*
pp_new(const gchar* name, Config *config)
{
	RemPP			*pp;
	GError			*err;
	DBusGProxy		*self_test_proxy;
	
	////////// init //////////
	
	g_type_init();

	pp = g_object_new(rem_pp_get_type(), NULL);
	
	pp->config = config;
	
	pp->ml = g_main_loop_new(NULL, FALSE);
	
	pp->name = g_strdup(name);
	
	pp->plob = g_hash_table_new_full(&g_str_hash, &g_str_equal, NULL, &g_free);
	
	pp->flags = REM_FLAG_BPP;
	pp->flags |= pp->config->cmd_volume ? 0 :REM_FLAG_VOLUME_UNKNOWN;
	pp->flags |= pp->config->cmd_playing ? 0 : REM_FLAG_PLAYBACK_UNKNOWN; 
	
	////////// argument vector for child processes //////////
	
	pp->argv[0] = pp->config->shell;
	pp->argv[1] = "-c";
	pp->argv[2] = NULL; // commands go here;
	pp->argv[3] = NULL;
	
	////////// dbus connection //////////
	
	object_info_update(pp->name);
	
	err = NULL;
	pp->dbus_conn = rem_dbus_connect(&err);
	if (err) {
		LOG_ERROR_GERR(err, "failed to connect to dbus");
		pp_destroy(pp);
		return NULL;
	}
	
	////////// chek if there already is a proxy for our player //////////
	
	self_test_proxy = rem_dbus_proxy(pp->dbus_conn, pp->name);
	
	err = NULL;
	dbus_g_proxy_call(self_test_proxy, "Control", &err,
					  G_TYPE_UINT, REM_CTL_IGNORE, G_TYPE_INT, 0,
					  G_TYPE_STRING, "", G_TYPE_INVALID, G_TYPE_INVALID);

	g_object_unref(self_test_proxy);
	
	if (err && err->domain == DBUS_GERROR &&
		err->code == DBUS_GERROR_SERVICE_UNKNOWN) {
		
		LOG_DEBUG("ok, no proxy for %s running yet", pp->name);
		g_error_free(err);
		
	} else if (err) {
		
		LOG_WARN_GERR(err, "failed to talk pp %s - however, it looks like a "
					  "proxy for %s is already running", pp->name, pp->name);
		pp_destroy(pp);
		return NULL;
		
	} else {
		
		LOG_WARN("a proxy for %s is already running", pp->name);
		pp_destroy(pp);
		return NULL;
	}
	
	////////// get dbus proxy for server //////////
	
	pp->dbus_proxy = rem_dbus_proxy(pp->dbus_conn, "Server");
	
	err = NULL;
	rem_dbus_register(pp->dbus_conn, &dbus_glib_rem_pp_object_info,
					  rem_pp_get_type(), pp, pp->name, &err);
	if (err) {
		LOG_ERROR_GERR(err, "failed to register dbus service");
		pp_destroy(pp);
		return NULL;
	}
	
	return pp;
}

///////////////////////////////////////////////////////////////////////////////
//
// DBUS interface - implementation
//
///////////////////////////////////////////////////////////////////////////////

static gboolean
rem_pp_control(RemPP *pp,
			   guint control, gint paramI, gchar *paramS,
			   GError **err)
{
	gchar		*cmd, *s;
	gboolean	trigger_sync;
	
	if (!pp_running(pp))
		return TRUE;
	
	LOG_DEBUG("called (%u, %i, '%s')", control, paramI, paramS);
	
	s = NULL; cmd = NULL; trigger_sync = TRUE;
	
	switch (control) {
		case REM_CTL_PLAYPAUSE:
			cmd = pp->config->cmd_playpause;
		break;
		case REM_CTL_NEXT:
			cmd = pp->config->cmd_next;
		break;
		case REM_CTL_PREV:
			cmd = pp->config->cmd_prev;
		break;
		case REM_CTL_JUMP:
			if (!g_str_equal(paramS, REM_PLAYLIST_ID))
				break;
			cmd = pp->config->cmd_jump;
			s = g_strdup_printf("%i", paramI - 1);
		break;
		case REM_CTL_STOP:
			cmd = pp->config->cmd_stop;
		break;
		case REM_CTL_SEEK_FWD:
			cmd = pp->config->cmd_seek_fwd;
			trigger_sync = FALSE;
		break;
		case REM_CTL_SEEK_BWD:
			cmd = pp->config->cmd_seek_bwd;
			trigger_sync = FALSE;
		break;
		case REM_CTL_VOLUME:
			cmd = pp->config->cmd_volume_set;
			s = g_strdup_printf("%i", paramI);
			trigger_sync = FALSE;
		break;
		case REM_CTL_IGNORE:
			return TRUE; // ignore
		break;
		default:
			LOG_WARN("control %u not supported", control);
			return TRUE;
		break;
	}
	
	if (s) {
		g_setenv(REM_BPP_CTL_PARAM_ENVKEY, s, TRUE);
		g_free(s);
	} else {
		g_unsetenv(REM_BPP_CTL_PARAM_ENVKEY);
	}
	
	execute(pp, cmd, NULL);

	if (trigger_sync && !pp->idle_sync_triggered) {
		pp->idle_sync_triggered = TRUE;
		g_idle_add((GSourceFunc) &sync_once, pp);
	}

	return TRUE;
}

static gboolean
rem_pp_request_plob(RemPP *pp,
					gchar *id, GHashTable **meta,
					GError **err)
{
	g_assert(id);
	g_assert(meta && !*meta);
	
	LOG_DEBUG("server requests plob '%s'", id);

	*meta = g_hash_table_new_full(&g_str_hash, &g_str_equal, NULL, &g_free);

	// here id equals name
	g_hash_table_insert(*meta, REM_PLOB_META_ABSTRACT, g_strdup(id));
	
	return TRUE;
}

static gboolean
rem_pp_request_ploblist(RemPP *rpp,
						gchar *id,
						gchar ***nested_ids, gchar ***nested_names,
						gchar ***ids, gchar ***names,
						GError **err)
{
	// nothing to do
	return TRUE;
}

static gboolean
rem_pp_bye(RemPP *pp, GError **err)
{
	LOG_INFO("server said bye");
	g_main_loop_quit(pp->ml);
	pp->dbus_proxy = NULL; // prevent a 'bye' to the server
	return TRUE;
}

///////////////////////////////////////////////////////////////////////////////
//
// main
//
///////////////////////////////////////////////////////////////////////////////

static RemPP	*pp_g = NULL;

static Config	config_g;

static const RemConfigEntry	config_desc[] = {
	{ "config", "tick", G_TYPE_INT, FALSE, &config_g.tick },
	{ "config", "shell", G_TYPE_STRING, FALSE, &config_g.shell },
	{ "commands", "running", G_TYPE_STRING, FALSE, &config_g.cmd_running },
	{ "commands", "playpause", G_TYPE_STRING, FALSE, &config_g.cmd_playpause },
	{ "commands", "stop", G_TYPE_STRING, FALSE, &config_g.cmd_stop },
	{ "commands", "next", G_TYPE_STRING, FALSE, &config_g.cmd_next },
	{ "commands", "prev", G_TYPE_STRING, FALSE, &config_g.cmd_prev },
	{ "commands", "jump", G_TYPE_STRING, FALSE, &config_g.cmd_jump },
	{ "commands", "seek-fwd", G_TYPE_STRING, FALSE, &config_g.cmd_seek_fwd },
	{ "commands", "seek-bwd", G_TYPE_STRING, FALSE, &config_g.cmd_seek_bwd },
	{ "commands", "volume-set", G_TYPE_STRING, FALSE, &config_g.cmd_volume_set },
	{ "commands", "volume", G_TYPE_STRING, FALSE, &config_g.cmd_volume },
	{ "commands", "playing", G_TYPE_STRING, FALSE, &config_g.cmd_playing },
	{ "commands", "plob", G_TYPE_STRING, FALSE, &config_g.cmd_plob },
	{ "commands", "playlist", G_TYPE_STRING, FALSE, &config_g.cmd_playlist },
	{ NULL, NULL, G_TYPE_INVALID, FALSE, NULL }
};

static void
sighandler(gint sig)
{
	LOG_INFO("received signal %s", g_strsignal(sig));
	if (pp_g && pp_g->ml)
		g_main_loop_quit(pp_g->ml);
}

int main(int argc, char **argv)
{
	RemPP				*pp;
	gboolean			ok, started_by_launcher;
	struct sigaction	siga;
	gint				ret;
	GError				*err;
	const gchar			*name, *logname;
	
	if (argc != 2) {
		g_printerr("Usage: remuco-bpp BPP-FILE\n");
		return 1;
	}
	
	name = argv[1];
	
	////////// set default config //////////
	
	memclr(Config, &config_g);
	
	config_g.tick = 5;
	config_g.shell = "/bin/sh";
	
	////////// load and check config //////////
	
	ok = rem_config_load(name, "bpp", TRUE, config_desc);
	
	if (!ok)
		return 1;
	
	ok = check_config(&config_g);
	
	if (!ok)
		return 1;
	
	////////// debug mode ? //////////
	
	started_by_launcher = (gboolean) g_getenv(REM_ENV_BPP_LAUNCHER);
	
	if (!started_by_launcher) {
		g_print("- - - manual invocation -> log goes to stdout/err - - -\n");
		logname = NULL;
	} else {
		logname = name;
	}

	ok = rem_util_create_cache_dir();
	if (!ok)
		return 1;

	rem_log_init(logname);

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
	
	////////// pp init //////////
	
	pp = pp_new(name, &config_g);
	if (!pp)
		return 1;
	
	pp_g = pp;
	
	////////// check if player is running, otherwise .. bye //////////
	
	if (!pp_running(pp)) {
		pp_destroy(pp);
		return 0;
	}
	
	////////// go for it //////////
	
	g_timeout_add(pp->config->tick * 1000, (GSourceFunc) &sync, pp);
	
	err = NULL;
	rem_server_hello(pp->dbus_proxy, pp->name, pp->flags, 0, &err);
	if (err) {
		LOG_ERROR_GERR(err, "failed to say hello to server");
		pp_destroy(pp);
		return 1;
	}
	
	LOG_INFO("up and running");
	
	g_main_loop_run(pp->ml);
	
	LOG_INFO("going down");

	////////// clean up //////////
	
	ret = pp->error_shutdown ? 1 : 0;
	
	if (pp && pp->dbus_proxy && pp->name)
		rem_server_bye(pp->dbus_proxy, pp->name, NULL);

	pp_destroy(pp);
	
	return ret;
}
