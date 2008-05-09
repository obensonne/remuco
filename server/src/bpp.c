#include <signal.h>	// for sigaction() etc.
#include <stdio.h>	// for sscanf()

#include "bpp.h"
#include "common.h"
#include "config.h"
#include "util.h"
#include "server-glue-c.h"
#include "dbus.h"

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
// configuration
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

/** Configuration, initialized with default values. */
static Config config = {
		"/bin/sh",				/* shell */
		NULL, NULL, NULL,		/* commands */
		NULL, NULL, NULL,		/* commands */
		NULL, NULL, NULL,		/* commands */
		NULL, NULL, NULL, NULL,	/* commands */
		5						/* tick */
};

static const RemConfigEntry	config_entries[] = {
	{ "config", "tick", G_TYPE_INT, FALSE, &config.tick },
	{ "config", "shell", G_TYPE_STRING, FALSE, &config.shell },
	{ "commands", "running", G_TYPE_STRING, FALSE, &config.cmd_running },
	{ "commands", "playpause", G_TYPE_STRING, FALSE, &config.cmd_playpause },
	{ "commands", "stop", G_TYPE_STRING, FALSE, &config.cmd_stop },
	{ "commands", "next", G_TYPE_STRING, FALSE, &config.cmd_next },
	{ "commands", "prev", G_TYPE_STRING, FALSE, &config.cmd_prev },
	{ "commands", "jump", G_TYPE_STRING, FALSE, &config.cmd_jump },
	{ "commands", "seek-fwd", G_TYPE_STRING, FALSE, &config.cmd_seek_fwd },
	{ "commands", "seek-bwd", G_TYPE_STRING, FALSE, &config.cmd_seek_bwd },
	{ "commands", "volume-set", G_TYPE_STRING, FALSE, &config.cmd_volume_set },
	{ "commands", "volume", G_TYPE_STRING, FALSE, &config.cmd_volume },
	{ "commands", "playing", G_TYPE_STRING, FALSE, &config.cmd_playing },
	{ "commands", "plob", G_TYPE_STRING, FALSE, &config.cmd_plob },
	{ "commands", "playlist", G_TYPE_STRING, FALSE, &config.cmd_playlist },
	{ NULL, NULL, G_TYPE_INVALID, FALSE, NULL }
};

///////////////////////////////////////////////////////////////////////////////
//
// type defs
//
///////////////////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////////////////////////
//
// bpp type
//
///////////////////////////////////////////////////////////////////////////////

struct _RemBasicProxyPriv {
	
	gchar			*name;
	guint			flags;
	Config			*config;
	GMainLoop		*ml;
	gchar			*argv[4]; // { shell, '-c', cmd, NULL }
	DBusGConnection	*dbus_conn;
	DBusGProxy		*dbus_proxy;
	GHashTable		*plob;
	gboolean		idle_sync_triggered;
	
};

G_DEFINE_TYPE(RemBasicProxy, rem_basic_proxy, G_TYPE_OBJECT);

static void
rem_basic_proxy_class_init(RemBasicProxyClass *klass)
{
	// nothing to do
}

static void
rem_basic_proxy_init(RemBasicProxy *bpp)
{
	// nothing to do
}

