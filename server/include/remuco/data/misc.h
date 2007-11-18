#ifndef REMUCO_MISC_H_
#define REMUCO_MISC_H_

#ifndef REMUCO_H_
#error "Include <remuco.h> !"
#endif

G_BEGIN_DECLS

/**
 * Simple control commands.
 * 
 * Some of these commands are used in combination with a numeric parameter.
 * In that case the meaning of the parameter is explained too.
 * 
 * @see RemPPCallbacks::simple_ctrl
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
	/**
	 * Adjust the volume.
	 * Param: the volume in percent
	 */
	REM_SCTRL_CMD_VOLUME,
	/**
	 * Rate the currently active plob.
	 * Param: the rating value with 0 <= rating <=
	 *        RemPPDescriptor::max_rating_value
	 */
	REM_SCTRL_CMD_RATE,
	/** FUTURE FEATURE */
	REM_SCTRL_CMD_VOTE, // <FUTURE FEATURE
	/** FUTURE FEATURE */
	REM_SCTRL_CMD_SEEK,
	/**
	 * Set the repeat mode.
	 * Param: the repeat mode (see RemRepeatMode)
	 */
	REM_SCTRL_CMD_REPEAT,
	/**
	 * Set the shuffle mode.
	 * Param: the shuffle mode (see RemShuffleMode)
	 */
	REM_SCTRL_CMD_SHUFFLE,
	REM_SCTRL_CMD_COUNT
} RemSimpleControlCommand;


G_END_DECLS


#endif /*REMUCO_MISC_H_*/
