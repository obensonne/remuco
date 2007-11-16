#include "functions.h"
#include "types.h"
#include "util.h"
#include <remuco.h>

struct _RemPPPriv {
	/** Player proxy private data. */
	PyObject			*pp_priv;
	/** Player proxy callbacks. */
	RemPyPPCallbacks	*pp_callbacks;
	/** Player status to use for the player proxy. */
	RemPyPlayerStatus	*pp_ps;
	/**
	 * Self reference as a Python C object. Need to remember it to decref it
	 * on server shut down.
	 */
	PyObject			*self_py;
	RemServer			*server;
};

/** Will be called if the refocunt of RemPPPriv::self_py is 0 */
static void
priv_pp_priv_destroy(void *data)
{
	RemPPPriv *priv = (RemPPPriv*) priv;
	
	Py_CLEAR(priv->pp_priv);
	Py_CLEAR(priv->pp_callbacks);
	Py_CLEAR(priv->pp_ps);
	// priv->self_py should already have a ref count of 0, that's why we're here
	g_slice_free(RemPPPriv, priv);
}

///////////////////////////////////////////////////////////////////////////////
//
// player proxy callback functions - prototypes
//
///////////////////////////////////////////////////////////////////////////////

static void
rcb_synchronize(RemPPPriv *priv, RemPlayerStatus *ps);

static RemLibrary*
rcb_get_library(RemPPPriv *priv);

static RemPlob*
rcb_get_plob(RemPPPriv *priv, const gchar *pid);

static RemStringList*
rcb_get_ploblist(RemPPPriv *priv, const gchar *plid);

static void
rcb_notify(RemPPPriv *priv, RemServerEvent event);

static void
rcb_play_ploblist(RemPPPriv *priv, const gchar *plid);

static RemStringList*
rcb_search(RemPPPriv *priv, const RemPlob *plob);

static void
rcb_simple_control(RemPPPriv *priv, RemSimpleControlCommand cmd, gint param);

static void
rcb_update_plob(RemPPPriv *pp_priv, const RemPlob *plob);

static void
rcb_update_ploblist(RemPPPriv *pp_priv,
				   const gchar *plid,
				   const RemStringList* pids);

///////////////////////////////////////////////////////////////////////////////
//
// data convert functions
//
///////////////////////////////////////////////////////////////////////////////

/**
 * @param sl A string list. Gets cleared in any case. 
 * @param pylist A python list. My be 'None' in which case 'sl' is returned as
 *               an empty string list. If it is not 'None' nor a list, PyErr
 *               gets set.
 */
static void
priv_conv_sl_py2c(PyObject *pylist, RemStringList *sl)
{
	guint		len, u;
	PyObject	*item;
	
	rem_sl_clear(sl);
	
	if (!PyList_Check(pylist)) {
		PyErr_Clear();
		PyErr_SetString(PyExc_TypeError, "invalide type (expected a list)");
		return;
	}
	
	len = PyList_GET_SIZE(pylist);
	LOG_NOISE("pylist has %i elements\n", len);

	for (u = 0; u < len; u++) {
		
		item = PyList_GET_ITEM(pylist, u); // item is a borrowed ref
		
		if (item == Py_None) {
			rem_sl_append(sl, NULL);
		} else if (PyString_CheckExact(item)) {
			rem_sl_append_const(sl, PyString_AS_STRING(item));
		} else {
			PyErr_Clear();
			PyErr_SetString(PyExc_TypeError,
					"invalide type (list element is not a string)");
			return;			
		}
	}
}

static PyObject*
priv_conv_sl_c2py(const RemStringList *sl)
{
	PyObject	*list, *item;
	const gchar	*s;
	
	list = PyList_New(rem_sl_length(sl));
	
	rem_sl_iterator_reset(sl);
	
	while ((s = rem_sl_iterator_next(sl))) {
		
		item = PyString_FromString(s);
		PyList_Append(list, item);
		Py_DECREF(item);
		
	}
	
	return list;
}

static PyObject*
priv_conv_plob_c2py(const RemPlob *plob)
{
	PyObject		*dict, *dict_key, *dict_val;
	const gchar		*meta_name = NULL, *meta_value = NULL;

	dict = PyDict_New();
	
	rem_plob_meta_iter_reset(plob);
	
	rem_plob_meta_iter_next(plob, &meta_name, &meta_value);
	
	while (meta_name) {
		
		dict_key = PyString_FromString(meta_name);
		dict_val = PyString_FromString(meta_value);
		PyDict_SetItem(dict, dict_key, dict_val); // increases ref of dict_{key,val}
		Py_DECREF(dict_key);
		Py_DECREF(dict_val);
		
		rem_plob_meta_iter_next(plob, &meta_name, &meta_value);
	}	
	
	return dict;
}