///////////////////////////////////////////////////////////////////////////////
//
// private functions
//
///////////////////////////////////////////////////////////////////////////////

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
execute(RemBasicProxy *bpp, const gchar *cmd, gchar **out)
{
	gchar	*stdout, *stderr;
	gint	exit;
	GError	*err;
	guint	l;
	
	g_assert(!out || (out && !*out));
	
	if (!cmd)
		return TRUE;
	
	bpp->priv->argv[2] = (gchar*) cmd;
	
	err = NULL, stderr = NULL, exit = 0;
	g_spawn_sync(NULL, bpp->priv->argv, NULL, 0, NULL, NULL, &stdout, &stderr,
				 &exit, &err);
	
	if (err) { // serious error
		LOG_ERROR_GERR(err, "failed to run '%s'", cmd);
		g_free(stdout);
		g_free(stderr);
		bpp->error = TRUE;
		rem_bpp_down(bpp);
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
pp_running(RemBasicProxy *bpp)
{
	gboolean running;
	
	running = execute(bpp, bpp->priv->config->cmd_running, NULL);
	
	if (bpp->error) {
		return FALSE;
	}

	if (!running) {
		LOG_INFO("player is down");
		rem_bpp_down(bpp);
		return FALSE;
	}

	return TRUE;
}

/** Handle a possible error in a reply from the server. */
static gboolean
handle_server_error(RemBasicProxy *bpp, GError *err)
{
	if (!err)
		return TRUE;
	
	if (err->domain = DBUS_GERROR && err->code == DBUS_GERROR_NO_REPLY) {
			
		LOG_WARN_GERR(err, "no reply from server, probably it is busy");
		return TRUE; // don't care
	}
	
	if (err->domain = REM_SERVER_ERR_DOMAIN) {
		
		if (g_str_equal(err->message, REM_SERVER_ERR_UNKNOWN_PLAYER)) {
			g_error_free(err);
			LOG_INFO("have to reconnect to server");
			err = NULL;
			net_sf_remuco_Server_hello(bpp->priv->dbus_proxy, bpp->priv->name,
									   bpp->priv->flags, 0, &err);
			if (err) {
				LOG_ERROR_GERR(err, "failed to say hello to server");
				bpp->error = TRUE;
				rem_bpp_down(bpp);
				return FALSE;
			} else {
				return TRUE;
			}
			
		} else {
			
			LOG_ERROR_GERR(err, "unexpected server error (looks like a bug)");
			bpp->error = TRUE;
			rem_bpp_down(bpp);
			return FALSE;			
		}
		
	}
		
	LOG_ERROR_GERR(err, "failed to talk to server");
	bpp->error = TRUE;
	rem_bpp_down(bpp);
	return FALSE;			
}

/** Get the current player state. */
static gboolean
sync(RemBasicProxy *bpp)
{
	gchar		*out, **entries;
	GError		*err;
	gboolean	have_plob, ok;
	
	guint		volume;
	guint		playing;
	gint		ret;

	LOG_DEBUG("pp sync");
	
	if (!pp_running(bpp))
		return FALSE;
	
	////////// plob //////////

	have_plob = FALSE;
	
	if (bpp->priv->config->cmd_plob) {
	
		out = NULL;
		execute(bpp, bpp->priv->config->cmd_plob, &out);
		
		LOG_DEBUG("plob: '%s'", out ? out : "");
		
		have_plob = out ? TRUE : FALSE;
		
		g_hash_table_insert(bpp->priv->plob, REM_PLOB_META_ABSTRACT, out);

		err = NULL;
		net_sf_remuco_Server_update_plob(bpp->priv->dbus_proxy, bpp->priv->name,
										 out, NULL, bpp->priv->plob, &err);

		ok = handle_server_error(bpp, err);
		if (!ok)
			return FALSE;
	}
	
	////////// state //////////
	
	// we only can say sth. about player state if one of these commands is set
	if (bpp->priv->config->cmd_plob || bpp->priv->config->cmd_volume ||
		bpp->priv->config->cmd_playing) {
		
		volume = -1;
		if (bpp->priv->config->cmd_volume) {
			out = NULL;
			execute(bpp, bpp->priv->config->cmd_volume, &out);
			LOG_DEBUG("volume: '%s'", out ? out : "");
			ret = sscanf(out ? out : "", "%u", &volume);
			if (ret != 1) {
				LOG_DEBUG("output '%s' of command '%s' is not valid",
						 out, bpp->priv->config->cmd_volume);
				volume = 0;
			}
			g_free(out);
		}
			
		playing = have_plob ? REM_PLAYBACK_PLAY : REM_PLAYBACK_STOP; 
		if (bpp->priv->config->cmd_playing) {
			out = NULL;
			execute(bpp, bpp->priv->config->cmd_playing, &out);
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
		net_sf_remuco_Server_update_state(bpp->priv->dbus_proxy,
										  bpp->priv->name, playing, volume,
										  FALSE, FALSE, 0, FALSE, &err);
	
		ok = handle_server_error(bpp, err);
		if (!ok)
			return FALSE;
	}
	
	////////// playlist //////////
	
	if (bpp->priv->config->cmd_playlist) {
	
		out = NULL; entries = NULL;
		execute(bpp, bpp->priv->config->cmd_playlist, &out);
		
		entries = out ? g_strsplit(out, "\n", 0) : NULL;
	
		rem_util_dump_sv("playlist", entries);
		
		g_free(out);
	
		err = NULL;
		net_sf_remuco_Server_update_playlist(bpp->priv->dbus_proxy,
											 bpp->priv->name,
											 (const gchar**) entries,
											 (const gchar**) entries, &err);
		
		g_strfreev(entries);
		
		ok = handle_server_error(bpp, err);
		if (!ok)
			return FALSE;
	}
	
	return TRUE;
}

static gboolean
sync_once(RemBasicProxy *bpp)
{
	sync(bpp);
	bpp->priv->idle_sync_triggered = FALSE;
	return FALSE;
}

///////////////////////////////////////////////////////////////////////////////
//
// dbus interface
//
///////////////////////////////////////////////////////////////////////////////

gboolean
rem_pp_control(RemBasicProxy *bpp,
			   guint control, gint paramI, gchar *paramS,
			   GError **err)
{
	gchar		*cmd, *s;
	gboolean	trigger_sync;
	
	if (!pp_running(bpp))
		return TRUE;
	
	LOG_DEBUG("called (%u, %i, '%s')", control, paramI, paramS);
	
	s = NULL; cmd = NULL; trigger_sync = TRUE;
	
	switch (control) {
		case REM_CTL_PLAYPAUSE:
			cmd = bpp->priv->config->cmd_playpause;
		break;
		case REM_CTL_NEXT:
			cmd = bpp->priv->config->cmd_next;
		break;
		case REM_CTL_PREV:
			cmd = bpp->priv->config->cmd_prev;
		break;
		case REM_CTL_JUMP:
			if (!g_str_equal(paramS, REM_PLAYLIST_ID))
				break;
			cmd = bpp->priv->config->cmd_jump;
			s = g_strdup_printf("%i", paramI - 1);
		break;
		case REM_CTL_STOP:
			cmd = bpp->priv->config->cmd_stop;
		break;
		case REM_CTL_SEEK_FWD:
			cmd = bpp->priv->config->cmd_seek_fwd;
			trigger_sync = FALSE;
		break;
		case REM_CTL_SEEK_BWD:
			cmd = bpp->priv->config->cmd_seek_bwd;
			trigger_sync = FALSE;
		break;
		case REM_CTL_VOLUME:
			cmd = bpp->priv->config->cmd_volume_set;
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
	
	execute(bpp, cmd, NULL);

	if (trigger_sync && !bpp->priv->idle_sync_triggered) {
		bpp->priv->idle_sync_triggered = TRUE;
		g_idle_add((GSourceFunc) &sync_once, bpp);
	}

	return TRUE;
}

gboolean
rem_pp_request_plob(RemBasicProxy *bpp,
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

gboolean
rem_pp_request_ploblist(RemBasicProxy *bpp,
						gchar *id,
						gchar ***nested_ids, gchar ***nested_names,
						gchar ***ids, gchar ***names,
						GError **err)
{
	// nothing to do
	return TRUE;
}

gboolean
rem_pp_bye(RemBasicProxy *bpp, GError **err)
{
	LOG_INFO("server said bye");
	bpp->priv->dbus_proxy = NULL; // prevent a 'bye' to the server
	rem_bpp_down(bpp);
	return TRUE;
}

///////////////////////////////////////////////////////////////////////////////
//
// main
//
///////////////////////////////////////////////////////////////////////////////

/**
 * On successfull setup, TRUE is returned. Otherwise FALSE is returned and
 * bpp->error says if there was an error (TRUE) or if the player was simply not
 * running (FALSE).
 */
gboolean
rem_bpp_up(RemBasicProxy *bpp, const gchar *name, GMainLoop *ml)
{
	gboolean			ok;
	GError				*err;
	gboolean			one_cmd_is_set;
	
	g_assert(!bpp->priv);
	
	bpp->error = FALSE;
	
	////////// load and check config //////////
	
	ok = rem_config_load(name, "bpp", TRUE, config_entries);
	
	if (!ok) {
		bpp->error = TRUE;
		return FALSE;
	}
	
	if (config.tick < 1) {
		LOG_ERROR("tick must be > 0");
		bpp->error = TRUE;
		return FALSE;
	}
	
	one_cmd_is_set =
		config.cmd_running || config.cmd_playpause || config.cmd_next ||
		config.cmd_prev || config.cmd_jump || config.cmd_stop ||
		config.cmd_seek_fwd || config.cmd_seek_bwd ||
		config.cmd_volume_set ||
		config.cmd_volume || config.cmd_playing ||
		config.cmd_plob || config.cmd_playlist;
	
	if (!one_cmd_is_set) {
		LOG_ERROR("at least one command must be set");
		bpp->error = TRUE;
		return FALSE;
	}
	
	if (config.cmd_volume_set && !config.cmd_volume) {
			
		LOG_ERROR("command 'volume-set' requires command 'volume'");
		bpp->error = TRUE;
		return FALSE;
	}
		
	////////// pp init //////////

	bpp->priv = g_slice_new0(RemBasicProxyPriv);
	
	bpp->priv->config = &config;
	
	bpp->priv->flags =
		REM_FLAG_BPP |
		(bpp->priv->config->cmd_volume ? 0 : REM_FLAG_VOLUME_UNKNOWN) |
		(bpp->priv->config->cmd_playing ? 0 : REM_FLAG_PLAYBACK_UNKNOWN);
	
	////////// argument vector for child processes //////////
	
	bpp->priv->argv[0] = bpp->priv->config->shell;
	bpp->priv->argv[1] = "-c";
	bpp->priv->argv[2] = NULL; // commands go here;
	bpp->priv->argv[3] = NULL;
	
	////////// dbus connection //////////
	
	err = NULL;
	bpp->priv->dbus_conn = rem_dbus_connect(&err);
	if (err) {
		LOG_ERROR_GERR(err, "failed to connect to dbus");
		g_slice_free(RemBasicProxyPriv, bpp->priv);
		bpp->error = TRUE;
		return FALSE;
	}
	
	////////// get dbus proxy for server //////////
	
	bpp->priv->dbus_proxy = rem_dbus_proxy(bpp->priv->dbus_conn, "Server");
	
	////////// check if player is running, otherwise .. bye //////////
	
	if (!pp_running(bpp)) {
		g_slice_free(RemBasicProxyPriv, bpp->priv);
		return FALSE;
	}
	
	////////// go for it //////////
	
	g_timeout_add(bpp->priv->config->tick * 1000, (GSourceFunc) &sync, bpp);
	
	bpp->priv->name = g_strdup(name);

	err = NULL;
	net_sf_remuco_Server_hello(bpp->priv->dbus_proxy, bpp->priv->name,
							   bpp->priv->flags, 0, &err);
	if (err) {
		LOG_ERROR_GERR(err, "failed to say hello to server");
		g_free(bpp->priv->name);
		g_slice_free(RemBasicProxyPriv, bpp->priv);
		bpp->error = TRUE;
		return FALSE;
	}
	
	
	bpp->priv->plob = g_hash_table_new_full(&g_str_hash, &g_str_equal, NULL,
											&g_free);
	
	g_main_loop_ref(ml);
	
	bpp->priv->ml = ml;
	
	return TRUE;
}

void
rem_bpp_down(RemBasicProxy *bpp)
{
	RemBasicProxyPriv	*priv;
	
	if (!bpp->priv)
		return;
	
	priv = bpp->priv;
	
	bpp->priv = NULL;
	
	LOG_INFO("goind down");
	
	if (priv->dbus_proxy) // if server said bye, dbus-proxy is NULL
		net_sf_remuco_Server_bye(priv->dbus_proxy, priv->name, NULL);
	
	g_free(priv->name);
	
	if (priv->plob)
		g_hash_table_destroy(priv->plob);

	g_main_loop_quit(priv->ml);
	
	g_main_loop_unref(priv->ml);
	
	g_slice_free(RemBasicProxyPriv, priv);
}
