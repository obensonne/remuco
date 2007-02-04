#ifndef REMDBUS_H_
#define REMDBUS_H_

#include <dbus/dbus-glib.h>

/**
 * Checks if calling a method via dbus failed. Prints out the error if there
 * was one. The Gerror (_gerr) gets freed automatically.
 * 
 * @param _ret (gboolean)
 * 	return of dbus method call
 * @param _gerr (*GError)
 * 	error of method call
 */
#define REM_DBUS_CHECK_RET(_ret, _gerr) if (!_ret) {			\
	if (_gerr->domain == DBUS_GERROR &&				\
		_gerr->code == DBUS_GERROR_REMOTE_EXCEPTION) {		\
		LOG_ERROR("dbus remote method exception %s: %s\n",	\
			dbus_g_error_get_name(_gerr), _gerr->message);	\
	} else {							\
		LOG_ERROR("dbus error: %s\n", _gerr->message);		\
	}								\
	g_error_free (_gerr);						\
}


struct rem_dbus_proxy {
	DBusGProxy		*dbp;
	char			*service, *path, *iface;
};

int
rem_dbus_connect(void);

void
rem_dbus_disconnect(void);

int
rem_dbus_get_proxy(struct rem_dbus_proxy *dbp);


#endif /*REMDBUS_H_*/
