#ifndef MISC_H_
#define MISC_H_

#ifndef REMUCO_H_
#error "Include <remuco.h> !"
#endif

G_BEGIN_DECLS

#define REM_PLOBLIST_PLID_PLAYLIST	"__PLAYLIST__"
#define REM_PLOBLIST_NAME_PLAYLIST	"Playlist"
#define REM_PLOBLIST_PLID_QUEUE		"__QUEUE__"
#define REM_PLOBLIST_NAME_QUEUE		"Queue"

/**
 * Simple control commands.
 * 
 * Some of these commands are used in combination with a numeric parameter.
 * In that case the meaning of the parameter is explained too.
 * 
 * @see pp_ctrl()
 */
typedef enum {
	REM_SCTRL_CMD_PLAYPAUSE,
	REM_SCTRL_CMD_STOP,
	REM_SCTRL_CMD_RESTART,
	REM_SCTRL_CMD_NEXT,
	REM_SCTRL_CMD_PREV,
	/**
	 * Jump to a specific position in playlist or queue.
	 * Param:
	 * - &gt; 0 : position in playlist
	 * - &lt; 0 : position in queue
	 * .
	 * So 1 (resp. -1) denotes the first position in the playlist (resp. queue)
	 * -- this is similar to RemPlayerStatus::cap_pos.
	 */
	REM_SCTRL_CMD_JUMP,
	REM_SCTRL_CMD_VOLUME,
	REM_SCTRL_CMD_RATE,
	REM_SCTRL_CMD_VOTE, // FUTURE FEATURE
	REM_SCTRL_CMD_SEEK, // FUTURE FEATURE
	REM_SCTRL_CMD_COUNT
} RemSimpleControlCommand;

G_END_DECLS


#endif /*MISC_H_*/
