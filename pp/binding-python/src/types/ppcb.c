#include "ppcb.h"

#include <glib.h>

static PyMemberDef PPCallbacks_members[] = {
	{"snychronize", T_OBJECT_EX, offsetof(RemPyPPCallbacks, synchronize), 0,
	 "Server requests to snychronizes a player status. "
	 "Params: (Object (PP private data), PlayerStatus (in/out param)). "
	 "Returns: None."},
	{"get_plob", T_OBJECT_EX, offsetof(RemPyPPCallbacks, get_plob), 0,
     "Server requests a plob. "
	 "Params: (Object (PP private data), String (PID of the requested plob)). "
	 "Retruns: Dict (plob meta information)."},
	{"get_library", T_OBJECT_EX, offsetof(RemPyPPCallbacks, get_library), 0,
	 "Server requests the library. "
	 "Params: (Object (PP private data)). "
	 "Returns: List (containing PLIDs as strings)."},
	{"get_ploblist", T_OBJECT_EX, offsetof(RemPyPPCallbacks, get_ploblist), 0,
	 "Server requests a ploblist."
	 "Params: (Object (PP private data), String (PLID)). "
	 "Returns: List (containing PIDs as strings)."},
	{"notify", T_OBJECT_EX, offsetof(RemPyPPCallbacks, notify), 0,
	 "Server notifies that an event occured. "
	 "Params: (Object (PP private data), int (event code)). "
	 "Returns: None."},
	{"play_ploblist", T_OBJECT_EX, offsetof(RemPyPPCallbacks, play_ploblist), 0,
	 "Server requests to play a ploblist (resp. to fill the playlist with its "
	 "content). "
	 "Params: (Object (PP private data), String (PLID)). "
	 "Returns: None."},
	{"search", T_OBJECT_EX, offsetof(RemPyPPCallbacks, search), 0,
	 "Server requests to return a list with the PIDs of all plobs, that have "
	 "equal meta information to the given plob. "
	 "Params: (Object (PP private data), Dict (plob meta information)). "
	 "Returns: List (containing PIDs as strings)."
	 " Note: FUTURE FEATURE!"},
	{"simple_control", T_OBJECT_EX, offsetof(RemPyPPCallbacks, simple_control), 0,
	 "Server requests the PP do control the player. "
	 "Params: (Object (PP private data), int (command), int (param)). "
	 "Returns: None."},
	{"update_plob", T_OBJECT_EX, offsetof(RemPyPPCallbacks, update_plob), 0,
	 "RemLib requests the PP to update the the meta information of a plob "
	 "(which has been changed on client side). "
	 "Params: (Object (PP private data), String (PID of the plob to change), "
	 "Dict (new plob meta information)). "
	 "Returns: None."
	 " Note: FUTURE FEATURE!"},
	{"update_ploblist", T_OBJECT_EX, offsetof(RemPyPPCallbacks, update_ploblist), 0,
	 "RemLib requests the PP to update the contents of a ploblist. "
	 "Params: (Object (PP private data), String (PLID of the list to change), "
	 ", List (containing PIDs as strings - the new content of the list)). "
	 "Returns: None."
	 " Note: FUTURE FEATURE!"},
    {NULL}  /* Sentinel */
};

#define REMPY_INIT_CBF(_cbf) G_STMT_START {	\
	Py_CLEAR(self->_cbf);					\
	Py_INCREF(Py_None);						\
    self->_cbf = Py_None;					\
} G_STMT_END

static int
PPCallbacks_init(RemPyPPCallbacks *self, PyObject *args, PyObject *kwds)
{
    
	REMPY_INIT_CBF(synchronize);
	REMPY_INIT_CBF(get_plob);
	REMPY_INIT_CBF(get_library);
	REMPY_INIT_CBF(get_ploblist);
	REMPY_INIT_CBF(notify);
	REMPY_INIT_CBF(play_ploblist);
	REMPY_INIT_CBF(search);
	REMPY_INIT_CBF(simple_control);
	REMPY_INIT_CBF(update_plob);
	REMPY_INIT_CBF(update_ploblist);
	
    return 0;
}

