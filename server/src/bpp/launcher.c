#include <signal.h>		// for sigaction() etc.
#include <sys/types.h>	// for WIFEXITED, see waitpid(2)
#include <sys/wait.h>	// for WIFEXITED, see waitpid(2)

#include "bpp.h"
#include "../server/util.h"

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

typedef struct {
	
	GMainLoop		*ml;
	gchar			*argv[3];
	
	GHashTable		*bpps;
	
} RemLauncher;

typedef struct {
	
	gchar			*name;
	GPid			pid;
	RemLauncher		*launcher;
	
} RemBPP;

///////////////////////////////////////////////////////////////////////////////
//
// pp specific functions
//
///////////////////////////////////////////////////////////////////////////////

static void
bpp_destroy(RemBPP *bpp)
{
	if (!bpp)
		return;
	
	g_free(bpp->name);

	if (bpp->pid)
		g_spawn_close_pid(bpp->pid); // does nothing on *nix
	
	g_slice_free(RemBPP, bpp);
}

static void
bpp_exited(GPid pid, gint status, RemBPP *bpp)
{
	LOG_DEBUG("BPP %s exited", bpp->name);
	
	g_spawn_close_pid(pid);
	
	bpp->pid = 0;

	if (WIFEXITED(status) && WEXITSTATUS(status) == 0)
		return; // normal exit
	
	LOG_WARN("BPP %s exited unnormally -> don't try to restart", bpp->name);
	
	g_hash_table_remove(bpp->launcher->bpps, bpp->name);
}

/** Launch a BPP if it is not running already. */
static void
launch_bpp(gpointer key, gpointer value, gpointer data)
{
	RemBPP		*bpp;
	GError		*err;
	
	bpp = (RemBPP*) value;
	
	if (bpp->pid) // already running
		return;

	LOG_DEBUG("try to start BPP '%s'", bpp->name);
	
	bpp->launcher->argv[1] = bpp->name;
	err = NULL;
	g_spawn_async(NULL, bpp->launcher->argv, NULL,
				  G_SPAWN_SEARCH_PATH | G_SPAWN_DO_NOT_REAP_CHILD |
				  G_SPAWN_STDOUT_TO_DEV_NULL | G_SPAWN_STDERR_TO_DEV_NULL,
				  NULL, NULL, &bpp->pid, &err);

	if (err) {
		LOG_ERROR_GERR(err, "failed to start BPP %s", bpp->name);
		g_main_loop_quit(bpp->launcher->ml);
		return;
	}
	
	g_child_watch_add(bpp->pid, (GChildWatchFunc) &bpp_exited, bpp);

}

/** Run over all BPPs and starts them if they do not run already. */ 
static gboolean
launch_bpps(RemLauncher *launcher)
{
	g_hash_table_foreach(launcher->bpps, &launch_bpp, NULL);
	
	return TRUE;
}

static gboolean
launch_bpps_first(RemLauncher *launcher)
{
	launch_bpps(launcher);
	
	g_timeout_add(10000, (GSourceFunc) &launch_bpps, launcher);
	
	return FALSE;
}

static gboolean
setup_bpps_from_dir(RemLauncher *launcher, const gchar *dir_name)
{
	GError		*err;
	GDir		*dir;
	const gchar	*entry;
	gchar		*name;
	guint		entry_len;
	gboolean	entry_valid;
	RemBPP		*bpp;
	
	if (!g_file_test(dir_name, G_FILE_TEST_IS_DIR)) {
		return TRUE;
	}
	
	err = NULL;
	dir = g_dir_open(dir_name, 0, &err);
	if (err) {
		LOG_ERROR_GERR(err, "failed to read dir '%s'", dir_name);
		return FALSE;
	}
	
	////////// loop over all bpp-files //////////

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

		bpp = g_slice_new0(RemBPP);
		
		bpp->launcher = launcher;
		bpp->name = name;
		g_hash_table_insert(launcher->bpps, bpp->name, bpp);
		
	}
	
	g_dir_close(dir);
	
	return TRUE;
	
}

static gboolean
setup_bpps(RemLauncher *launcher)
{
	gchar			*dir_name;
	gboolean		ok;
	
	
	////////// get bpp files from remuco lib dir //////////

//	dir_name = g_build_filename(REM_PREFIX, "usr", "lib", "remuco", NULL);
//	
//	ok = setup_bpps_from_dir(launcher, dir_name);
//	
//	g_free(dir_name);
//	
//	if (!ok)
//		return FALSE;

	////////// get bpp files from user config //////////
	
	dir_name = g_build_filename(g_get_user_config_dir(), "remuco", NULL);
	
	ok = setup_bpps_from_dir(launcher, dir_name);
	
	g_free(dir_name);
	
	if (!ok)
		return FALSE;

	return TRUE;	

}

///////////////////////////////////////////////////////////////////////////////
//
// main
//
///////////////////////////////////////////////////////////////////////////////

static RemLauncher *launcher_g = NULL; // global access for sighandler

static void
sighandler(gint sig)
{
	LOG_INFO("received signal %s", g_strsignal(sig));
	if (launcher_g && launcher_g->ml)
		g_main_loop_quit(launcher_g->ml);
}

int main(int argc, char **argv)
{
	gboolean			ok;
	struct sigaction	siga;
	const gchar			*logname;
	
	ok = rem_util_create_cache_dir();
	if (!ok)
		return 1;

	////////// debug mode ? //////////
	
	if (argc >= 2 && g_str_equal(argv[1], "--log-here"))
		logname = NULL;
	else
		logname = "BPP-Launcher";

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
	
	////////// init some data //////////
	
	launcher_g = g_slice_new0(RemLauncher);
	
	launcher_g->bpps = g_hash_table_new_full(&g_str_hash, &g_str_equal, NULL,
											 (GDestroyNotify) &bpp_destroy);
	
	launcher_g->ml = g_main_loop_new(NULL, FALSE);
	
	launcher_g->argv[0] = "remuco-bpp";
	launcher_g->argv[1] = NULL; // bpp file goes here
	launcher_g->argv[2] = NULL;
	
	g_setenv(REM_BPP_ENV_LAUNCHER, "x", FALSE);

	////////// set up pps //////////
	
	ok = setup_bpps(launcher_g);
	
	if (!ok)
		LOG_ERROR("there have been erors setting up the BPPs");
	
	if (ok && !g_hash_table_size(launcher_g->bpps)) {
		LOG_INFO("did not found any BPPs");
		ok = FALSE;
	}
	
	////////// go for it //////////
	
	if (ok) {
		g_idle_add((GSourceFunc) &launch_bpps_first, launcher_g);
		LOG_INFO("up and running");
		g_main_loop_run(launcher_g->ml);
	}
	
	LOG_INFO("going down");

	////////// clean up //////////
	
	g_main_loop_unref(launcher_g->ml);
	
	g_hash_table_destroy(launcher_g->bpps);
	
	g_slice_free(RemLauncher, launcher_g);
	
	return ok ? 0 : 1;
}
