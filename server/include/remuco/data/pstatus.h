#ifndef REMUCO_PSTATUS_H_
#define REMUCO_PSTATUS_H_

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
	REM_PBS_STOP,
	/** Play */
	REM_PBS_PLAY,
	/** Paused */
	REM_PBS_PAUSE,
	/** The player is OFF */
	REM_PBS_OFF,
	REM_PBS_COUNT
} RemPlaybackState;

/**
 * Various status flags.
 */
typedef enum {
	REM_PS_FLAG_NONE = 0,
	/** If set, indicates that the player's repeat mode is on. */
	REM_PS_FLAG_REPEAT = 1 << 0,
	/** If set, indicates that the player's shuffle mode is on. */
	REM_PS_FLAG_SHUFFLE = 1 << 1	
} RemPlayerStatusFlags;

/**
 * The RemPPDescriptor struct describes the status of a media player.
 * 
 * @see RemPPSynchronizeFunc
 */
typedef struct {
	/** Playback state. */
	RemPlaybackState		pbs;
	/** Volume. */
	guint					volume;
	/** Status flags. */
	RemPlayerStatusFlags	flags;
	/**
	 * Position of the currently active plob in playlist (or queue).
	 * - > 0 : position within playlist
	 * - = 0 : current plob is neither in playlist nor in queue
	 * - < 0 : position within playlist
	 * 
	 * So the first position in the playlist (or queue) is @em 1 (or @em -1) !
	 */
	gint					cap_pos;
	/**
	 * PID of the currently active plob. Use g_string_printf() or
	 * g_string_assign() to set the PID of the plob. The empty string ("") is
	 * reserved for "no plob". So use g_string_assign() with the empty string
	 * or g_string_truncate() if there is no currently active plob.
	 */
	GString					*cap_pid;
	/**
	 * List of PIDs of the plobs currently in the playlist. Use rem_sl_clear()
	 * and rem_sl_append() or rem_sl_append_const() to set the playlist.
	 * 
	 * @remark If a media player has no @em global @em playlist, playlist means
	 *         the currently active (played) ploblist. For instance Amarok, XMMS
	 *         and XMMS2 have a global playlist, while Rhythmbox has not.
 	 */
	RemStringList			*playlist;
	/**
	 * List of PIDs of the plobs currently in the queue. Use rem_sl_clear()
	 * and rem_sl_append() or rem_sl_append_const() to set the queue.
	 */
	RemStringList			*queue;
} RemPlayerStatus;

/** Creates a new RemPlayerStatus with all fields initialized. */
RemPlayerStatus*
rem_player_status_new(void);

/** Frees a RemPlayerStatus incl. the resources of its fields. */
void
rem_player_status_destroy(RemPlayerStatus*);

/*@}*/

G_END_DECLS

#endif /*REMUCO_PSTATUS_H_*/
