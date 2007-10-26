#ifndef REMPY_PPCB_H_
#define REMPY_PPCB_H_

#include <Python.h>
#include <structmember.h>

typedef struct {
	PyObject_HEAD
	PyObject	*synchronize;
	PyObject	*get_plob;
	PyObject	*get_library;
	PyObject	*get_ploblist;
	PyObject	*notify_error;
	PyObject	*play_ploblist;
	PyObject	*search;
	PyObject	*simple_control;
	PyObject	*update_plob;
	PyObject	*update_ploblist;
} RemPyPPCallbacks;

int
rempy_ppcb_init(void);

void
rempy_ppcb_add(PyObject *module);

int
rempy_ppcb_check_type(PyObject *po);

#endif /*REMPY_PPCB_H_*/
