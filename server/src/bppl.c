#include <sys/types.h>	// for WIFEXITED, see waitpid(2)
#include <sys/wait.h>	// for WIFEXITED, see waitpid(2)

#include "bppl.h"
#include "util.h"

///////////////////////////////////////////////////////////////////////////////
//
// constants
//
///////////////////////////////////////////////////////////////////////////////


///////////////////////////////////////////////////////////////////////////////
//
// typedefs
//
///////////////////////////////////////////////////////////////////////////////

struct _RemBasicProxyLauncher {
	
	gchar			*argv[3];
	
	GHashTable		*proxies;
	
};

typedef struct {
	
	gchar					*name;
	GPid					pid;
	RemBasicProxyLauncher	*launcher;
	
} RemBasicProxy;

///////////////////////////////////////////////////////////////////////////////
//
// private functions
//
///////////////////////////////////////////////////////////////////////////////

static void
bpp_destroy(RemBasicProxy *proxy)
{
	if (!proxy)
		return;
	
	g_free(proxy->name);

	if (proxy->pid) {
		g_source_remove_by_user_data(proxy);
		g_spawn_close_pid(proxy->pid); // does nothing on *nix
	}
	
	g_slice_free(RemBasicProxy, proxy);
}

static void
bpp_exited(GPid pid, gint status, RemBasicProxy *proxy)
{
	gchar	*log;
	
	g_spawn_close_pid(pid);
	
	proxy->pid = 0;

	if (WIFEXITED(status)) {
		switch (WEXITSTATUS(status)) {
			case REM_BPP_RET_ERROR:
				LOG_WARN("BPP %s experienced errors", proxy->name);
				break;
			case REM_BPP_RET_PLAYER_DOWN:
				LOG_DEBUG("%s is not running", proxy->name);
				return;
			case REM_BPP_RET_SERVER_BYE:
				LOG_DEBUG("BPP %s has been disabled", proxy->name);
				break;
			case REM_BPP_RET_OK:
				LOG_DEBUG("%s has gone down", proxy->name);
				return;
			default:
				g_assert_not_reached();
				break;
		}
	} else {
		LOG_WARN("BPP %s exited unnormally", proxy->name);
	}
	
	log = g_build_filename(g_get_user_cache_dir(), "remuco", proxy->name, NULL);
	
	LOG_WARN("inspect the BPP's log file for more information (%s.log)", log);
	
	g_free(log);
	
	LOG_DEBUG("disable BPP %s", proxy->name);
	
	g_hash_table_remove(proxy->launcher->proxies, proxy->name);
}

/** Launch a BPP if it is not running already. */
static void
bpp_launch(gpointer key, gpointer value, gpointer data)
{
	RemBasicProxy	*proxy;
	GError			*err;
	
	proxy = (RemBasicProxy*) value;
	
	if (proxy->pid) // already running
		return;

	LOG_DEBUG("starting BPP for %s", proxy->name);
	
	proxy->launcher->argv[1] = proxy->name;
	err = NULL;
	g_spawn_async(NULL, proxy->launcher->argv, NULL,
				  G_SPAWN_SEARCH_PATH | G_SPAWN_DO_NOT_REAP_CHILD |
				  G_SPAWN_STDOUT_TO_DEV_NULL | G_SPAWN_STDERR_TO_DEV_NULL,
				  NULL, NULL, &proxy->pid, &err);

	if (err) {
		LOG_ERROR_GERR(err, "failed to start BPP %s", proxy->name);
		g_hash_table_remove(proxy->launcher->proxies, proxy->name);
		return;
	}
	
	g_child_watch_add(proxy->pid, (GChildWatchFunc) &bpp_exited, proxy);

}

/** Iterate all proxies and start them if not running already. */ 
static gboolean
launch_bpps(RemBasicProxyLauncher *launcher)
{
	g_hash_table_foreach(launcher->proxies, &bpp_launch, NULL);
	
	return TRUE;
}

/** Iterate all proxies and start them if not running already. */ 
static gboolean
launch_bpps_first(RemBasicProxyLauncher *launcher)
{
	launch_bpps(launcher);
	
	g_timeout_add(10000, (GSourceFunc) &launch_bpps, launcher);
	
	return FALSE;
}

