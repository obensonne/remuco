#ifndef SHELL_H_
#define SHELL_H_

#include <glib-object.h>

#include "server.h"

typedef struct {

	GObject					parent;

	RemServer				*server;
	
} RemShell;

typedef struct
{
	GObjectClass parent_class;
	
} RemShellClass;

GType
rem_shell_get_type(void);

#define REM_SHELL_TYPE		  		(rem_shell_get_type ())
#define REM_SHELL(obj)		  		(G_TYPE_CHECK_INSTANCE_CAST ((obj), REM_SHELL_TYPE, RemShell))
#define REM_SHELL_CLASS(klass)	  	(G_TYPE_CHECK_CLASS_CAST ((klass), REM_SHELL_TYPE, RemShellClass))
#define REM_IS_SHELL(obj)	  		(G_TYPE_CHECK_INSTANCE_TYPE ((obj), REM_SHELL_TYPE))
#define REM_IS_SHELL_CLASS(klass)	(G_TYPE_CHECK_CLASS_TYPE ((klass), REM_SHELL_TYPE))
#define REM_SHELL_GET_CLASS(obj)  	(G_TYPE_INSTANCE_GET_CLASS ((obj), REM_SHELL_TYPE, RemShellClass))

///////////////////////////////////////////////////////////////////////////////
//
// dbus interface
//
///////////////////////////////////////////////////////////////////////////////

gboolean
rem_shell_ping(RemShell *shell, GError **err);

gboolean
rem_shell_shutdown(RemShell *shell, GError **err);

#endif /*SHELL_H_*/
