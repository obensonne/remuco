#include <Python.h>
#include <remuco/log.h>

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
	{"poll",  rempy_server_poll, METH_VARARGS,
		"Instructs the server to periodically poll the PP for changes. "
		"Params: (Object (server private data)). "
		"For more information see rem_server_poll() in the C API documentaion."},
	{"log_noise", rempy_log_noise, METH_VARARGS,
		"Log a noisy message within the Remuco server log system. "
		"Note: This function only has an effect if the server library as well "
		"as the python binding has been compiled with -DDO_LOG_NOISE (which ."
		"is probably not the case if you fetched a binary versions of them)."
		"Params: (String (msg))."},
	{"log_debug", rempy_log_debug, METH_VARARGS,
		"Log a debug message within the Remuco server log system. "
		"Params: (String (msg))."},
	{"log_info", rempy_log_info, METH_VARARGS,
		"Log an info message within the Remuco server log system. "
		"Params: (String (msg))."},
	{"log_warn", rempy_log_warn, METH_VARARGS,
		"Log a warning message within the Remuco server log system. "
		"Params: (String (msg))."},
	{"log_error", rempy_log_error, METH_VARARGS,
		"Log an error message within the Remuco server log system. "
		"Params: (String (msg))."},
	{NULL, NULL, 0, NULL}		/* Sentinel */
};

#ifndef PyMODINIT_FUNC	/* declarations for DLL import/export */
#define PyMODINIT_FUNC void
#endif
PyMODINIT_FUNC
initremuco(void) 
{
	PyObject* m;
	
	rem_log_init(REM_LL_DEBUG);
	
	if (rempy_ppdesc_init() < 0) return;
	if (rempy_ppcb_init() < 0) return;
	if (rempy_pstatus_init() < 0) return;

	m = Py_InitModule3("remuco", remuco_methods,
					   "Python binding to the Remuco server library.");

	rempy_ppdesc_add(m);
	rempy_ppcb_add(m);
	rempy_pstatus_add(m);
	rempy_constants_add(m);
}