static void
setup_bpps_from_dir(RemBasicProxyLauncher *launcher, const gchar *dir_name)
{
	GError			*err;
	GDir			*dir;
	const gchar		*entry;
	gchar			*name;
	guint			entry_len;
	gboolean		entry_valid;
	RemBasicProxy	*proxy;
	
	LOG_DEBUG("checking for BPPs in '%s'", dir_name);
	
	if (!g_file_test(dir_name, G_FILE_TEST_IS_DIR)) {
		return;
	}
	
	err = NULL;
	dir = g_dir_open(dir_name, 0, &err);
	if (err) {
		LOG_WARN_GERR(err, "failed to read BPP dir '%s'", dir_name);
		return;
	}
	
	////////// iterate all bpp-files //////////

	for (entry = g_dir_read_name(dir); entry; entry = g_dir_read_name(dir)) {
		
		////////// check if entry is a BPP file //////////
		
		entry_len = strlen(entry);
		entry_valid = entry_len > strlen(REM_BPP_FILE_SUFFIX);
		entry_valid &= g_str_has_suffix(entry, REM_BPP_FILE_SUFFIX);

		if (!entry_valid) {
			continue;
		}
		
		////////// setup a BPP //////////
		
		// cut suffix from entry to use it as a BPP name
		name = g_strdup(entry);
		name[entry_len - strlen(REM_BPP_FILE_SUFFIX)] = '\0';

		proxy = g_slice_new0(RemBasicProxy);
		
		proxy->launcher = launcher;
		proxy->name = name;
		g_hash_table_insert(launcher->proxies, proxy->name, proxy);
		
		LOG_DEBUG("found BPP %s", proxy->name);
	}
	
	g_dir_close(dir);
	
	return;
}

static gboolean
setup_bpps(RemBasicProxyLauncher *launcher)
{
	gchar			*dir_name;
	gboolean		ok;
	GSList			*dirs, *dirs_iter;
	gchar			*dir;
	const gchar		*env;
	
	dirs = NULL;
	
	////////// dirs to look for BPPs //////////

//  Loading BPPs from these dirs requires that bpp.c receives a full path
//	as argument and that bpp.c (via config.c) is able to load a BPP file from
//	other dies that ~/.config/remuco.
//	So for now, only user config dir is used for BPP files.
	
//	dir = g_build_filename("usr", "lib", "remuco", NULL);
//	dirs = g_slist_append(dirs, dir);
//	
//	dir = g_build_filename("usr", "local", "lib", "remuco", NULL);
//	dirs = g_slist_append(dirs, dir);
//
//	env = g_getenv(REM_ENV_BPP_EXTRA_DIR);
//	if (env) {
//		dirs = g_slist_append(dirs, g_strdup(env));
//	}
	
	dir = g_build_filename(g_get_user_config_dir(), "remuco", NULL);
	dirs = g_slist_append(dirs, dir);

	////////// iterate the dirs and set up the proxies //////////

	for (dirs_iter = dirs; dirs_iter; dirs_iter = dirs_iter->next) {
	
		dir = (gchar*) dirs_iter->data;
		
		setup_bpps_from_dir(launcher, dir);
	
		g_free(dir);
	
	}
	
	g_slist_free(dirs);
	
	return TRUE;
}

///////////////////////////////////////////////////////////////////////////////
//
// public functions
//
///////////////////////////////////////////////////////////////////////////////

/** Never returns NULL */
RemBasicProxyLauncher*
rem_bppl_up(void)
{
	RemBasicProxyLauncher	*launcher;
	gboolean				ok;
	
	LOG_DEBUG("set up BPPs");
	
	launcher = g_slice_new0(RemBasicProxyLauncher);
	
	launcher->proxies = g_hash_table_new_full(&g_str_hash, &g_str_equal, NULL,
											  (GDestroyNotify) &bpp_destroy);
	
	launcher->argv[0] = "remuco-bpp";
	launcher->argv[1] = NULL; // bpp file goes here
	launcher->argv[2] = NULL;
	
	g_setenv(REM_ENV_BPP_LAUNCHER, "x", FALSE);

	////////// set up pps //////////
	
	ok = setup_bpps(launcher);
	
	if (!ok)
		LOG_ERROR("there have been erors setting up the BPPs");
	
	if (ok && !g_hash_table_size(launcher->proxies)) {
		LOG_INFO("did not found any BPPs");
		ok = FALSE;
	}
	
	////////// go for it //////////
	
	if (ok) {
		g_idle_add((GSourceFunc) &launch_bpps_first, launcher);
	}
	
	return launcher;
}

void
rem_bppl_down(RemBasicProxyLauncher *launcher)
{
	if (!launcher)
		return;
	
	LOG_DEBUG("shutdown bppl");
	
	g_source_remove_by_user_data(launcher);
	
	g_hash_table_destroy(launcher->proxies);
	
	g_slice_free(RemBasicProxyLauncher, launcher);
}

guint
rem_bppl_bpp_count(RemBasicProxyLauncher *launcher)
{
	g_assert(launcher);
	g_assert(launcher->proxies);
	
	return g_hash_table_size(launcher->proxies);
}

