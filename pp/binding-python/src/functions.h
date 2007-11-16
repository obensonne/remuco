#ifndef REMPY_FUNCTIONS_H_
#define REMPY_FUNCTIONS_H_

#include <Python.h>

PyObject*
rempy_server_up(PyObject *self, PyObject *args);

PyObject*
rempy_server_down(PyObject *self, PyObject *args);

PyObject*
rempy_server_notify(PyObject *self, PyObject *args);

PyObject*
rempy_server_poll(PyObject *self, PyObject *args);

#endif /*REMPY_FUNCTIONS_H_*/