static RemPPCallbacks*
priv_conv_ppcallbacks_py2c(PyObject *po)
{
	
#define REMPY_FUNC_CHECK(_fo, _fn)								\
	if (!PyCallable_Check(_fo)) {								\
		PyErr_Print();											\
		PyErr_SetString(PyExc_TypeError, "cannot call " _fn);	\
		return NULL;											\
	}

	RemPyPPCallbacks	*ppcb_py;
	RemPPCallbacks		*ppcb_c;
	
	if (!rempy_ppcb_check_type(po)) {
		PyErr_Clear();
		PyErr_SetString(PyExc_TypeError, "invalide type (expected PPCallbacks)");
		return NULL;
	}
	
	ppcb_py = (RemPyPPCallbacks*) po;
	
	ppcb_c = g_slice_new0(RemPPCallbacks);
	
	if (ppcb_py->get_library != Py_None) {
		REMPY_FUNC_CHECK(ppcb_py->get_library, "get_library");
		ppcb_c->get_library = &rcb_get_library;
	}
	if (ppcb_py->get_plob != Py_None) {
		REMPY_FUNC_CHECK(ppcb_py->get_plob, "get_plob");
		ppcb_c->get_plob = &rcb_get_plob;
	}
	if (ppcb_py->get_ploblist != Py_None) {
		REMPY_FUNC_CHECK(ppcb_py->get_ploblist, "get_ploblist");
		ppcb_c->get_ploblist = &rcb_get_ploblist;
	}
	if (ppcb_py->notify != Py_None) {
		REMPY_FUNC_CHECK(ppcb_py->notify, "notify");
		ppcb_c->notify = &rcb_notify;
	}
	if (ppcb_py->play_ploblist != Py_None) {
		REMPY_FUNC_CHECK(ppcb_py->play_ploblist, "play_ploblist");
		ppcb_c->play_ploblist = &rcb_play_ploblist;
	}
	if (ppcb_py->search != Py_None) {
		REMPY_FUNC_CHECK(ppcb_py->search, "search");
		ppcb_c->search = &rcb_search;
	}
	if (ppcb_py->simple_control != Py_None) {
		REMPY_FUNC_CHECK(ppcb_py->simple_control, "simple_control");
		ppcb_c->simple_ctrl = &rcb_simple_control;
	}
	if (ppcb_py->synchronize != Py_None) {
		REMPY_FUNC_CHECK(ppcb_py->synchronize, "synchronize");
		ppcb_c->synchronize = &rcb_synchronize;
	}
	if (ppcb_py->update_plob != Py_None) {
		REMPY_FUNC_CHECK(ppcb_py->update_plob, "update_plob");
		ppcb_c->update_plob = &rcb_update_plob;
	}
	if (ppcb_py->update_ploblist != Py_None) {
		REMPY_FUNC_CHECK(ppcb_py->update_ploblist, "update_ploblist");
		ppcb_c->update_ploblist = &rcb_update_ploblist;
	}
	
	return ppcb_c;
	
}

