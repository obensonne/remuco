#include <Python.h>
#include <remuco.h>

///////////////////////////////////////////////////////////////////////////////
//
// macros and constants
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Lets _pp point to the data pointed to by the Python C object _po.
 * On error, return with exception (NULL) from the function where this
 * macro gets used.
 */
#define GET_PP_RETURN_ON_ERROR(_po, _pp) do {				\
	_pp = pp_conv_po2pp(_po);					\
	if (!pp) {							\
		if (!PyErr_Occurred())					\
			PyErr_SetString(PyExc_AttributeError,		\
						"malformed pp");	\
		return NULL;						\
	}								\
} while(0)

#define SETERR(_fs, args...) do {					\
	LOG_ERROR(_fs "\n", ##args);					\
	g_set_error(err, 0, 0, _fs, ##args);				\
} while(0)

#define PY_FUNC_CHECK(_fo, _fn, _ok)					\
	if (!PyCallable_Check(_fo)) {					\
		PyErr_Print();						\
		LOG_ERROR("cannot call python function %s\n", _fn);	\
		_ok = FALSE;						\
	}

#define PY_THREAD_JIBHER \
	PyGILState_STATE	_pGS; \
	_pGS = PyGILState_Ensure();

#define PY_THREAD_DAHASTE \
	PyGILState_Release(_pGS);

#define PY_CATCH_EXC			\
	if G_UNLIKELY(PyErr_Occurred()) {	\
		PyErr_Print();			\
		g_assert_not_reached();		\
	} 

#define MTN_IMG	"__img__"

///////////////////////////////////////////////////////////////////////////////
//
// types and structs
//
///////////////////////////////////////////////////////////////////////////////

/**
 * This struct represents the Python player proxy.
 */
typedef struct {
	PyObject		*name,		// name of the module to use
				*module;	// the module
	PyObject		*get_ps, *get_current_plob_pid, *get_plob,
				*get_ploblist, *get_library, *ctrl,
				*update_plob, *update_ploblist,
				*play_ploblist, *search, *error;	
	PyObject		*pp; // some private data used by Python PP
} pp_pymod_t;

/**
 * Data used by this wrapper player proxy.
 */
struct _rem_pp {
	rem_pinfo_t		pinfo;
	rem_ps_t		ps;
	const rem_server_t	*server;
	pp_pymod_t		py;
};



///////////////////////////////////////////////////////////////////////////////
//
// functions to convert data from C to Python and viceversa 
//
// These functions fail (glib assertion error) if something is wrong. Except:
// pp_conv_po2pp(), this conversion is done with a paramteter we got when python
// code called on of our functions. The prefered error behaviour is here to
// raise a python exception.

///////////////////////////////////////////////////////////////////////////////

static rem_pp_t*
pp_conv_po2pp(PyObject *po)
{
	rem_pp_t *pp;

	if (!po || !PyCObject_Check(po)) return NULL;
	
	pp = (rem_pp_t*) PyCObject_AsVoidPtr(po);
	
	if (!pp || !pp->pinfo.name) {
		LOG_ERROR("uninitialized pp structure\n");
		return NULL;
	}
	
	return pp;
}

static inline PyObject*
pp_conv_pp2po(rem_pp_t *pp)
{
	return PyCObject_FromVoidPtr(pp, NULL); 
}

static rem_sv_t*
pp_conv_list2sv(PyObject *pList)
{
	
	PyObject	*item;
	guint		u, len;
	gchar		*s;
	rem_sv_t	*sv = NULL;
	
	g_assert(pList);
	g_assert(PyList_Check(pList));
	
	sv = rem_sv_new();

	len = PyList_Size(pList);
	LOG_NOISE("list has %i elements\n", len);

	for (u = 0; u < len; u++) {
		
		item = PyList_GetItem(pList, u); // item is a borrowed ref
		
		g_assert(item);
		
		s = PyString_AsString(item);

		g_assert(s);		
		
		rem_sv_append(sv, g_strdup(s));
	}

	return sv;
}

static PyObject*
pp_conv_sv2list(const rem_sv_t *sv)
{
	PyObject	*pList, *pString;
	guint		u;
	
	pList = PyList_New(sv->l);
	
	for (u = 0; u < sv->l; ++u) {
		
		pString = PyString_FromString(sv->v[u]);
		
		PyList_Append(pList, pString);
		
	}
	
	return pList;
}

static rem_iv_t*
pp_conv_list2iv(PyObject *pList)
{
	
	PyObject	*item;
	guint		u, len;
	gint		i = 0;
	rem_iv_t	*iv = NULL;
	
	g_assert(pList);
	g_assert(PyList_Check(pList));

	iv = rem_iv_new();

	len = PyList_Size(pList);
	LOG_NOISE("list has %i elements\n", len);

	for (u = 0; u < len; u++) {
		
		item = PyList_GetItem(pList, u); // item is a borrowed ref
		
		g_assert(item);
		g_assert(PyInt_Check(item));
		
		i = (gint) PyInt_AsLong(item);
		
		rem_iv_append(iv, i);
	}

	return iv;
}

static inline rem_plob_t*
pp_conv_dict2plob(PyObject *pDict_meta, const gchar *pid)
{
	PyObject	*pList_keys, *pString_mtn, *pString_mtv;
	gchar		*mtn, *mtv;
	guint		u, len;
	rem_plob_t	*plob;

	
	g_assert(pDict_meta);
	g_assert(PyDict_Check(pDict_meta));

	plob = rem_plob_new(g_strdup(pid));
	
	pList_keys = PyDict_Keys(pDict_meta);
	len = PyList_Size(pList_keys);
	
	for (u = 0; u < len; u++) {
		
		pString_mtn = PyList_GetItem(pList_keys, u);
		pString_mtv = PyDict_GetItem(pDict_meta, pString_mtn);
		
		mtn = PyString_AsString(pString_mtn);
		
		g_assert(mtn);
		
		mtv = PyString_AsString(pString_mtv);
		
		if (G_UNLIKELY(!mtv)) continue;
		
		if G_UNLIKELY(g_str_equal(mtn, MTN_IMG)) {
			plob->img = g_strdup(mtv);
		} else {
			rem_plob_meta_add(plob, g_strdup(mtn), g_strdup(mtv));
		}
	}
	
	Py_DECREF(pList_keys);
	
	return plob;
}

static inline PyObject*
pp_conv_plob2dict(const rem_plob_t *plob)
{
	PyObject	*pDict_meta;
	guint		u, l;
	
	pDict_meta = PyDict_New();
	
	l = rem_plob_meta_num(plob);
	
	for (u = 0; u < l; ++u) {
		PyDict_SetItemString(
			pDict_meta,
			rem_plob_meta_get_mtn(plob, u),
			PyString_FromString(rem_plob_meta_get_mtv(plob, u)));
	}
	
	return pDict_meta;
	
}

///////////////////////////////////////////////////////////////////////////////
//
// player proxy interface (called by remuco server library)
//
///////////////////////////////////////////////////////////////////////////////

G_CONST_RETURN rem_ps_t*
pp_get_ps(rem_pp_t *pp)
{
	g_assert_debug(pp);

	PyObject	*pTuple_return;

	PY_THREAD_JIBHER;

	pTuple_return = PyObject_CallFunction(pp->py.get_ps,
				"(O)", pp->py.pp);
	PY_CATCH_EXC;
	g_assert_debug(pTuple_return);

	PyArg_ParseTuple(pTuple_return, "iiiii", &pp->ps.state, &pp->ps.volume,
		&pp->ps.pl_pos, &pp->ps.repeat_mode, &pp->ps.shuffle_mode);
	
	PY_CATCH_EXC;

	Py_DECREF(pTuple_return);
		
	PY_THREAD_DAHASTE;

	return &pp->ps;
}

gchar*
pp_get_current_plob_pid(rem_pp_t *pp)
{
	PyObject	*pString_pid;
	gchar		*pid;

	PY_THREAD_JIBHER;

	pString_pid = PyObject_CallFunction(pp->py.get_current_plob_pid,
				"(O)", pp->py.pp);
	PY_CATCH_EXC;
	g_assert_debug(pString_pid);

	if (pString_pid == Py_None) {
		pid = NULL;
	} else {
		pid = PyString_AsString(pString_pid);
		PY_CATCH_EXC;
	}

	if (pid) pid = g_strdup(pid);

	Py_DECREF(pString_pid);
	
	PY_THREAD_DAHASTE;

	return pid;
}

rem_plob_t*
pp_get_plob(rem_pp_t *pp, const gchar *pid)
{
	PyObject	*pDict_meta;
	rem_plob_t	*plob;

	PY_THREAD_JIBHER;

	pDict_meta = PyObject_CallFunction(pp->py.get_plob,
				"Os", pp->py.pp, pid);
	PY_CATCH_EXC;
	g_assert_debug(pDict_meta);

	plob = pp_conv_dict2plob(pDict_meta, pid);

	PY_THREAD_DAHASTE;

	return plob;
}

rem_sv_t*
pp_get_ploblist(rem_pp_t *pp, const gchar *plid)
{
	PyObject	*pList_pids;
	rem_sv_t	*pids;
	
	PY_THREAD_JIBHER;

	pList_pids = PyObject_CallFunction(pp->py.get_ploblist,
				"Os", pp->py.pp, plid);
	PY_CATCH_EXC;

	pids = pp_conv_list2sv(pList_pids);

	PY_THREAD_DAHASTE;

	return pids;
}

rem_library_t*
pp_get_library(rem_pp_t *pp)
{
	PyObject	*pList_plids, *pList_names, *pList_flags, *pTuple_return;
	rem_library_t	*pls;

	PY_THREAD_JIBHER;

	pTuple_return = PyObject_CallFunction(pp->py.get_library,
				"(O)", pp->py.pp);
	PY_CATCH_EXC;

	g_assert_debug(pTuple_return);

	PyArg_ParseTuple(pTuple_return, "OOO",
		&pList_plids, &pList_names, &pList_flags);
	PY_CATCH_EXC;

	pls = g_malloc0(sizeof(rem_library_t));
	
	pls->plids = pp_conv_list2sv(pList_plids);
	rem_sv_dump(pls->plids);
	pls->names = pp_conv_list2sv(pList_names);
	rem_sv_dump(pls->names);
	pls->flags = pp_conv_list2iv(pList_flags);
	
	Py_DECREF(pTuple_return);

	g_assert(pls->plids && pls->names && pls->flags);
	g_assert(pls->plids->l == pls->names->l);
	g_assert(pls->names->l == pls->flags->l); 
		
	PY_THREAD_DAHASTE;

	return pls;
}

// FUTURE FEATUE (not yet supported on client side)
rem_sv_t*
pp_search(rem_pp_t *pp, const rem_plob_t *plob)
{
	PyObject	*pDict_meta, *pList_pids;
	rem_sv_t	*pl;

	PY_THREAD_JIBHER;

	pDict_meta = pp_conv_plob2dict(plob);

	pList_pids = PyObject_CallFunction(pp->py.update_plob,
				"(OO)", pp->py.pp, pDict_meta);
	PY_CATCH_EXC;

	Py_DECREF(pDict_meta);	

	pl = pp_conv_list2sv(pList_pids);

	Py_DECREF(pList_pids);
	
	PY_THREAD_DAHASTE;

	return pl;
}

void
pp_play_ploblist(rem_pp_t *pp, const gchar *plid)
{
	PyObject	*pObject_return;

	PY_THREAD_JIBHER;

	pObject_return = PyObject_CallFunction(pp->py.play_ploblist,
				"(Os)", pp->py.pp, plid);
	PY_CATCH_EXC;
	g_assert_debug(pObject_return);
	g_assert(pObject_return == Py_None);

	Py_DECREF(pObject_return);	

	PY_THREAD_DAHASTE;
}

void
pp_update_plob(rem_pp_t *pp, const rem_plob_t *plob)
{
	PyObject	*pDict_meta, *pObject_return;

	PY_THREAD_JIBHER;

	pDict_meta = pp_conv_plob2dict(plob);

	pObject_return = PyObject_CallFunction(pp->py.update_plob,
				"(OsO)", pp->py.pp, plob->pid, pDict_meta);
	PY_CATCH_EXC;
	g_assert_debug(pObject_return);
	g_assert(pObject_return == Py_None);

	Py_DECREF(pDict_meta);

	Py_DECREF(pObject_return);

	PY_THREAD_DAHASTE;
}

// FUTURE FEATUE (not yet supported on client side)
void
pp_update_ploblist(rem_pp_t *pp, const gchar *plid, const rem_sv_t* pids)
{
       PyObject        *pList_pids, *pObject_return;

       PY_THREAD_JIBHER;

       pList_pids = pp_conv_sv2list(pids);

       pObject_return = PyObject_CallFunction(pp->py.update_ploblist,
                               "(OsO)", pp->py.pp, plid, pList_pids);
       PY_CATCH_EXC;
       g_assert_debug(pObject_return);
       g_assert(pObject_return == Py_None);

       Py_DECREF(pList_pids);

       Py_DECREF(pObject_return);
       
       PY_THREAD_DAHASTE;
}


void
pp_ctrl(rem_pp_t *pp, const rem_sctrl_t *sc)
{
	PyObject	*pObject_return;
	
	PY_THREAD_JIBHER;

	pObject_return = PyObject_CallFunction(pp->py.ctrl,
				"Oii", pp->py.pp, sc->code, sc->param);
	PY_CATCH_EXC;
	g_assert_debug(pObject_return);
	g_assert(pObject_return == Py_None);

	Py_DECREF(pObject_return);

	PY_THREAD_DAHASTE;
}

void
pp_error(rem_pp_t *pp, GError *err)
{
	PyObject	*pObject_return;
	
	PY_THREAD_JIBHER;

	pObject_return = PyObject_CallFunction(pp->py.error,
				"Os", pp->py.pp, err->message);
	PY_CATCH_EXC;
	g_assert_debug(pObject_return);
	g_assert(pObject_return == Py_None);

	Py_DECREF(pObject_return);

	PY_THREAD_DAHASTE;
}

///////////////////////////////////////////////////////////////////////////////
//
// Integrate Python module (the Python player proxy)
//
///////////////////////////////////////////////////////////////////////////////

static void
pp_py_module_down(rem_pp_t *pp)
{
	// Clean up
	if (pp->py.module)	{ Py_DECREF(pp->py.module); }
	if (pp->py.name)	{ Py_DECREF(pp->py.name); }
	
	if (pp->py.pp)		{ Py_DECREF(pp->py.pp); }
}

static void
pp_py_module_up(rem_pp_t* pp, const gchar *module, GError **err)
{
	g_assert_debug(pp && err && !*err);
	
	PyObject	*pDict;
	gboolean	ok;

	// Initialize the Python Interpreter
	//Py_Initialize();

	// Build the name object
	pp->py.name = PyString_FromString(module);
	
	LOG_INFO("loading module %s\n", module);

	// Load the module object
	pp->py.module = PyImport_Import(pp->py.name);

	if (!pp->py.module) {
		PyErr_Print();
		SETERR("could not load python module");
		pp_py_module_down(pp);
		return;
	}

	// pDict is a borrowed reference 
	pDict = PyModule_GetDict(pp->py.module);
	
	if (!pDict) {
		PyErr_Print();
		SETERR("could not get module dictionary");
		pp_py_module_down(pp);
		return;
	}
	
	// register and check the functions in the python module
	
	// pFunc_* are borrowed references 
	pp->py.get_ps = PyDict_GetItemString(pDict, "pp_get_ps");
	pp->py.get_current_plob_pid = PyDict_GetItemString(pDict, "pp_get_current_plob_pid");
	pp->py.get_plob = PyDict_GetItemString(pDict, "pp_get_plob");
	pp->py.get_ploblist = PyDict_GetItemString(pDict, "pp_get_ploblist");
	pp->py.get_library = PyDict_GetItemString(pDict, "pp_get_library");
	pp->py.ctrl = PyDict_GetItemString(pDict, "pp_ctrl");
	pp->py.update_plob = PyDict_GetItemString(pDict, "pp_update_plob");
	pp->py.update_ploblist = PyDict_GetItemString(pDict, "pp_update_ploblist"); // FUTURE FEATURE
	pp->py.play_ploblist = PyDict_GetItemString(pDict, "pp_play_ploblist");
	pp->py.search = PyDict_GetItemString(pDict, "pp_search"); // FUTURE FEATURE
	pp->py.error = PyDict_GetItemString(pDict, "pp_error");

	ok = TRUE;
	PY_FUNC_CHECK(pp->py.get_ps, "pp_get_ps", ok);
	PY_FUNC_CHECK(pp->py.get_current_plob_pid, "pp_get_current_plob_pid", ok);
	PY_FUNC_CHECK(pp->py.get_plob, "pp_get_plob", ok);
	PY_FUNC_CHECK(pp->py.get_ploblist, "pp_get_ploblist", ok);
	PY_FUNC_CHECK(pp->py.get_library, "pp_get_library", ok);
	PY_FUNC_CHECK(pp->py.ctrl, "pp_ctrl", ok);
	PY_FUNC_CHECK(pp->py.update_plob, "pp_update_plob", ok);
	PY_FUNC_CHECK(pp->py.update_ploblist, "pp_update_ploblist", ok); // FUTURE FEATURE
	PY_FUNC_CHECK(pp->py.play_ploblist, "pp_play_ploblist", ok);
	PY_FUNC_CHECK(pp->py.search, "rem_pp_search", ok); // FUTURE FEATURE
	PY_FUNC_CHECK(pp->py.error, "pp_error", ok);
	
	if (!ok) {
		SETERR("pp python module function error");
		pp_py_module_down(pp);
		return;
	}
	
	PyEval_InitThreads();	// XXX realy needed?
				// (see Python/C API Reference Manual 8.1)
	
	return;
}

///////////////////////////////////////////////////////////////////////////////
//
// wrapped server interface (called by Python player proxy)
//
///////////////////////////////////////////////////////////////////////////////

static PyObject*
remuco_server_start_wrapper(PyObject *self, PyObject *args)
{
	rem_pp_t	*pp;
	gint		pp_notifies_server;
	GError		*err;
	gboolean	ok;
	gchar		*name, *module;
	
	if (!args) return NULL;
	
	ok = rem_server_check_compatibility(REM_LIB_MAJOR, REM_LIB_MINOR);
	if (!ok) {
		PyErr_Clear();
		PyErr_SetString(PyExc_RuntimeError,
			"server version not compatible");
		return NULL;
	}
	
	pp = g_malloc0(sizeof(rem_pp_t));
	
	ok = (gboolean) PyArg_ParseTuple(args, "Oisiiis", &pp->py.pp,
		&pp->pinfo.features, &name, &pp->pinfo.rating_none,
		&pp->pinfo.rating_max, &pp_notifies_server, &module);
	
	if (!ok || !name || !module || !pp->py.pp) {
		LOG_ERROR("args malformed\n");
		return NULL;
	}
	
	Py_INCREF(pp->py.pp);
	
	err = NULL;
	pp_py_module_up(pp, module, &err);

	if (err) {
		LOG_ERROR("%s\n", err->message);
		g_free(err);
		g_free(pp);
		return NULL;
	}

	pp->pinfo.name = g_strdup(name);
	
	err = NULL;
	pp->server = rem_server_start(
			pp, &pp->pinfo, (gboolean) pp_notifies_server, &err);
	
	if (err) {
		LOG_ERROR("failed to start server (%s)\n", err->message);
		g_free(err);
		
		pp_py_module_down(pp);
		
		g_free(pp->pinfo.name);
		g_free(pp);
		
		return NULL;
	}
	
	
	return pp_conv_pp2po(pp);
}

static PyObject*
remuco_server_stop_wrapper(PyObject *self, PyObject *args)
{
	rem_pp_t	*pp;
	gboolean	ok;
	PyObject	*py_pp;

	if (!args) return NULL;

	ok = (gboolean) PyArg_ParseTuple(args, "O", &py_pp);

	if (!ok) {
		LOG_ERROR("args malformed\n");
		return NULL;
	}

	GET_PP_RETURN_ON_ERROR(py_pp, pp);
	
	Py_BEGIN_ALLOW_THREADS	// Needed because we are currently blocking the
				// interpreter and so the server thread might
				// currently hang in a Python function. To enable
				// the server to finish its work we must give
				// back the interpreter.
	
	rem_server_stop((rem_server_t*) pp->server);
	
	Py_END_ALLOW_THREADS	// Re-request the interpreter.
	
	pp_py_module_down(pp);

	g_free(pp->pinfo.name);
	g_free(pp);
	
	PyCObject_SetVoidPtr(py_pp, NULL);
	
	Py_RETURN_NONE;
}

static PyObject*
remuco_server_notify_wrapper(PyObject *self, PyObject *args)
{
	rem_pp_t	*pp;
	gboolean	ok;
	PyObject	*py_pp;
	gint		flags;

	ok = (gboolean) PyArg_ParseTuple(args, "Oi", &py_pp, &flags);

	if (!ok) {
		LOG_ERROR("args malformed\n");
		return NULL;
	}

	GET_PP_RETURN_ON_ERROR(py_pp, pp);
	
	rem_server_notify((rem_server_t*) pp->server, flags);
	
	Py_RETURN_NONE;	
}


///////////////////////////////////////////////////////////////////////////////
//
// Python extension stuff
//
///////////////////////////////////////////////////////////////////////////////

static PyMethodDef RemucoMethods[] = {
    {"start",  remuco_server_start_wrapper, METH_VARARGS,
    	"Start the server. "
    	"Params: (O: pp private data which will later be used as first param when pp "
    	"interface functions get callled, i: features, s: name, i: rating_none,"
    	" i: rating_max, "
    	"i: pp_notifies_server, s: calling module) "
    	"Returns: O: pp (this private object reference must be used as first"
    	"parameter when calling the other server interface functions)"},
    {"stop",  remuco_server_stop_wrapper, METH_VARARGS,
    	"Stop the server. "
    	"Params: (O: pp)"
    	"Note: pp will be freed in this function so it cannot be used anymore."},
    {"notify",  remuco_server_notify_wrapper, METH_VARARGS,
    	"Notify the server about a change in player state. "
    	"Params: (O: pp, i: change_flags)"},
    {NULL, NULL, 0, NULL}        /* Sentinel */
};

PyMODINIT_FUNC
initremuco(void)
{
	PyObject	*module;
	
	module = Py_InitModule("remuco", RemucoMethods);
    
	PyModule_AddStringConstant(module, "PLOB_META_ALBUM", REM_PLOB_META_ALBUM);
	PyModule_AddStringConstant(module, "PLOB_META_ARTIST", REM_PLOB_META_ARTIST);
	PyModule_AddStringConstant(module, "PLOB_META_BITRATE", REM_PLOB_META_BITRATE);
	PyModule_AddStringConstant(module, "PLOB_META_COMMENT", REM_PLOB_META_COMMENT);
	PyModule_AddStringConstant(module, "PLOB_META_GENRE", REM_PLOB_META_GENRE);
	PyModule_AddStringConstant(module, "PLOB_META_LENGTH", REM_PLOB_META_LENGTH);
	PyModule_AddStringConstant(module, "PLOB_META_TITLE", REM_PLOB_META_TITLE);
	PyModule_AddStringConstant(module, "PLOB_META_TRACK", REM_PLOB_META_TRACK);
	PyModule_AddStringConstant(module, "PLOB_META_YEAR", REM_PLOB_META_YEAR);
	PyModule_AddStringConstant(module, "PLOB_META_RATING", REM_PLOB_META_RATING);
	PyModule_AddStringConstant(module, "PLOB_META_TAGS", REM_PLOB_META_TAGS);
	PyModule_AddStringConstant(module, "PLOB_META_TYPE", REM_PLOB_META_TYPE);
	PyModule_AddStringConstant(module, "PLOB_META_TYPE_AUDIO", REM_PLOB_META_TYPE_AUDIO);
	PyModule_AddStringConstant(module, "PLOB_META_TYPE_VIDEO", REM_PLOB_META_TYPE_VIDEO);
	PyModule_AddStringConstant(module, "PLOB_META_ANY", REM_PLOB_META_ANY);

	PyModule_AddStringConstant(module, "PLOB_META_IMG", MTN_IMG);

	PyModule_AddStringConstant(module, "PLOBLIST_PLID_PLAYLIST", REM_PLOBLIST_PLID_PLAYLIST);
	PyModule_AddStringConstant(module, "PLOBLIST_NAME_PLAYLIST", REM_PLOBLIST_NAME_PLAYLIST);
	PyModule_AddStringConstant(module, "PLOBLIST_PLID_QUEUE", REM_PLOBLIST_PLID_QUEUE);
	PyModule_AddStringConstant(module, "PLOBLIST_NAME_QUEUE", REM_PLOBLIST_NAME_QUEUE);
	
	PyModule_AddIntConstant(module, "PLOBLISTS_FLAG_EDITABLE", REM_LIBRARY_PL_FLAG_EDITABLE); // FUTURE FEATURE 

	PyModule_AddIntConstant(module, "PS_STATE_STOP", REM_PS_STATE_STOP); 
	PyModule_AddIntConstant(module, "PS_STATE_PLAY", REM_PS_STATE_PLAY); 
	PyModule_AddIntConstant(module, "PS_STATE_PAUSE", REM_PS_STATE_PAUSE); 
	PyModule_AddIntConstant(module, "PS_STATE_PROBLEM", REM_PS_STATE_PROBLEM); 
	PyModule_AddIntConstant(module, "PS_STATE_OFF", REM_PS_STATE_OFF); 
	PyModule_AddIntConstant(module, "PS_STATE_ERROR", REM_PS_STATE_ERROR); 
	PyModule_AddIntConstant(module, "PS_STATE_SRVOFF", REM_PS_STATE_SRVOFF); 
	PyModule_AddIntConstant(module, "PS_STATE_COUNT", REM_PS_STATE_COUNT); 
	
	PyModule_AddIntConstant(module, "PS_SHUFFLE_MODE_OFF", REM_PS_SHUFFLE_MODE_OFF); 
	PyModule_AddIntConstant(module, "PS_SHUFFLE_MODE_ON", REM_PS_SHUFFLE_MODE_ON); 
	
	PyModule_AddIntConstant(module, "PS_REPEAT_MODE_NONE", REM_PS_REPEAT_MODE_NONE); 
	PyModule_AddIntConstant(module, "PS_REPEAT_MODE_PLOB", REM_PS_REPEAT_MODE_PLOB); 
	PyModule_AddIntConstant(module, "PS_REPEAT_MODE_ALBUM", REM_PS_REPEAT_MODE_ALBUM); 
	PyModule_AddIntConstant(module, "PS_REPEAT_MODE_PL", REM_PS_REPEAT_MODE_PL);
	
	PyModule_AddIntConstant(module, "SCTRL_CMD_PLAYPAUSE", REM_SCTRL_CMD_PLAYPAUSE); 
	PyModule_AddIntConstant(module, "SCTRL_CMD_STOP", REM_SCTRL_CMD_STOP); 
	PyModule_AddIntConstant(module, "SCTRL_CMD_RESTART", REM_SCTRL_CMD_RESTART); 
	PyModule_AddIntConstant(module, "SCTRL_CMD_NEXT", REM_SCTRL_CMD_NEXT); 
	PyModule_AddIntConstant(module, "SCTRL_CMD_PREV", REM_SCTRL_CMD_PREV); 
	PyModule_AddIntConstant(module, "SCTRL_CMD_JUMP", REM_SCTRL_CMD_JUMP); 
	PyModule_AddIntConstant(module, "SCTRL_CMD_VOLUME", REM_SCTRL_CMD_VOLUME); 
	PyModule_AddIntConstant(module, "SCTRL_CMD_RATE", REM_SCTRL_CMD_RATE); 
	PyModule_AddIntConstant(module, "SCTRL_CMD_VOTE", REM_SCTRL_CMD_VOTE); // FUTURE FEATURE 
	PyModule_AddIntConstant(module, "SCTRL_CMD_SEEK", REM_SCTRL_CMD_SEEK); // FUTURE FEATURE 
	PyModule_AddIntConstant(module, "SCTRL_CMD_COUNT", REM_SCTRL_CMD_COUNT); 

	PyModule_AddIntConstant(module, "FEATURE_PLAYLIST", REM_PINFO_FEATURE_PLAYLIST);
	PyModule_AddIntConstant(module, "FEATURE_PLAYLIST_EDIT", REM_PINFO_FEATURE_PLAYLIST_EDIT); // FUTURE FEATURE
	PyModule_AddIntConstant(module, "FEATURE_PLAYLIST_JUMP", REM_PINFO_FEATURE_PLAYLIST_JUMP);
	PyModule_AddIntConstant(module, "FEATURE_PLAYLIST_MODE_REPEAT_ONE_PLOB", REM_PINFO_FEATURE_PLAYLIST_MODE_REPEAT_ONE_PLOB);
	PyModule_AddIntConstant(module, "FEATURE_PLAYLIST_MODE_REPEAT_ALBUM", REM_PINFO_FEATURE_PLAYLIST_MODE_REPEAT_ALBUM);
	PyModule_AddIntConstant(module, "FEATURE_PLAYLIST_MODE_REPEAT_PLAYLIST", REM_PINFO_FEATURE_PLAYLIST_MODE_REPEAT_PLAYLIST);
	PyModule_AddIntConstant(module, "FEATURE_PLAYLIST_MODE_SHUFFLE", REM_PINFO_FEATURE_PLAYLIST_MODE_SHUFFLE);
	PyModule_AddIntConstant(module, "FEATURE_QUEUE", REM_PINFO_FEATURE_QUEUE);
	PyModule_AddIntConstant(module, "FEATURE_QUEUE_EDIT", REM_PINFO_FEATURE_QUEUE_EDIT); // FUTURE FEATURE
	PyModule_AddIntConstant(module, "FEATURE_QUEUE_JUMP", REM_PINFO_FEATURE_QUEUE_JUMP);
	PyModule_AddIntConstant(module, "FEATURE_PLOB_EDIT", REM_PINFO_FEATURE_PLOB_EDIT);
	PyModule_AddIntConstant(module, "FEATURE_PLOB_TAGS", REM_PINFO_FEATURE_PLOB_TAGS);
	PyModule_AddIntConstant(module, "FEATURE_SEEK", REM_PINFO_FEATURE_SEEK); // FUTURE FEATURE
	PyModule_AddIntConstant(module, "FEATURE_RATE", REM_PINFO_FEATURE_RATE);
	PyModule_AddIntConstant(module, "FEATURE_PLAY_NEXT_CANDIDATE", REM_PINFO_FEATURE_PLAY_NEXT_CANDIDATE); // FUTURE FEATURE
	PyModule_AddIntConstant(module, "FEATURE_SEARCH", REM_PINFO_FEATURE_SEARCH); // FUTURE FEATURE
	PyModule_AddIntConstant(module, "FEATURE_LIBRARY", REM_PINFO_FEATURE_LIBRARY);
	PyModule_AddIntConstant(module, "FEATURE_LIBRARY_PLOBLIST_CONTENT", REM_PINFO_FEATURE_LIBRARY_PLOBLIST_CONTENT);
		
}

///////////////////////////////////////////////////////////////////////////////

