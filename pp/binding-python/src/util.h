#ifndef REMPY_UTIL_H_
#define REMPY_UTIL_H_

#include <Python.h>
#include <glib.h>

/*
#define rempy_assert(_expr, _msg) G_STMT_START {	\
	if G_UNLIKELY(!(_expr)) {						\
		if (PyErr_Occurred()) PyErr_Print();		\
		PyErr_Clear();								\
		PyErr_SetString(PyExc_RuntimeError, _msg);	\
		PyErr_Print();								\
		LOG_BUG("%s", _msg);						\
		g_assert_not_reached();						\
	}												\
} G_STMT_END
*/

#define REMPY_LOG_API_BUG(x, args...)											\
	g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_CRITICAL,								\
	"\n"																	\
	"*************************************************************\n"		\
	"** BAD API USAGE ** \n"												\
	"** Detected in %s (%s): " x "\n"										\
	"** This is probably a bug in a player proxy that used the\n"			\
	"** Remuco API with invalid parameters or in an invalid state.\n"		\
	"*************************************************************",		\
	G_STRLOC, G_STRFUNC, ##args);

#define rempy_api_warn(_msg) G_STMT_START {				\
		if (PyErr_Occurred()) PyErr_Print();			\
		REMPY_LOG_API_BUG(_msg);						\
		PyErr_SetString(PyExc_TypeError, _msg);			\
} G_STMT_END

#define rempy_api_check(_expr, _msg) G_STMT_START {		\
		if G_UNLIKELY(!(_expr)) {						\
			rempy_api_warn(_msg);						\
			g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_ERROR,	\
				"** Aborting now ...\n");				\
		}												\
} G_STMT_END

#endif /*REMPY_UTIL_H_*/
