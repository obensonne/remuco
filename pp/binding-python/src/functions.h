#ifndef REMPY_FUNCTIONS_H_
#define REMPY_FUNCTIONS_H_

#include <Python.h>

///////////////////////////////////////////////////////////////////////////////
//
// server interface wrapper functions
//
///////////////////////////////////////////////////////////////////////////////

PyObject*
rempy_server_up(PyObject *self, PyObject *args);

PyObject*
rempy_server_down(PyObject *self, PyObject *args);

PyObject*
rempy_server_notify(PyObject *self, PyObject *args);

PyObject*
rempy_server_poll(PyObject *self, PyObject *args);

///////////////////////////////////////////////////////////////////////////////
//
// functions to integrate python pp logging into server log
//
///////////////////////////////////////////////////////////////////////////////

PyObject*
rempy_log_noise(PyObject *self, PyObject *args);

PyObject*
rempy_log_debug(PyObject *self, PyObject *args);

PyObject*
rempy_log_info(PyObject *self, PyObject *args);

PyObject*
rempy_log_warn(PyObject *self, PyObject *args);

PyObject*
rempy_log_error(PyObject *self, PyObject *args);

#endif /*REMPY_FUNCTIONS_H_*/