static RemPPDescriptor*
priv_conv_ppdescriptor_py2c(PyObject *po)
{
	RemPyPPDescriptor	*ppd_py;
	RemPPDescriptor		*ppd;
	
	if (!rempy_ppdesc_check_type(po)) {
		PyErr_Clear();
		PyErr_SetString(PyExc_TypeError, "invalide type (expected PPDescriptor)");
		return NULL;
	}

	ppd_py = (RemPyPPDescriptor*) po;
	
	ppd = g_slice_new0(RemPPDescriptor);
	
	if (ppd_py->charset == Py_None) {
		ppd->charset = NULL;
	} else if (PyString_Check(ppd_py->charset)) {
		ppd->charset = g_strdup(PyString_AsString(ppd_py->charset));
	} else {
		PyErr_Clear();
		PyErr_SetString(PyExc_TypeError, "PPDescriptor.charset is not a string");
		g_slice_free(RemPPDescriptor, ppd);
		return NULL;
	}
	
	if (ppd_py->player_name == Py_None) {
		ppd->player_name = NULL;
	} else if (PyString_Check(ppd_py->player_name)) {
		ppd->player_name = g_strdup(PyString_AsString(ppd_py->player_name));
	} else {
		PyErr_Clear();
		PyErr_SetString(PyExc_TypeError, "PPDescriptor.player_name is not a string");
		if (ppd->charset) g_free(ppd->charset);
		g_slice_free(RemPPDescriptor, ppd);
		return NULL;
	}
	
	ppd->max_rating_value = (guint) ppd_py->max_rating_value;
	ppd->supported_repeat_modes = (RemRepeatMode) ppd_py->supported_repeat_modes;
	ppd->supported_shuffle_modes = (RemShuffleMode) ppd_py->supported_shuffle_modes;
	ppd->supports_playlist = (gboolean) ppd_py->supports_playlist;
	ppd->supports_playlist_jump = (gboolean) ppd_py->supports_playlist_jump;
	ppd->supports_queue = (gboolean) ppd_py->supports_queue;
	ppd->supports_queue_jump = (gboolean) ppd_py->supports_queue_jump;
	ppd->supports_seek = (gboolean) ppd_py->supports_seek;
	ppd->supports_tags = (gboolean) ppd_py->supports_tags;
	
	return ppd;
}

static void
priv_conv_pstatus_py2c(RemPyPlayerStatus *ps_py, RemPlayerStatus *ps)
{
	if (ps_py->cap_pid == Py_None) {
		g_string_truncate(ps->cap_pid, 0);
	} else if (PyString_Check(ps_py->cap_pid)) {
		g_string_assign(ps->cap_pid, PyString_AsString(ps_py->cap_pid));
	} else {
		PyErr_Clear();
		PyErr_SetString(PyExc_TypeError, "PlayerStatus.cap_pid is not a string");
		return;
	}
	
	if (ps_py->playlist == Py_None) {
		PyErr_SetString(PyExc_TypeError, "PlayerStatus.playlist is not set");
		return;
	} else {
		priv_conv_sl_py2c(ps_py->playlist, ps->playlist);
		if (PyErr_Occurred()) {
			PyErr_Print();
			PyErr_SetString(PyExc_TypeError, "PlayerStatus.playlist malformed");
			return;
		}
	}	
	
	if (ps_py->queue == Py_None) {
		PyErr_SetString(PyExc_TypeError, "PlayerStatus.queue is not set");
		return;
	} else {
		priv_conv_sl_py2c(ps_py->queue, ps->queue);
		if (PyErr_Occurred()) {
			PyErr_Print();
			PyErr_SetString(PyExc_TypeError, "PlayerStatus.queue malformed");
			return;
		}
	}
	
	ps->cap_pos = ps_py->cap_pos;
	ps->repeat = (RemRepeatMode) ps_py->repeat;
	ps->shuffle = (RemShuffleMode) ps_py->shuffle;
	ps->pbs = (RemPlaybackState) ps_py->pbs;
	ps->volume = ps_py->volume;
}


///////////////////////////////////////////////////////////////////////////////
//
// player proxy callback functions
//
///////////////////////////////////////////////////////////////////////////////

static void
rcb_synchronize(RemPPPriv *priv, RemPlayerStatus *ps)
{
	PyObject	*args, *ret;
	
	args = Py_BuildValue("OO", priv->pp_priv, (PyObject*) priv->pp_ps);
	
	ret = PyObject_Call(priv->pp_callbacks->synchronize, args, NULL);

	Py_DECREF(args);
	rempy_assert(ret, "error calling function 'synchronize'");
	Py_DECREF(ret);

	priv_conv_pstatus_py2c(priv->pp_ps, ps);
	
	rempy_assert(!PyErr_Occurred(), "bad return from 'synchronize'");
}

