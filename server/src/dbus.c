#include "dbus.h"
#include <string.h>

#define REM_DBUS_INTROSPECTABLE	"org.freedesktop.DBus.Introspectable.Introspect"

#define REM_DBUS_NAMESPACE_DOTTED	"net.sf.remuco"
#define REM_DBUS_NAMESPACE_SLASHED	"/net/sf/remuco"

#define REM_DBUS_SERVER_SERVICE	REM_DBUS_NAMESPACE_DOTTED ".Server"
#define REM_DBUS_SERVER_PATH	REM_DBUS_NAMESPACE_SLASHED "/Server"
#define REM_DBUS_SERVER_IFACE	REM_DBUS_NAMESPACE_DOTTED".Server"

#define REM_DBUS_NAME_MAX_LEN	(255 - strlen(REM_DBUS_NAMESPACE_DOTTED) - 1)

/**
 * Check if a string is a valid DBUS service name. The sring is valid if it
 * contains only the chars '[a-z][A-Z][0-9]_' and if it does not start with a
 * digit.
 * 
 * @see http://dbus.freedesktop.org/doc/dbus-specification.html#message-protocol-names
 */
gboolean
rem_dbus_check_name(const gchar *s)
{
	guint	u, l;
	
	if (!s || !s[0])
		return FALSE;
	
	if (s[0] >= '0' && s[0] <= '9')
		return FALSE;
	
	l = REM_DBUS_NAME_MAX_LEN;
	
	for (u = 0; s[u] && u < l; u++) {
		if (!((s[u] >= 'a' && s[u] <= 'z') ||
			  (s[u] >= 'A' && s[u] <= 'Z') ||
			  (s[u] >= '0' && s[u] <= '9') ||
			  s[u] == '_'))
			break;
	}

	if (s[u])
		return FALSE;

	return TRUE;
}

/**
 * Get a connection to DBUS.
 * 
 * @param err
 * 		return location for a GError, or NULL
 * 
 * @return
 * 		the connection or NULL if an error occured (in that case *err will
 *      be non-NULL)
 */
DBusGConnection*
rem_dbus_connect(GError **err)
{
	DBusGConnection *conn;
	
	conn = dbus_g_bus_get(DBUS_BUS_SESSION, err);

	g_assert(conn || !err || *err);
	
	return conn;
}

/**
 * Register a GObject as a DBUS service.
 * 
 * @param conn
 * 		a DBUS connection (see rem_dbus_connect())
 * @param object_info
 *		the object's DBUS GLib object info (can be found in the object's DBUS
 * 		server mode glue)
 * @param object_type
 * 		the object's type (get it via <OBJECT>_get_type())
 * @param object
 * 		the object
 * @param name
 * 		the service name (will be used as net.sf.remuco.<NAME>)
 * @param err
 * 		return location for a GError, or NULL
 */
void
rem_dbus_register(DBusGConnection *conn, const DBusGObjectInfo *object_info,
				  GType object_type, gpointer object, const gchar *name,
				  GError **err)
{
	DBusGProxy		*proxy;
	guint			ret;
	gchar			*path, *iface;

	// install introspection info for 'object'
	dbus_g_object_type_install_info(object_type, object_info);
	
	path = g_strdup_printf("%s/%s", REM_DBUS_NAMESPACE_SLASHED, name);
	
	// register dbus path
	dbus_g_connection_register_g_object(conn, path, G_OBJECT(object));
	
	g_free(path);

	// register service name (constants defined in dbus-glib-bindings.h)
	proxy = dbus_g_proxy_new_for_name(conn,
									  DBUS_SERVICE_DBUS,
									  DBUS_PATH_DBUS,
									  DBUS_INTERFACE_DBUS);

	iface = g_strdup_printf("%s.%s", REM_DBUS_NAMESPACE_DOTTED, name);

	err = NULL;
	org_freedesktop_DBus_request_name(proxy, iface, 0, &ret, err);
	
	g_free(iface);

	g_object_unref(proxy);
}

/**
 * Get a proxy for a Remuco DBUS service.
 * 
 * @param conn
 * 		a DBUS connection (see rem_dbus_connect())
 * @param name
 * 		the service name (will be used as net.sf.remuco.<NAME>)
 * 
 * @return
 * 		the proxy (not NULL)
 */
DBusGProxy*
rem_dbus_proxy(DBusGConnection *conn, const gchar *name)
{
	DBusGProxy		*proxy;
	gchar			*service, *path, *iface;
	
	service = g_strdup_printf("%s.%s", REM_DBUS_NAMESPACE_DOTTED, name);
	path = g_strdup_printf("%s/%s", REM_DBUS_NAMESPACE_SLASHED, name);
	iface = g_strdup_printf("%s.%s", REM_DBUS_NAMESPACE_DOTTED, name);
	
	proxy = dbus_g_proxy_new_for_name(conn, service, path, iface);
	
	g_free(service);
	g_free(path);
	g_free(iface);
	
	return proxy;
}

