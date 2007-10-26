#include "pstatus.h"

static PyMemberDef PlayerStatus_members[] = {
	{"state", T_INT, offsetof(RemPyPlayerStatus, state), 0,
			"See also description of RemPlayerStatus in C API documentaion."},
	{"volume", T_INT, offsetof(RemPyPlayerStatus, volume), 0,
			"See also description of RemPlayerStatus in C API documentaion."},
	{"repeat", T_INT, offsetof(RemPyPlayerStatus, repeat), 0,
			"See also description of RemPlayerStatus in C API documentaion."},
	{"shuffle", T_INT, offsetof(RemPyPlayerStatus, shuffle), 0,
			"See also description of RemPlayerStatus in C API documentaion."},
	{"cap_pos", T_INT, offsetof(RemPyPlayerStatus, cap_pos), 0,
			"See also description of RemPlayerStatus in C API documentaion."},
	{"cap_pid", T_OBJECT_EX, offsetof(RemPyPlayerStatus, cap_pid), 0,
			"The PID (as a string) of the currently active plob. Set to None "
			"if there is no currently active plob. "
			"See also description of RemPlayerStatus in C API documentaion."},
	{"playlist", T_OBJECT_EX, offsetof(RemPyPlayerStatus, playlist), 0,
			"A list of all PIDs (as strings) of the plobs in the (current) playlist. "
			"See also description of RemPlayerStatus in C API documentaion."},
	{"queue", T_OBJECT_EX, offsetof(RemPyPlayerStatus, queue), 0,
			"A list of all PIDs (as strings) of the plobs in the queue. "
			"See also description of RemPlayerStatus in C API documentaion."},
	{NULL}  /* Sentinel */
};

static int
PlayerStatus_init(RemPyPlayerStatus *self, PyObject *args, PyObject *kwds)
{
    Py_CLEAR(self->cap_pid);
    Py_CLEAR(self->playlist);
    Py_CLEAR(self->queue);

	Py_INCREF(Py_None);
    self->cap_pid = Py_None;
    self->playlist = PyList_New(0);
    self->queue = PyList_New(0);
    
    return 0;
}

static PyObject*
PlayerStatus_new(PyTypeObject *type, PyObject *args, PyObject *kwds)
{
	RemPyPlayerStatus *self;

    self = (RemPyPlayerStatus *)type->tp_alloc(type, 0);
    if (self != NULL) {
    	Py_INCREF(Py_None);
        self->cap_pid = Py_None;
        self->playlist = PyList_New(0);
        self->queue = PyList_New(0);
    } else {
    	return NULL;
    }

    return (PyObject *)self;
}

static void
PlayerStatus_dealloc(RemPyPlayerStatus* self)
{
    Py_CLEAR(self->cap_pid);
    Py_CLEAR(self->playlist);
    Py_CLEAR(self->queue);
    self->ob_type->tp_free((PyObject*)self);
}

static PyTypeObject PlayerStatusType = {
	PyObject_HEAD_INIT(NULL)
	0,									/*ob_size*/
	"remuco.PlayerStatus",				/*tp_name*/
	sizeof(RemPyPlayerStatus),				/*tp_basicsize*/
	0,									/*tp_itemsize*/
	(destructor)PlayerStatus_dealloc,	/*tp_dealloc*/
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
	"Player Status. For details see description of RemPlayerStatus in C API documentaion.",	/* tp_doc */
    0,									/* tp_traverse */
    0,									/* tp_clear */
    0,									/* tp_richcompare */
    0,									/* tp_weaklistoffset */
    0,									/* tp_iter */
    0,									/* tp_iternext */
    0,									/* tp_methods */
    PlayerStatus_members,				/* tp_members */
    0,									/* tp_getset */
    0,									/* tp_base */
    0,									/* tp_dict */
    0,									/* tp_descr_get */
    0,									/* tp_descr_set */
    0,									/* tp_dictoffset */
    (initproc)PlayerStatus_init,		/* tp_init */
    0,									/* tp_alloc */
    PlayerStatus_new,					/* tp_new */
};

RemPyPlayerStatus*
rempy_pstatus_new()
{
	return PyObject_New(RemPyPlayerStatus, &PlayerStatusType);
}

int
rempy_pstatus_init(void)
{
	return PyType_Ready(&PlayerStatusType);
}

void
rempy_pstatus_add(PyObject *module)
{
	Py_INCREF(&PlayerStatusType);
	PyModule_AddObject(module, "PlayerStatus", (PyObject *)&PlayerStatusType);
}

int
rempy_pstatus_check_type(PyObject *po)
{
	return PyObject_TypeCheck(po, &PlayerStatusType);
}
