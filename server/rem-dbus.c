#include "rem-dbus.h"
#include "rem-log.h"

#define REM_DBUS_IFACE_INTROSPECT	"org.freedesktop.DBus.Introspectable"
#define REM_DBUS_MAX_PROXIES		20
static DBusGConnection	*dbc;

static DBusGProxy	*dbp_a[REM_DBUS_MAX_PROXIES];
static DBusGProxy	*dbp_a_num;

int
rem_dbus_connect(void)
{
	GError *g_err;
	
	g_type_init();

	g_err = NULL;
	dbc = dbus_g_bus_get(DBUS_BUS_SESSION, &g_err);
	if (!dbc) {
		LOG_ERROR("connecting to dbus failed: %s\n", g_err->message);
		g_error_free(g_err);
		return -1;
	}
	return 0;
}

int
rem_dbus_get_proxy(struct rem_dbus_proxy *dbp)
{
	GError *g_err;
	DBusGProxy *dbp_intro;
	gboolean ret;
	
	if (dbp_a_num == REM_DBUS_MAX_PROXIES) {
		LOG_ERROR("too much dbus proxies");
		return -1;
	}

	dbp_intro = dbus_g_proxy_new_for_name(g_dbus_conn, dbp->service,
				dbp->path, REM_DBUS_IFACE_INTROSPECT);
	if (!dbp_intro) {
		LOG_ERROR("getting introspection proxy failed\n");
		return -1;
	}
			
	g_err = NULL;
	char *str = NULL;
	ret = dbus_g_proxy_call(dbp_intro, "Introspect", &g_err, G_TYPE_INVALID,
					G_TYPE_STRING, &str, G_TYPE_INVALID);
	REM_DBUS_CHECK_RET(ret, g_err);
	g_free(dbp_intro);	// TODO is that ok?
	if (!ret) {
		return -1;
	}

	// Create a proxy object for the "bus driver"
	dbp->dbp = dbus_g_proxy_new_for_name(g_dbus_conn, dbp->service,
							dbp->path, dbp->iface);

	dbp_a[dbp_a_num] = dbp->dbp;
	dbp_a_num++;

	return 0;
}

void
rem_dbus_disconnect(void)
{
	int i;
	for (i = 0; i < dbp_a_num; i++) {
		if (dbp_a[i]) {
			g_free(dbp_a[i]);	// TODO is that ok?
			dbp_a[i] = NULL;
		}
	}
	dbp_a_num = 0;
	
	dbus_g_connection_flush(dbc);
	dbus_g_connection_unref(dbc);
	
	// TODO is this all to close a connection ?
}