static RemLibrary*
rcb_get_library(RemPPPriv *priv)
{
	gboolean		ok;
	PyObject		*args, *ret, *plids, *names, *flags, *item;
	RemLibrary		*lib;
	guint			u, len;
	const gchar		*plid, *name;
	RemPloblistFlag	flag;
	
	////////// call python function //////////
	
	args = Py_BuildValue("O", priv->pp_priv);
	
	ret = PyObject_Call(priv->pp_callbacks->get_library, args, NULL);

	Py_DECREF(args);

	plids = NULL; names = NULL; flags = NULL;
	ok = PyArg_ParseTuple(ret, "OOO", &plids, &names, &flags); // borrowed refs

	////////// check returned data //////////
	
	rempy_assert(ok &&
			PyList_Check(plids) && PyList_Check(names) && PyList_Check(flags),
			"bad return from 'get_library': expected 3 lists");
	rempy_assert(PyList_GET_SIZE(plids) == PyList_GET_SIZE(names),
			"bad return from 'get_library': lists differ in length");
	rempy_assert(PyList_GET_SIZE(names) == PyList_GET_SIZE(flags),
			"bad return from 'get_library': lists differ in length");
	
	////////// build library //////////
	
	lib = rem_library_new();
	
	len = PyList_GET_SIZE(plids);
	
	for (u = 0; u < len; u++) {
		
		item = PyList_GET_ITEM(plids, u); // item is a borrowed ref
		rempy_assert(PyString_CheckExact(item),
				"bad return from 'get_library': PLIDs must be strings");
		plid = PyString_AS_STRING(item); // borrowed ref
		
		item = PyList_GET_ITEM(names, u); // item is a borrowed ref
		rempy_assert(PyString_CheckExact(item),
				"bad return from 'get_library': names must be strings");
		name = PyString_AS_STRING(item); // borrowed ref

		item = PyList_GET_ITEM(flags, u); // item is a borrowed ref
		rempy_assert(PyInt_Check(item),
				"bad return from 'get_library': flags must be integers");
		flag = (RemPloblistFlag) PyInt_AS_LONG(item);

		rem_library_append_const(lib, plid, name, flag);
		
	}
	
	Py_DECREF(ret);

	return lib;
}

static RemPlob*
rcb_get_plob(RemPPPriv *priv, const gchar *pid)
{
	RemPlob			*plob;
	gboolean		ok;
	PyObject		*args, *ret, *dict, *key, *val;
	gint			pos;
	
	////////// call python function //////////
	
	args = Py_BuildValue("Os", priv->pp_priv, pid);
	
	ret = PyObject_Call(priv->pp_callbacks->get_plob, args, NULL);

	Py_DECREF(args);

	dict = NULL;
	ok = PyArg_ParseTuple(ret, "O", &dict); // borrowed refs

	////////// check returned data //////////
	
	rempy_assert(ok && PyDict_Check(dict),
			"bad return from 'get_plob': expected 1 dictionary");
	
	////////// build plob //////////
	
	plob = rem_plob_new(g_strdup(pid));
	
	pos = 0; key = NULL; val = NULL;
	while (PyDict_Next(dict, &pos, &key, &val)) { // borrowed refs
		
		rempy_assert(PyString_CheckExact(key),
				"bad return from 'get_plob': dict must contain strings");
		if (val == Py_None) continue;
		rempy_assert(PyString_CheckExact(val),
				"bad return from 'get_plob': dict must contain strings");

		rem_plob_meta_add_const(
				plob, PyString_AS_STRING(key), PyString_AS_STRING(val));
	}
	
	Py_DECREF(ret);

	return plob;
}

static RemStringList*
rcb_get_ploblist(RemPPPriv *priv, const gchar *plid)
{
	gboolean		ok;
	PyObject		*args, *ret, *list, *item;
	guint			u, len;
	RemStringList	*sl;
	
	sl = rem_sl_new();
	
	////////// call python function //////////
	
	args = Py_BuildValue("Os", priv->pp_priv, plid);
	
	ret = PyObject_Call(priv->pp_callbacks->get_ploblist, args, NULL);

	Py_DECREF(args);

	list = NULL;
	ok = PyArg_ParseTuple(ret, "O", &list); // borrowed refs

	////////// check returned data //////////
	
	rempy_assert(ok && PyList_Check(list),
			"bad return from 'get_ploblist': expected 1 list");
	
	////////// build plob list //////////
	
	sl = rem_sl_new();
	
	len = PyList_GET_SIZE(list);
	
	for (u = 0; u < len; u++) {
		
		item = PyList_GET_ITEM(list, u); // item is a borrowed ref
		rempy_assert(PyString_CheckExact(item),
				"bad return from 'get_ploblist': PIDs must be strings");
		rem_sl_append_const(sl, PyString_AS_STRING(item)); // borrowed ref
		
	}
	
	Py_DECREF(ret);
	
	return sl;
}

