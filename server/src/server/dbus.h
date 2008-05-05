#ifndef DBUS_H_
#define DBUS_H_

#include <dbus/dbus-glib-bindings.h>

////////// server <-> proxy interaction protocol version //////////

#define REM_SERVER_PP_PROTO_VERSION		1

////////// server errors exported via dbus //////////

#define REM_SERVER_ERR_DOMAIN			g_quark_from_string("rem_server_error")

#define REM_SERVER_ERR_INVALID_DATA		"rem_server_invalid_data"
#define REM_SERVER_ERR_VERSION_MISMATCH	"rem_server_version_mismatch"
#define REM_SERVER_ERR_UNKNOWN_PLAYER	"rem_server_unknown_player"

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
