#ifndef PS_H_
#define PS_H_

#ifndef REMUCO_H_
#error "Include <remuco.h> !"
#endif

G_BEGIN_DECLS

/**
 * @ingroup dx_dt
 * @defgroup dx_ps Player Status 
 */

/*@{*/

/**
 * Playback states.
 */
typedef enum {
	/** Stop */
	REM_PS_STATE_STOP,
	/** Play */
	REM_PS_STATE_PLAY,
	/** Paused */
	REM_PS_STATE_PAUSE,
	/** The player is OFF */
	REM_PS_STATE_OFF,
	REM_PS_STATE_COUNT
} RemPlaybackState;

/**
 * Playback shuffle modes.
 */
typedef enum {
	/** Sequential play order. */
	REM_PS_SHUFFLE_MODE_OFF = 0,
	/** Random play order. */
	REM_PS_SHUFFLE_MODE_ON = 1
} RemShuffleMode;

/**
 * Playback repeat modes.
 */
typedef enum {
	/** No repeat. */
	REM_PS_REPEAT_MODE_NONE = 0,
	/** Repeat the current plob. */
	REM_PS_REPEAT_MODE_PLOB = 1 << 0,
	/** Repeat the current album. */
	REM_PS_REPEAT_MODE_ALBUM = 1 << 1,
	/** Repeat the playlist. */
	REM_PS_REPEAT_MODE_PL = 1 << 2
} RemRepeatMode;

/**
 * Flags describing differences between two RemPlayerStatus.
 * 
 * @see rem_player_status_compare()
 */
typedef enum {
	/** No difference. */
	REM_PS_DIFF_NONE = 0,
	/** @a state, @a volume, @a repeat, @a shuffle and/or @a cap_pos differs. */
	REM_PS_DIFF_SVRSP = 1 << 0,
	/** @a cap_pid differs. */
	REM_PS_DIFF_PID = 1 << 2,
	/** All differ. */
	REM_PS_DIFF_ALL = 0xFFFF
} RemPlayerStatusCompareResult;

/**
 * The RemPPDescriptor struct describes the status of a media player.
 * 
 * @see RemPPGetPlayerStatusFunc
 */
typedef struct {
	/** Playback state. */
	RemPlaybackState		state;
	/** Volume. */
	gint				volume;
	/** Playback state. */
	RemRepeatMode	repeat;
	/** Playback state. */
	RemShuffleMode	shuffle;
	/**
	 * Position of the currently active plob in playlist (or queue).
	 * - > 0 : position within playlist
	 * - = 0 : current plob is neither in playlist nor in queue
	 * - < 0 : position within playlist
	 * 
	 * So the first position in the playlist (or queue) is @em 1 (or @em -1) !
	 */
	gint				cap_pos;
	/**
	 * PID of the currently active plob. Use g_string_assign() to set the PID.
	 */
	GString				*cap_pid;
} RemPlayerStatus;

RemPlayerStatus*
rem_player_status_new(void);

void
rem_player_status_destroy(RemPlayerStatus*);

void
rem_player_status_copy(RemPlayerStatus *src, RemPlayerStatus *dst);

RemPlayerStatusCompareResult
rem_player_status_compare(RemPlayerStatus *one, RemPlayerStatus *two);

/*@}*/

G_END_DECLS

#endif /*PS_H_*/