static void
rcb_notify(RemPPPriv *priv, RemServerEvent event)
{
	PyObject	*args, *ret;
	
	args = Py_BuildValue("Oi", priv->pp_priv, event);
	
	ret = PyObject_Call(priv->pp_callbacks->notify, args, NULL);

	Py_DECREF(args);
	
	rempy_assert(ret, "error calling function 'notify'");

	if (event == REM_SERVER_EVENT_DOWN) {
		Py_DECREF(priv->self_py);
		// priv_pp_priv_destroy(priv); should be called automatically by the
		// Py_DECREF above
	}
	
	Py_DECREF(ret);

}

static void
rcb_play_ploblist(RemPPPriv *priv, const gchar *plid)
{
	PyObject	*args, *ret;
	
	args = Py_BuildValue("Os", priv->pp_priv, plid);
	
	ret = PyObject_Call(priv->pp_callbacks->play_ploblist, args, NULL);

	Py_DECREF(args);
	
	rempy_assert(ret, "error calling function 'play_ploblist'");
	
	Py_DECREF(ret);

	
}

static RemStringList*
rcb_search(RemPPPriv *priv, const RemPlob *plob)
{
	gboolean		ok;
	PyObject		*args, *ret, *dict, *list, *item;
	guint			u, len;
	RemStringList	*sl;
	
	sl = rem_sl_new();
	
	////////// create python plob //////////
	
	dict = priv_conv_plob_c2py(plob);
	
	////////// call python function //////////
	
	args = Py_BuildValue("OO", priv->pp_priv, dict);
	
	ret = PyObject_Call(priv->pp_callbacks->search, args, NULL);

	Py_DECREF(args);
	Py_DECREF(dict);

	list = NULL;
	ok = PyArg_ParseTuple(ret, "O", &list); // borrowed refs

	////////// check returned data //////////
	
	rempy_assert(ok && PyList_Check(list),
			"bad return from 'search': expected 1 list");
	
	////////// build search result //////////
	
	sl = rem_sl_new();
	
	len = PyList_GET_SIZE(list);
	
	for (u = 0; u < len; u++) {
		
		item = PyList_GET_ITEM(list, u); // item is a borrowed ref
		rempy_assert(PyString_CheckExact(item),
				"bad return from 'search': PIDs must be strings");
		rem_sl_append_const(sl, PyString_AS_STRING(item)); // borrowed ref
		
	}
	
	Py_DECREF(ret);
	
	return sl;
	
}

static void
rcb_simple_control(RemPPPriv *priv, RemSimpleControlCommand cmd, gint param)
{
	PyObject	*args, *ret;
	
	args = Py_BuildValue("Oii", priv->pp_priv, cmd, param);
	
	ret = PyObject_Call(priv->pp_callbacks->simple_control, args, NULL);

	Py_DECREF(args);
	
	rempy_assert(ret, "error calling function 'simple_control'");
	
	Py_DECREF(ret);	
}

static void
rcb_update_plob(RemPPPriv *priv, const RemPlob *plob)
{
	PyObject		*args, *ret, *dict;
	
	////////// create python plob //////////
	
	dict = priv_conv_plob_c2py(plob);
	
	////////// call python function //////////
	
	args = Py_BuildValue("OO", priv->pp_priv, dict);
	
	ret = PyObject_Call(priv->pp_callbacks->update_plob, args, NULL);

	Py_DECREF(args);
	Py_DECREF(dict);

	rempy_assert(ret, "error calling function 'update_plob'");
	
	Py_DECREF(ret);
	
	return;
		
}

static void
rcb_update_ploblist(RemPPPriv *priv,
				   const gchar *plid,
				   const RemStringList* pids)
{
	PyObject		*args, *ret, *list;
	
	////////// create python list //////////
	
	list = priv_conv_sl_c2py(pids);
	
	////////// call python function //////////
	
	args = Py_BuildValue("OsO", priv->pp_priv, plid, list);
	
	ret = PyObject_Call(priv->pp_callbacks->update_ploblist, args, NULL);

	Py_DECREF(args);
	Py_DECREF(list);

	rempy_assert(ret, "error calling function 'update_ploblist'");
	
	Py_DECREF(ret);
	
	return;	
}

///////////////////////////////////////////////////////////////////////////////
//
// server interface wrapper functions
//
///////////////////////////////////////////////////////////////////////////////

