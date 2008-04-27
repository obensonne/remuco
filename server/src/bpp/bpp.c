#include <signal.h>	// for sigaction() etc.
#include <stdio.h>	// for sscanf()

#include "bpp.h"
#include "../server/config.h"
#include "../server/util.h"
#include "../server/dbus.h"
#include "dbus-server-glue.h"

///////////////////////////////////////////////////////////////////////////////
//
// macros and constants
//
///////////////////////////////////////////////////////////////////////////////

#define REM_SERVER_PP_PROTO_VERSION		1

#define REM_SERVER_ERROR_DOMAIN			g_quark_from_string("rem_server_error")

#define REM_SERVER_ERR_INVALID_DATA		"rem_server_invalid_data"
#define REM_SERVER_ERR_VERSION_MISMATCH	"rem_server_version_mismatch"
#define REM_SERVER_ERR_UNKNOWN_PLAYER	"rem_server_unknown_player"

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
	
} RemPPConfig;

///////////////////////////////////////////////////////////////////////////////
//
// pp type
//
///////////////////////////////////////////////////////////////////////////////

typedef struct {
	
	GObject			parent;
	
	gchar			*name;
	guint			flags;
	gboolean		error_shutdown;
	RemPPConfig	*config;
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
rem_pp_request_plob(RemPP *rpp,
					gchar *id, GHashTable **meta,
					GError **err);

static gboolean
rem_pp_request_ploblist(RemPP *rpp,
						gchar *id,
						gchar ***nested_ids, gchar ***nested_names,
						gchar ***ids, gchar ***names,
						GError **err);

#include "dbus-pp-glue.h"

///////////////////////////////////////////////////////////////////////////////
//
// private functions
//
///////////////////////////////////////////////////////////////////////////////

static gboolean
check_config(RemPPConfig *config) {
	
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
 * Run a command.
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
run_command(RemPP *pp, const gchar *cmd, gchar **out)
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

static gboolean
pp_running(RemPP *pp)
{
	gboolean running;
	
	running = run_command(pp, pp->config->cmd_running, NULL);
	
	if (pp->error_shutdown)
		return FALSE;

	if (!running) {
		LOG_INFO("player %s is down", pp->name);
		g_main_loop_quit(pp->ml);
		return FALSE;
	}

	return TRUE;
}

static gboolean
handle_server_error(RemPP *pp, GError *err)
{
	if (err) {
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
			LOG_ERROR_GERR(err, "failed to talk to server");
			g_main_loop_quit(pp->ml);
			pp->error_shutdown = TRUE;
			return FALSE;			
		}
	} else {
		return TRUE;
	}
}

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
		run_command(pp, pp->config->cmd_plob, &out);
		
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
			run_command(pp, pp->config->cmd_volume, &out);
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
			run_command(pp, pp->config->cmd_playing, &out);
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
		run_command(pp, pp->config->cmd_playlist, &out);
		
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
pp_new(const gchar* name, RemPPConfig *config)
{
	RemPP			*pp;
	GError			*err;
	
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
	
	////////// dbus //////////
	
	object_info_update(pp->name);
	
	err = NULL;
	pp->dbus_conn = rem_dbus_connect(&err);
	if (err) {
		LOG_ERROR_GERR(err, "failed to connect to dbus");
		pp_destroy(pp);
		return NULL;
	}
	
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
	
	run_command(pp, cmd, NULL);

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

///////////////////////////////////////////////////////////////////////////////
//
// main
//
///////////////////////////////////////////////////////////////////////////////

static RemPP				*global_pp = NULL;

static RemPPConfig			global_config;

static const RemConfigEntry	config_desc[] = {
	{ "config", "tick", G_TYPE_INT, FALSE, &global_config.tick },
	{ "config", "shell", G_TYPE_STRING, FALSE, &global_config.shell },
	{ "commands", "running", G_TYPE_STRING, FALSE, &global_config.cmd_running },
	{ "commands", "playpause", G_TYPE_STRING, FALSE, &global_config.cmd_playpause },
	{ "commands", "stop", G_TYPE_STRING, FALSE, &global_config.cmd_stop },
	{ "commands", "next", G_TYPE_STRING, FALSE, &global_config.cmd_next },
	{ "commands", "prev", G_TYPE_STRING, FALSE, &global_config.cmd_prev },
	{ "commands", "jump", G_TYPE_STRING, FALSE, &global_config.cmd_jump },
	{ "commands", "seek-fwd", G_TYPE_STRING, FALSE, &global_config.cmd_seek_fwd },
	{ "commands", "seek-bwd", G_TYPE_STRING, FALSE, &global_config.cmd_seek_bwd },
	{ "commands", "volume-set", G_TYPE_STRING, FALSE, &global_config.cmd_volume_set },
	{ "commands", "volume", G_TYPE_STRING, FALSE, &global_config.cmd_volume },
	{ "commands", "playing", G_TYPE_STRING, FALSE, &global_config.cmd_playing },
	{ "commands", "plob", G_TYPE_STRING, FALSE, &global_config.cmd_plob },
	{ "commands", "playlist", G_TYPE_STRING, FALSE, &global_config.cmd_playlist },
	{ NULL, NULL, G_TYPE_INVALID, FALSE, NULL }
};

static void
sighandler(gint sig)
{
	LOG_INFO("received signal %s", g_strsignal(sig));
	if (global_pp && global_pp->ml)
		g_main_loop_quit(global_pp->ml);
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
	
	memclr(RemPPConfig, &global_config);
	
	global_config.tick = 5;
	global_config.shell = "/bin/sh";
	
	////////// load and check config //////////
	
	ok = rem_config_load(name, "bpp", TRUE, config_desc);
	
	if (!ok)
		return 1;
	
	ok = check_config(&global_config);
	
	if (!ok)
		return 1;
	
	////////// debug mode ? //////////
	
	started_by_launcher = (gboolean) g_getenv(REM_BPP_ENV_LAUNCHER);
	
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
	
	pp = pp_new(name, &global_config);
	if (!pp)
		return 1;
	
	global_pp = pp;
	
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