static PyObject*
PPCallbacks_new(PyTypeObject *type, PyObject *args, PyObject *kwds)
{
	RemPyPPCallbacks *self;

    self = (RemPyPPCallbacks *)type->tp_alloc(type, 0);
    if (self != NULL) {
    	REMPY_INIT_CBF(synchronize);
    	REMPY_INIT_CBF(get_plob);
    	REMPY_INIT_CBF(get_library);
    	REMPY_INIT_CBF(get_ploblist);
    	REMPY_INIT_CBF(notify);
    	REMPY_INIT_CBF(play_ploblist);
    	REMPY_INIT_CBF(search);
    	REMPY_INIT_CBF(simple_control);
    	REMPY_INIT_CBF(update_plob);
    	REMPY_INIT_CBF(update_ploblist);
    } else {
    	return NULL;
    }

    return (PyObject *)self;
}

static void
PPCallbacks_dealloc(RemPyPPCallbacks* self)
{
	Py_CLEAR(self->synchronize);
	Py_CLEAR(self->get_plob);
	Py_CLEAR(self->get_library);
	Py_CLEAR(self->get_ploblist);
	Py_CLEAR(self->notify);
	Py_CLEAR(self->play_ploblist);
	Py_CLEAR(self->search);
	Py_CLEAR(self->simple_control);
	Py_CLEAR(self->update_plob);
	Py_CLEAR(self->update_ploblist);
    self->ob_type->tp_free((PyObject*)self);
}

static PyTypeObject PPCallbacksType = {
	PyObject_HEAD_INIT(NULL)
	0,									/*ob_size*/
	"remuco.PPCallbacks",				/*tp_name*/
	sizeof(RemPyPPCallbacks),				/*tp_basicsize*/
	0,									/*tp_itemsize*/
	(destructor)PPCallbacks_dealloc,	/*tp_dealloc*/
	0,									/*tp_print*/
	0,									/*tp_getattr*/
	0,									/*tp_setattr*/
	0,									/*tp_compare*/
	0,									/*tp_repr*/
	0,									/*tp_as_number*/
	0,									/*tp_as_sequence*/
	0,									/*tp_as_mapping*/
	0,									/*tp_hash */
	0,									/*tp_call*/
	0,									/*tp_str*/
	0,									/*tp_getattro*/
	0,									/*tp_setattro*/
	0,									/*tp_as_buffer*/
	Py_TPFLAGS_DEFAULT,					/*tp_flags*/
	"Player Proxy Callbacks. For details see description of RemPPDescriptor in C API documentaion.",	/* tp_doc */
    0,									/* tp_traverse */
    0,									/* tp_clear */
    0,									/* tp_richcompare */
    0,									/* tp_weaklistoffset */
    0,									/* tp_iter */
    0,									/* tp_iternext */
    0,									/* tp_methods */
    PPCallbacks_members,             	/* tp_members */
    0,									/* tp_getset */
    0,									/* tp_base */
    0,									/* tp_dict */
    0,									/* tp_descr_get */
    0,									/* tp_descr_set */
    0,									/* tp_dictoffset */
    (initproc)PPCallbacks_init,			/* tp_init */
    0,									/* tp_alloc */
    PPCallbacks_new,					/* tp_new */
};

int
rempy_ppcb_init(void)
{
	return PyType_Ready(&PPCallbacksType);
}

void
rempy_ppcb_add(PyObject *module)
{
	Py_INCREF(&PPCallbacksType);
	PyModule_AddObject(module, "PPCallbacks", (PyObject *)&PPCallbacksType);
}

int
rempy_ppcb_check_type(PyObject *po)
{
	return PyObject_TypeCheck(po, &PPCallbacksType);
}
