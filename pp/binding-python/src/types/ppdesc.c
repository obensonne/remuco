#include "ppdesc.h"


static PyMemberDef PPDescriptor_members[] = {
    {"player_name", T_OBJECT_EX, offsetof(RemPyPPDescriptor, player_name), 0,
			"See description of RemPPDescriptor in C API documentaion."},
    {"charset", T_OBJECT_EX, offsetof(RemPyPPDescriptor, charset), 0,
			"See description of RemPPDescriptor in C API documentaion."},
	{"max_rating_value", T_INT, offsetof(RemPyPPDescriptor, max_rating_value), 0,
			"See description of RemPPDescriptor in C API documentaion."},
	{"supports_seek", T_INT, offsetof(RemPyPPDescriptor, supports_seek), 0,
			"See description of RemPPDescriptor in C API documentaion."},
	{"supports_playlist", T_INT, offsetof(RemPyPPDescriptor, supports_playlist), 0,
			"See description of RemPPDescriptor in C API documentaion."},
	{"supports_playlist_jump", T_INT, offsetof(RemPyPPDescriptor, supports_playlist_jump), 0,
			"See description of RemPPDescriptor in C API documentaion."},
	{"supports_queue", T_INT, offsetof(RemPyPPDescriptor, supports_queue), 0,
			"See description of RemPPDescriptor in C API documentaion."},
	{"supports_queue_jump", T_INT, offsetof(RemPyPPDescriptor, supports_queue_jump), 0,
			"See description of RemPPDescriptor in C API documentaion."},
	{"supports_tags", T_INT, offsetof(RemPyPPDescriptor, supports_tags), 0,
			"See description of RemPPDescriptor in C API documentaion."},
	{"supported_repeat_modes", T_INT, offsetof(RemPyPPDescriptor, supported_repeat_modes), 0,
			"See description of RemPPDescriptor in C API documentaion."},
	{"supported_shuffle_modes", T_INT, offsetof(RemPyPPDescriptor, supported_shuffle_modes), 0,
			"See description of RemPPDescriptor in C API documentaion."},
    {NULL}  /* Sentinel */
};


static int
PPDescriptor_init(RemPyPPDescriptor *self, PyObject *args, PyObject *kwds)
{
    Py_CLEAR(self->charset);
	Py_INCREF(Py_None);
    self->charset = Py_None;
    
    Py_CLEAR(self->player_name);
	Py_INCREF(Py_None);
    self->player_name = Py_None;
    
    return 0;
}

static PyObject*
PPDescriptor_new(PyTypeObject *type, PyObject *args, PyObject *kwds)
{
	RemPyPPDescriptor *self;

    self = (RemPyPPDescriptor *)type->tp_alloc(type, 0);
    if (self != NULL) {
    	Py_INCREF(Py_None);
        self->charset = Py_None;
    	Py_INCREF(Py_None);
        self->player_name = Py_None;
    } else {
    	return NULL;
    }

    return (PyObject *)self;
}

static void
PPDescriptor_dealloc(RemPyPPDescriptor* self)
{
	Py_CLEAR(self->charset);
	Py_CLEAR(self->player_name);
    self->ob_type->tp_free((PyObject*)self);
}

static PyTypeObject PPDescriptorType = {
	PyObject_HEAD_INIT(NULL)
	0,									/*ob_size*/
	"remuco.PPDescriptor",				/*tp_name*/
	sizeof(RemPyPPDescriptor),				/*tp_basicsize*/
	0,									/*tp_itemsize*/
	(destructor)PPDescriptor_dealloc,	/*tp_dealloc*/
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
	"Player Proxy Descriptor. For details see description of RemPPDescriptor in C API documentaion.",	/* tp_doc */
    0,									/* tp_traverse */
    0,									/* tp_clear */
    0,									/* tp_richcompare */
    0,									/* tp_weaklistoffset */
    0,									/* tp_iter */
    0,									/* tp_iternext */
    0,									/* tp_methods */
    PPDescriptor_members,				/* tp_members */
    0,									/* tp_getset */
    0,									/* tp_base */
    0,									/* tp_dict */
    0,									/* tp_descr_get */
    0,									/* tp_descr_set */
    0,									/* tp_dictoffset */
    (initproc)PPDescriptor_init,		/* tp_init */
    0,									/* tp_alloc */
    PPDescriptor_new,					/* tp_new */
};

int
rempy_ppdesc_init(void)
{
	return PyType_Ready(&PPDescriptorType);
}

void
rempy_ppdesc_add(PyObject *module)
{
	Py_INCREF(&PPDescriptorType);
	PyModule_AddObject(module, "PPDescriptor", (PyObject *)&PPDescriptorType);
}

int
rempy_ppdesc_check_type(PyObject *po)
{
	return PyObject_TypeCheck(po, &PPDescriptorType);
}

