#ifndef REMDBUS_H_
#define REMDBUS_H_

#include <dbus/dbus-glib.h>

/**
 * Checks if calling a method via dbus failed. Prints out the error if there
 * was one.
 * 
 * @param _ret (gboolean): return of dbus method call
 * @param _gerr (*GError): error of method call
 */
#define REM_DBUS_CHECK_RET(_ret, _gerr) do {				\
	if (!_ret) {							\
		if (_gerr->domain == DBUS_GERROR &&			\
			_gerr->code == DBUS_GERROR_REMOTE_EXCEPTION) {	\
			LOG_ERROR("dbus remote method exception %s: %s",\
				dbus_g_error_get_name(_gerr),		\
				_gerr->message);			\
		} else {						\
			LOG_ERROR("dbus error: %s\n", _gerr->message);	\
		}							\
		g_error_free (_gerr);					\
	}								\
} while(0)


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

#define REM_DBUS_CALL_NOREPLY(_dbp, _method, args..) do {	\
	dbus_g_proxy_call_no_reply(_dbp, _method, #args, G_TYPE_INVALID);
} while(0)

#endif /*REMDBUS_H_*/
