#ifndef DBUS_H_
#define DBUS_H_

#include <dbus/dbus-glib-bindings.h>

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
