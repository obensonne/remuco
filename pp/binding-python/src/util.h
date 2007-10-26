#ifndef REMPY_UTIL_H_
#define REMPY_UTIL_H_

#include <Python.h>
#include <glib.h>

#define rempy_assert(_expr, _msg) G_STMT_START {	\
	if G_UNLIKELY(!(_expr)) {						\
		if (PyErr_Occurred()) PyErr_Print();		\
		PyErr_Clear();								\
		PyErr_SetString(PyExc_RuntimeError, _msg);	\
		PyErr_Print();								\
		g_assert_not_reached();						\
	}												\
} G_STMT_END

#endif /*REMPY_UTIL_H_*/
