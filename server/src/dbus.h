#ifndef DBUS_H_
#define DBUS_H_

#include <dbus/dbus-glib-bindings.h>

////////// server <-> proxy interaction protocol version //////////

#define REM_SERVER_PP_PROTO_VERSION		2

////////// errors exported via dbus //////////

#define REM_SERVER_ERR_DOMAIN			g_quark_from_string("rem_server_error")

#define REM_SERVER_ERR_INVALID_DATA		"Conveyed data is invalid."
#define REM_SERVER_ERR_INVALID_DATA_NUM	1
#define REM_SERVER_ERR_VERSION			"Server version incompatible."
#define REM_SERVER_ERR_VERSION_NUM		2
#define REM_SERVER_ERR_UNKNOWN			"Conveyed player name is unknown."
#define REM_SERVER_ERR_UNKNOWN_NUM		3

G_BEGIN_DECLS

gboolean
rem_dbus_check_name(const gchar *s);

DBusGConnection*
rem_dbus_connect(GError **err);

void
rem_dbus_register(DBusGConnection *conn, const DBusGObjectInfo *object_info,
				  GType object_type, gpointer object, const gchar *name,
				  GError **err);

DBusGProxy*
rem_dbus_proxy(DBusGConnection *conn, const gchar *name);

G_END_DECLS

#endif /*DBUS_H_*/
