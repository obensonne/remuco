#ifndef REMPY_PSTATUS_H_
#define REMPY_PSTATUS_H_

#include <Python.h>
#include <structmember.h>

typedef struct {
	PyObject_HEAD
	int			pbs;
	int			volume;
	int			flags;
	int			cap_pos;
	PyObject	*cap_pid;
	PyObject	*playlist;
	PyObject	*queue;
} RemPyPlayerStatus;


int
rempy_pstatus_init(void);

void
rempy_pstatus_add(PyObject *module);

RemPyPlayerStatus*
rempy_pstatus_new(void);

int
rempy_pstatus_check_type(PyObject *po);

#endif /*REMPY_PSTATUS_H_*/
