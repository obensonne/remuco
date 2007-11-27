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

#define REMPY_API_BUG(x, args...)											\
	g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_CRITICAL,								\
	"\n"																	\
	"*************************************************************\n"		\
	"** BAD API USAGE ** \n"												\
	"** Detected in %s (%s): " x "\n"										\
	"** This is probably a bug in a player proxy that used the\n"			\
	"** Remuco API with invalid parameters or in an invalid state.\n"		\
	"*************************************************************",		\
	G_STRLOC, G_STRFUNC, ##args);

#define rempy_bapiu(_msg, _abort) G_STMT_START {		\
		if (PyErr_Occurred()) PyErr_Print();			\
		if (_abort) {									\
			REMPY_API_BUG(_msg);						\
			g_log(REM_LOG_DOMAIN, G_LOG_LEVEL_ERROR,	\
				"** Aborting now ...\n");				\
		} else {										\
			REMPY_API_BUG(_msg);						\
			PyErr_SetString(PyExc_TypeError, _msg);		\
		}												\
} G_STMT_END

#define rempy_bapiu_assert(_expr, _msg)	\
		if G_UNLIKELY(!(_expr)) rempy_bapiu(_msg, TRUE)

#endif /*REMPY_UTIL_H_*/
