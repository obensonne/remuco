#ifndef REMPY_PPDESC_H_
#define REMPY_PPDESC_H_

#include <Python.h>
#include <structmember.h>

typedef struct {
	PyObject_HEAD
	PyObject	*player_name;
	PyObject	*charset;
	int			notifies_changes;
	int			run_main_loop;
	int			max_rating_value;
	int			supports_seek;
	int			supports_playlist;
	int			supports_playlist_jump;
	int			supports_queue;
	int			supports_queue_jump;
	int			supports_tags;
	int			supported_repeat_modes;
	int			supported_shuffle_modes;
} RemPyPPDescriptor;

int
rempy_ppdesc_init(void);

void
rempy_ppdesc_add(PyObject *module);

int
rempy_ppdesc_check_type(PyObject *po);

#endif /*REMPY_PPDESC_H_*/
