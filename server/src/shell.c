#include "shell.h"
#include "daemon.h"
#include "dbus.h"
#include "common.h"

struct _RemShellPriv {
	
	RemServer	*server;
	
};

G_DEFINE_TYPE(RemShell, rem_shell, G_TYPE_OBJECT);

static void
rem_shell_class_init(RemShellClass *klass)
{
}

static void
rem_shell_init(RemShell *shell)
{
}

///////////////////////////////////////////////////////////////////////////////
//
// internal interface
//
///////////////////////////////////////////////////////////////////////////////

gboolean
rem_shell_up(RemShell *shell, RemServer *server)
{
	g_assert(!shell->priv);
	
	shell->priv = g_slice_new0(RemShellPriv);
	
	shell->priv->server = server;
	
	g_object_ref(shell->priv->server);
}

void
rem_shell_down(RemShell *shell)
{
	if (!shell)
		return;
	
	if (!shell->priv)
		return;
	
	g_object_unref(shell->priv->server);
	
	g_slice_free(RemShellPriv, shell->priv);
	
	shell->priv = NULL;
}

///////////////////////////////////////////////////////////////////////////////
//
// dbus interface
//
///////////////////////////////////////////////////////////////////////////////

gboolean
rem_shell_start(RemShell *shell, guint version, GError **err)
{
	g_assert(shell->priv);

	LOG_INFO("called");
	
	if (version != REM_SERVER_PP_PROTO_VERSION) {
		g_set_error(err, REM_SERVER_ERR_DOMAIN, REM_SERVER_ERR_VERSION_NUM,
					REM_SERVER_ERR_VERSION);
		return FALSE;
	}
	
	return TRUE;
}

gboolean
rem_shell_stop(RemShell *shell, GError **err)
{
	g_assert(shell->priv);

	LOG_DEBUG("received stop command");
	
	g_assert(shell->priv->server);

	rem_daemon_stop();
	
	return TRUE;
}

gboolean
rem_shell_get_proxies(RemShell *shell, gchar ***proxies, GError **err)
{
	g_assert(shell->priv);
	
	LOG_DEBUG("proxy list requested");
	
	g_assert(shell->priv->server);

	*proxies = rem_server_get_proxies(shell->priv->server);
	
	return TRUE;	
}

gboolean
rem_shell_get_clients(RemShell *shell, gchar ***clients, GError **err)
{
	g_assert(shell->priv);

	LOG_DEBUG("client list requested");
	
	g_assert(shell->priv->server);

	*clients = rem_server_get_clients(shell->priv->server);
	
	return TRUE;
}

gboolean
rem_shell_disable_proxy(RemShell *shell, gchar *proxy, GError **err)
{
	g_assert(shell->priv);

	LOG_DEBUG("external proxy stop (%s)", proxy);

	g_assert(shell->priv->server);

	rem_server_disable_proxy(shell->priv->server, proxy);
	
	return TRUE;
}
