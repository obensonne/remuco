#include <Python.h>

#include "types.h"
#include "functions.h"
#include "constants.h"

static PyMethodDef remuco_methods[] = {
	{"up",  rempy_server_up, METH_VARARGS,
		"Starts a Remuco server. "
		"Params: (PPDescriptor, PPCallbacks, Object (PP private data)). "
		"Returns: (Object (server private data - must be used as first "
		"parameter when calling the other server interface functions)). "
		"For more information see rem_server_up() in the C API documentaion."},
	{"down",  rempy_server_down, METH_VARARGS,
		"Shuts down a Remuco server. "
		"Params: (Object (server private data)). "
	   	"For more information see rem_server_down() in the C API documentaion."},
	{"notify",  rempy_server_notify, METH_VARARGS,
		"Notifies the Remuco server about a change in player status. "
		"Params: (Object (server private data)). "
		"For more information see rem_server_notify() in the C API documentaion."},
	{"poll",  rempy_server_notify, METH_VARARGS,
		"Instructs the server to periodically poll the PP for changes. "
		"Params: (Object (server private data)). "
		"For more information see rem_server_poll() in the C API documentaion."},
	{NULL, NULL, 0, NULL}		/* Sentinel */
};

#ifndef PyMODINIT_FUNC	/* declarations for DLL import/export */
#define PyMODINIT_FUNC void
#endif
PyMODINIT_FUNC
initremuco(void) 
{
	PyObject* m;
	
	if (rempy_ppdesc_init() < 0) return;
	if (rempy_ppcb_init() < 0) return;
	if (rempy_pstatus_init() < 0) return;

	m = Py_InitModule3("remuco", remuco_methods,
					   "Python binding to the Remuco library.");

	rempy_ppdesc_add(m);
	rempy_ppcb_add(m);
	rempy_pstatus_add(m);
	rempy_constants_add(m);
}