PyObject*
rempy_server_up(PyObject *self, PyObject *args)
{
	gboolean		ok;
	PyObject		*pp_desc;		// python pp's descriptor
	PyObject		*pp_callbacks;	// python pp's callbacks
	PyObject		*pp_priv;		// python pp's private data
	RemPPPriv		*priv;			// our (python binding) private data
	RemPPDescriptor	*desc;			// our (python binding) descriptor
	RemPPCallbacks	*callbacks;		// our (python binding) callbacks
	GError			*err;
	
	ok = (gboolean) PyArg_ParseTuple(args, "OOO", &pp_desc, &pp_callbacks,
							&pp_priv);

	if (!ok) return NULL;
	
	callbacks = priv_conv_ppcallbacks_py2c(pp_callbacks); 
	if (!callbacks) {
		PyErr_Print();
		PyErr_SetString(PyExc_TypeError, "bad argument #2");
		return NULL;
	}
	
	desc = priv_conv_ppdescriptor_py2c(pp_desc);
	if (!desc) {
		PyErr_Print();
		PyErr_SetString(PyExc_TypeError, "bad argument #1");
		g_slice_free(RemPPCallbacks, callbacks);
		return NULL;
	}
	
	priv = g_slice_new0(RemPPPriv);
	
	Py_INCREF(pp_priv); // http://docs.python.org/ext/ownershipRules.html
	priv->pp_priv = pp_priv;
	
	Py_INCREF(pp_callbacks); // http://docs.python.org/ext/ownershipRules.html
	priv->pp_callbacks = (RemPyPPCallbacks*) pp_callbacks;
	
	priv->pp_ps = rempy_pstatus_new();

	err = NULL;
	priv->server = rem_server_up(desc, callbacks, priv, &err);
	
	if (!priv->server) {
		PyErr_Clear();
		PyErr_SetString(
				PyExc_RuntimeError, err ? err->message : "server start failed");
		if (err) g_error_free(err);
		g_slice_free(RemPPCallbacks, callbacks);
		if (desc->charset) g_free(desc->charset);
		if (desc->player_name) g_free(desc->charset);
		g_slice_free(RemPPDescriptor, desc);
		priv_pp_priv_destroy(priv);
		return NULL;
	}
	
	priv->self_py = PyCObject_FromVoidPtr(priv, &priv_pp_priv_destroy);
	// ref count of pb_priv->self_py is now 1
	return priv->self_py; // ref count of pb_priv->self_py is now 2
	
}

PyObject*
rempy_server_down(PyObject *self, PyObject *args)
{
	gboolean	ok;
	PyObject	*priv_py;
	RemPPPriv	*priv;

	ok = (gboolean) PyArg_ParseTuple(args, "O", &priv_py);
	
	if (!ok) return NULL;
	
	if (!PyCObject_Check(priv_py)) {
		PyErr_Clear();
		PyErr_SetString(PyExc_TypeError,
				"bad argument #1: expected server private data");
	}
	
	priv = (RemPPPriv*) PyCObject_AsVoidPtr(priv_py);
	
	rem_server_down(priv->server);
	
	Py_INCREF(Py_None);
	
	return Py_None;
}

PyObject*
rempy_server_notify(PyObject *self, PyObject *args)
{
	gboolean	ok;
	PyObject	*priv_py;
	RemPPPriv	*priv;

	ok = (gboolean) PyArg_ParseTuple(args, "O", &priv_py);
	
	if (!ok) return NULL;
	
	if (!PyCObject_Check(priv_py)) {
		PyErr_Clear();
		PyErr_SetString(PyExc_TypeError,
				"bad argument #1: expected server private data");
	}
	
	priv = (RemPPPriv*) PyCObject_AsVoidPtr(priv_py);
	
	rem_server_notify(priv->server);
	
	Py_INCREF(Py_None);
	
	return Py_None;
}

PyObject*
rempy_server_poll(PyObject *self, PyObject *args)
{
	gboolean	ok;
	PyObject	*priv_py;
	RemPPPriv	*priv;

	ok = (gboolean) PyArg_ParseTuple(args, "O", &priv_py);
	
	if (!ok) return NULL;
	
	if (!PyCObject_Check(priv_py)) {
		PyErr_Clear();
		PyErr_SetString(PyExc_TypeError,
				"bad argument #1: expected server private data");
	}
	
	priv = (RemPPPriv*) PyCObject_AsVoidPtr(priv_py);
	
	rem_server_poll(priv->server);
	
	Py_INCREF(Py_None);
	
	return Py_None;
}
