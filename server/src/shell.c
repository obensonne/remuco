#include "shell.h"

#include "common.h"

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
// dbus interface
//
///////////////////////////////////////////////////////////////////////////////

gboolean
rem_shell_ping(RemShell *shell, GError **err)
{
	LOG_DEBUG("pong");
	
	return TRUE;
}

gboolean
rem_shell_shutdown(RemShell *shell, GError **err)
{
	LOG_DEBUG("received shutdown command");

	g_assert(shell->server);
	
	rem_server_down(shell->server);
	
	return TRUE;
}
