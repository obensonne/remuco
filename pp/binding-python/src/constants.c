#include "constants.h"

#include <remuco.h>

void
rempy_constants_add(PyObject *m)
{
	PyModule_AddStringConstant(m, "PLOB_META_ALBUM", REM_PLOB_META_ALBUM);
	PyModule_AddStringConstant(m, "PLOB_META_ARTIST", REM_PLOB_META_ARTIST);
	PyModule_AddStringConstant(m, "PLOB_META_BITRATE", REM_PLOB_META_BITRATE);
	PyModule_AddStringConstant(m, "PLOB_META_COMMENT", REM_PLOB_META_COMMENT);
	PyModule_AddStringConstant(m, "PLOB_META_GENRE", REM_PLOB_META_GENRE);
	PyModule_AddStringConstant(m, "PLOB_META_LENGTH", REM_PLOB_META_LENGTH);
	PyModule_AddStringConstant(m, "PLOB_META_TITLE", REM_PLOB_META_TITLE);
	PyModule_AddStringConstant(m, "PLOB_META_TRACK", REM_PLOB_META_TRACK);
	PyModule_AddStringConstant(m, "PLOB_META_YEAR", REM_PLOB_META_YEAR);
	PyModule_AddStringConstant(m, "PLOB_META_RATING", REM_PLOB_META_RATING);
	PyModule_AddStringConstant(m, "PLOB_META_TAGS", REM_PLOB_META_TAGS);
	PyModule_AddStringConstant(m, "PLOB_META_TYPE", REM_PLOB_META_TYPE);
	PyModule_AddStringConstant(m, "PLOB_META_TYPE_AUDIO", REM_PLOB_META_TYPE_AUDIO);
	PyModule_AddStringConstant(m, "PLOB_META_TYPE_VIDEO", REM_PLOB_META_TYPE_VIDEO);
	PyModule_AddStringConstant(m, "PLOB_META_ANY", REM_PLOB_META_ANY);
	PyModule_AddStringConstant(m, "PLOB_META_ART", REM_PLOB_META_ART);

	PyModule_AddIntConstant(m, "PS_PBS_STOP", REM_PBS_STOP); 
	PyModule_AddIntConstant(m, "PS_PBS_PLAY", REM_PBS_PLAY); 
	PyModule_AddIntConstant(m, "PS_PBS_PAUSE", REM_PBS_PAUSE); 
	PyModule_AddIntConstant(m, "PS_PBS_OFF", REM_PBS_OFF); 
	
	PyModule_AddIntConstant(m, "PS_SHUFFLE_MODE_OFF", REM_SHUFFLE_MODE_OFF); 
	PyModule_AddIntConstant(m, "PS_SHUFFLE_MODE_ON", REM_SHUFFLE_MODE_ON); 
	PyModule_AddIntConstant(m, "PS_REPEAT_MODE_NONE", REM_REPEAT_MODE_NONE); 
	PyModule_AddIntConstant(m, "PS_REPEAT_MODE_PLOB", REM_REPEAT_MODE_PLOB); 
	PyModule_AddIntConstant(m, "PS_REPEAT_MODE_ALBUM", REM_REPEAT_MODE_ALBUM); 
	PyModule_AddIntConstant(m, "PS_REPEAT_MODE_PL", REM_REPEAT_MODE_PL);
	
	PyModule_AddIntConstant(m, "SCTRL_CMD_PLAYPAUSE", REM_SCTRL_CMD_PLAYPAUSE); 
	PyModule_AddIntConstant(m, "SCTRL_CMD_STOP", REM_SCTRL_CMD_STOP); 
	PyModule_AddIntConstant(m, "SCTRL_CMD_RESTART", REM_SCTRL_CMD_RESTART); 
	PyModule_AddIntConstant(m, "SCTRL_CMD_NEXT", REM_SCTRL_CMD_NEXT); 
	PyModule_AddIntConstant(m, "SCTRL_CMD_PREV", REM_SCTRL_CMD_PREV); 
	PyModule_AddIntConstant(m, "SCTRL_CMD_JUMP", REM_SCTRL_CMD_JUMP); 
	PyModule_AddIntConstant(m, "SCTRL_CMD_VOLUME", REM_SCTRL_CMD_VOLUME); 
	PyModule_AddIntConstant(m, "SCTRL_CMD_RATE", REM_SCTRL_CMD_RATE); 
	PyModule_AddIntConstant(m, "SCTRL_CMD_VOTE", REM_SCTRL_CMD_VOTE); // FUTURE FEATURE 
	PyModule_AddIntConstant(m, "SCTRL_CMD_SEEK", REM_SCTRL_CMD_SEEK); // FUTURE FEATURE 
	PyModule_AddIntConstant(m, "SCTRL_CMD_REPEAT", REM_SCTRL_CMD_REPEAT); 
	PyModule_AddIntConstant(m, "SCTRL_CMD_SHUFFLE", REM_SCTRL_CMD_SHUFFLE); 
	PyModule_AddIntConstant(m, "SCTRL_CMD_COUNT", REM_SCTRL_CMD_COUNT); 

	PyModule_AddIntConstant(m, "SERVER_EVENT_ERROR", REM_SERVER_EVENT_ERROR); 
	PyModule_AddIntConstant(m, "SERVER_EVENT_DOWN", REM_SERVER_EVENT_DOWN); 

}
