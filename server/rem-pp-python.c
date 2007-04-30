/*
 * Copyright (C) 2006 Christian Buennig - See COPYING
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#if PYTHON_VERSION == 23
#include <python2.3/Python.h>
#elif PYTHON_VERSION == 24
#include <python2.4/Python.h>
#elif PYTHON_VERSION == 25
#include <python2.5/Python.h>
#else
#include <python/Python.h>
#endif
#include <stdlib.h>

#include "rem-pp.h"
#include "rem-log.h"

///////////////////////////////////////////////////////////////////////////////
//
// macros and constants
//
///////////////////////////////////////////////////////////////////////////////

#define ENV_PYTHONPATH_NAME	"PYTHONPATH"
#define ENV_PYTHONPATH_VAL	PYTHON_PATH

#define REM_PP_PYTHON_FUNC_CHECK(_fo, _fn, _err)			\
	if (!PyCallable_Check(_fo)) {					\
		PyErr_Print();						\
		LOG_ERROR("cannot call python function %s\n", _fn);	\
		_err = 1;						\
	}

#define REM_PP_PYTHON_GETITEM_INT(_list, _pos, _int, _err) do {		\
		PyObject *item;						\
		item = PyList_GetItem(_list, _pos);			\
		if (!item) {						\
			_err = 1;					\
			LOG_ERROR("no item at %i\n", _pos);		\
		} else {						\
			_int = PyInt_AsLong(item);			\
			if ((int) _int == -1 && PyErr_Occurred()) {	\
				LOG_ERROR("not an int at %i\n", _pos);	\
				_err = 1;				\
			} else {					\
				LOG_NOISE("got %i\n", _int);		\
			}						\
		}							\
} while(0)

#define REM_PP_PYTHON_GETITEM_STR(_list, _pos, _str, _err) do {		\
		PyObject *item;						\
		char *ca;						\
		_str = NULL;						\
		item = PyList_GetItem(_list, _pos);			\
		if (!item) {						\
			_err = 1;					\
			LOG_ERROR("no item at %i\n", _pos);		\
		} else {						\
			ca = PyString_AsString(item);			\
			if (!(ca)) {					\
				_err = 1;				\
				LOG_ERROR("not a str at %i\n", _pos);	\
			} else {					\
				_str = strdup(ca);			\
				LOG_NOISE("got %s\n", _str);		\
			}						\
		}							\
} while(0)

///////////////////////////////////////////////////////////////////////////////
//
// global vars
//
///////////////////////////////////////////////////////////////////////////////

static PyObject		*pName,		// name of the module to use
			*pModule;	// the python module to use

// references to the corresponding python functions
static PyObject		*pFunc_init, *pFunc_dispose, *pFunc_get_ps,
			*pFunc_process_cmd, *pFunc_get_song;


///////////////////////////////////////////////////////////////////////////////
//
// function prototypes
//
///////////////////////////////////////////////////////////////////////////////

static void rem_pp_python_clean_python(void);

///////////////////////////////////////////////////////////////////////////////
//
// functions public (pp interface)
//
// API documentation to these function can be found in rem-pp.h
//
///////////////////////////////////////////////////////////////////////////////

int
rem_pp_init(void)
{
	PyObject	*pInt_value, *pDict;
	int		error, n;
	char		*py_path, *py_path_new;
	
	// Adjust environment variable ENV_PYTHONPATH_NAME to find needed module
	py_path = getenv(ENV_PYTHONPATH_NAME);
	if (!py_path) py_path = "";
	n = strlen(py_path) + strlen(ENV_PYTHONPATH_VAL) + 2;
	py_path_new = malloc(n);
	if (!py_path_new) {
		LOG_ERROR("malloc failed\n");
		return -1;
	}
	sprintf(py_path_new, "%s:%s", ENV_PYTHONPATH_VAL, py_path);
	setenv(ENV_PYTHONPATH_NAME, py_path_new, 1);
	LOG_INFO("%s=%s\n", ENV_PYTHONPATH_NAME, py_path_new);
	free(py_path_new);
	
	// Initialize the Python Interpreter
	Py_Initialize();

	// Build the name object
	pName = PyString_FromString(REM_PP_PYTHON_MODULE);

	// Load the module object
	pModule = PyImport_Import(pName);

	if (!pModule) {
		PyErr_Print();
		LOG_ERROR("could not load python module\n");
		return -1;
	}

	// pDict is a borrowed reference 
	pDict = PyModule_GetDict(pModule);
	if (!pModule) {
		PyErr_Print();
		LOG_ERROR("could get module dictionary\n");
		return -1;
	}
	
	// pFunc_* are borrowed references 
	pFunc_init = PyDict_GetItemString(pDict, "rem_pp_init");
	pFunc_dispose = PyDict_GetItemString(pDict, "rem_pp_dispose");
	pFunc_get_ps = PyDict_GetItemString(pDict, "rem_pp_get_ps");
	pFunc_process_cmd = PyDict_GetItemString(pDict, "rem_pp_process_cmd");
	pFunc_get_song = PyDict_GetItemString(pDict, "rem_pp_get_song");

	error = 0;
	REM_PP_PYTHON_FUNC_CHECK(pFunc_init, "rem_pp_init", error);
	REM_PP_PYTHON_FUNC_CHECK(pFunc_dispose, "rem_pp_dispose", error);
	REM_PP_PYTHON_FUNC_CHECK(pFunc_get_ps, "rem_pp_get_ps", error);
	REM_PP_PYTHON_FUNC_CHECK(pFunc_process_cmd, "rem_pp_process_cmd", error);
	REM_PP_PYTHON_FUNC_CHECK(pFunc_get_song, "rem_pp_get_song", error);
	
	
	if (error) {
		rem_pp_python_clean_python();
		return -1;
	}
	
	pInt_value = PyObject_CallObject(pFunc_init, NULL);
	if (pInt_value) {
		error = PyInt_AsLong(pInt_value);
		Py_DECREF(pInt_value);
	} else {
		error = -1;
	}
	
	return error;
}

int
rem_pp_process_cmd(struct rem_pp_pc *pc)
{
	int ret;
	PyObject *pInt_value, *pTuple_args;
	
	pTuple_args = PyTuple_New(2);
	pInt_value = PyInt_FromLong(pc->cmd);
	PyTuple_SetItem(pTuple_args, 0, pInt_value);
	pInt_value = PyInt_FromLong(pc->param);
	PyTuple_SetItem(pTuple_args, 1, pInt_value);
	
	pInt_value = PyObject_CallObject(pFunc_process_cmd, pTuple_args);
	Py_DECREF(pTuple_args);
	if (pInt_value) {
		ret = (int) PyInt_AsLong(pInt_value);
		Py_DECREF(pInt_value);
		if (ret < 0) {
			return -1;
		}
	} else {
		LOG_ERROR("python function had no return value\n");
		return -1;
	}
	
	return 0;
}

void
rem_pp_dispose(void)
{
	PyObject_CallObject(pFunc_dispose, NULL);
	rem_pp_python_clean_python();
}

int
rem_pp_get_ps(struct rem_pp_ps *ps)
{
	int			error;
	unsigned int		i;
	PyObject 		*pList_ps, *pList_sids;
	union rem_pp_sid	*sid;
	
	pList_ps = PyObject_CallObject(pFunc_get_ps, NULL);
	if (pList_ps) {
		error = 0;
		
		REM_PP_PYTHON_GETITEM_INT(pList_ps, 0, ps->state, error);
		REM_PP_PYTHON_GETITEM_INT(pList_ps, 1, ps->volume, error);
		REM_PP_PYTHON_GETITEM_INT(pList_ps, 2, ps->pl_repeat, error);
		REM_PP_PYTHON_GETITEM_INT(pList_ps, 3, ps->pl_shuffle, error);
		REM_PP_PYTHON_GETITEM_INT(pList_ps, 4, ps->pl_pos, error);

		pList_sids = PyList_GetItem(pList_ps, 5);
		if (!pList_sids) {
			LOG_ERROR("no sid list in player state\n");
			error = 1;
		}

		if (error) {
			LOG_ERROR("ps data has bad format\n");
			Py_DECREF(pList_ps);
			return -1;
		}
		
		ps->pl_len = PyList_Size(pList_sids);
		LOG_NOISE("got %i (len)\n", ps->pl_len);
		
		ps->pl_sid_type = REM_PP_SID_TYPE_STRING;
		ps->pl_sid_list = malloc(ps->pl_len * sizeof(union rem_pp_sid));
		sid = ps->pl_sid_list;
		
		error = 0;
		for (i = 0; i < ps->pl_len; i++, sid++) {
			REM_PP_PYTHON_GETITEM_STR(pList_sids, i, sid->str, error);
			if (error) {
				LOG_ERROR("sid list has bad format\n");
				Py_DECREF(pList_ps);
				return -1;
			}
		}

		Py_DECREF(pList_ps);

	} else {
		LOG_ERROR("python function had no return value\n");
		return -1;
	}
	
	return 0;
}

void
rem_pp_free_ps(struct rem_pp_ps *ps)
{
	unsigned int u;
	for (u = 0; u < ps->pl_len; u++) {
 		free(ps->pl_sid_list[u].str);
	}
	free(ps->pl_sid_list);
	ps->pl_sid_list = NULL;
}

int
rem_pp_get_song(const union rem_pp_sid *sid, struct rem_pp_song *song)
{
	PyObject	*pDict_song, *pString_sid, *pList_names,
			*pString_name, *pString_value, *pTuple_args;
	int		i;
	char		*str;
	
	LOG_NOISE("getting tags for song %s\n", sid->str);
	pTuple_args = PyTuple_New(1);
	pString_sid = PyString_FromString(sid->str);
	PyTuple_SetItem(pTuple_args, 0, pString_sid);
	pDict_song = PyObject_CallObject(pFunc_get_song, pTuple_args);
	Py_DECREF(pTuple_args);
	if (pDict_song) {
		if (!PyDict_Check(pDict_song)) {
			LOG_ERROR("song data is no PyDict\n");
			Py_DECREF(pDict_song);
			return -1;
		}	
		pList_names = PyDict_Keys(pDict_song);
		song->tag_count = PyList_Size(pList_names);
		if (song->tag_count > REM_MAX_TAGS) {
			LOG_WARN("too much tags, truncating tag list\n");
			song->tag_count = REM_MAX_TAGS;
		}
		for (i = 0; i < song->tag_count; ++i) {
			pString_name = PyList_GetItem(pList_names, i);
			pString_value = PyDict_GetItem(pDict_song, pString_name);
			str = PyString_AsString(pString_name);
			song->tag_names[i] = strdup(str);
			str = PyString_AsString(pString_value);
			song->tag_values[i] = strdup(str);
		}
		Py_DECREF(pDict_song);
		Py_DECREF(pList_names);
	} else {
		LOG_ERROR("python function returned NULL\n");
		Py_DECREF(pString_sid);
		return -1;
	}

	return 0;
}

///////////////////////////////////////////////////////////////////////////////
//
// functions private
//
///////////////////////////////////////////////////////////////////////////////

static void rem_pp_python_clean_python()
{
	// Clean up
	if (pModule) { Py_DECREF(pModule); }
	if (pName) { Py_DECREF(pName); }
	
	// Finish the Python Interpreter
	Py_Finalize();
}
