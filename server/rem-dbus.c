/*
 * Copyright (C) 2007 Christian Buennig - See COPYING
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include "rem-dbus.h"
#include "rem-log.h"

///////////////////////////////////////////////////////////////////////////////
//
// macros and constants
//
///////////////////////////////////////////////////////////////////////////////

#define REM_DBUS_IFACE_INTROSPECT	"org.freedesktop.DBus.Introspectable"
#define REM_DBUS_MAX_PROXIES		20

static int
rem_dbus_introspect(struct rem_dbus_proxy *dbp);

///////////////////////////////////////////////////////////////////////////////
//
// global vars
//
///////////////////////////////////////////////////////////////////////////////

static DBusGConnection	*dbc;

// remember the proxies to free them when disconnectin from dbus
static DBusGProxy	*dbp_a[REM_DBUS_MAX_PROXIES];
static unsigned int	dbp_a_num;

///////////////////////////////////////////////////////////////////////////////
//
// public functions
//
///////////////////////////////////////////////////////////////////////////////

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
	LOG_DEBUG("connected to dbus\n");
	return 0;
}

int
rem_dbus_get_proxy(struct rem_dbus_proxy *dbp)
{
	int ret;
	
	if (dbp_a_num == REM_DBUS_MAX_PROXIES) {
		LOG_ERROR("too much dbus proxies");
		return -1;
	}

	ret = rem_dbus_introspect(dbp);
	if (ret < 0)
		return -1;

	// Create a proxy object for the "bus driver"
	dbp->dbp = dbus_g_proxy_new_for_name(dbc, dbp->service,
							dbp->path, dbp->iface);
	if (!dbp->dbp) {
		LOG_ERROR("getting proxy for %s failed\n", dbp->iface);
		return -1;
	}

	LOG_DEBUG("got proxy for %s\n", dbp->iface);

	dbp_a[dbp_a_num] = dbp->dbp;
	dbp_a_num++;

	return 0;
}

void
rem_dbus_disconnect(void)
{
	unsigned int i;
	for (i = 0; i < dbp_a_num; i++) {
		if (dbp_a[i]) {
			g_object_unref(dbp_a[i]);	// TODO is that ok?
			dbp_a[i] = NULL;
		}
	}
	dbp_a_num = 0;
	
	dbus_g_connection_flush(dbc);
	dbus_g_connection_unref(dbc);
	
	// TODO is this all to close a connection ?
}

///////////////////////////////////////////////////////////////////////////////
//
// private functions
//
///////////////////////////////////////////////////////////////////////////////

static int
rem_dbus_introspect(struct rem_dbus_proxy *dbp)
{
	GError		*g_err;
	DBusGProxy	*dbp_intro;
	gboolean	ret;
	char		*str;
	
	LOG_DEBUG("introspecting %s ..\n", dbp->iface);

	// get an introspection proxy 
	dbp_intro = dbus_g_proxy_new_for_name(dbc, dbp->service,
				dbp->path, REM_DBUS_IFACE_INTROSPECT);
	if (!dbp_intro) {
		LOG_ERROR("getting introspection proxy for %s failed\n",
								dbp->iface);
		return -1;
	}
			
	// call method "Intropsect"
	g_err = NULL;
	str = NULL;
	ret = dbus_g_proxy_call(dbp_intro, "Introspect",
					&g_err, G_TYPE_INVALID,
					G_TYPE_STRING, &str, G_TYPE_INVALID);
	REM_DBUS_CHECK_RET(ret, g_err);

	if (str) g_free(str);		// free introspection result
	g_object_unref(dbp_intro);	// unref the proxy

	if (!ret) {
		return -1;
		LOG_ERROR("introspection for %s failed\n", dbp->iface);
	}
	LOG_DEBUG("introspection ok\n");
	
	return 0;
}
