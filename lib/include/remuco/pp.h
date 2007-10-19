#ifndef REMUCO_PP_H_
#define REMUCO_PP_H_

#ifndef REMUCO_H_
#error "Include <remuco.h> !"
#endif

G_BEGIN_DECLS

/**
 * @ingroup dx_server
 * @defgroup dx_pp Player Proxy Types
 *  
 * Documentation of player proxy related types.
 * 
 * When a player proxy (PP) interacts with RemLib it does this (amongst others)
 * with callback functions and PP specific data types. They are decribed on this
 * page. You probably don't want to read this page from top to bottom but come
 * here when these callback functions and data type get mentioned in the
 * @ref dx_server documentation.
 */

/*@{*/

/**
 * The RemPPPriv struct is (for RemLib) an opaque data structure to store player
 * proxy private data. Player proxies may define <code>struct _RemPPPriv</code>
 * according to their needs.
 * 
 * @see rem_server_up()
 */
typedef struct _RemPPPriv RemPPPriv;

///////////////////////////////////////////////////////////////////////////////
//
// PP callback functions.
//
///////////////////////////////////////////////////////////////////////////////

/**
 * RemLib requests the PLIDs and names of a media player's ploblists.
 * 
 * @param[in]  pp_priv	the PP's private data
 * 
 * @return a RemLibrarylisting all available ploblists
 * 
 */
typedef RemLibrary*			(*RemPPGetLibraryFunc)
	(RemPPPriv *pp_priv);

/**
 * RemLib requests the PP to synchronize @a status to the current player status.
 * 
 * @param[in]  pp_priv	the PP's private data
 * @param[out] status	the RemPlayerStatus to synchronize
 * 
 * @remark Do not make any assumptions about the values in @a status.
 */
typedef void				(*RemPPSynchronizeFunc)
	(RemPPPriv *pp_priv, RemPlayerStatus *status);

/**
 * RemLib requests a RemPlob with a certain PID.
 * 
 * @param[in]  pp_priv	the PP's private data
 * @param[in]  pid		the PID of the requested RemPlob
 * 
 * @return the requested RemPlob
 */
typedef RemPlob*			(*RemPPGetPlobFunc)
	(RemPPPriv *pp_priv, const gchar *pid);

/**
 * RemLib requests the PIDs of the plobs in a certain ploblist.
 * 
 * @param[in]  pp_priv	the PP's private data
 * @param[in]  pid		the PLID of the ploblist
 * 
 * @return a list of the PIDs of the plobs in the specified ploblist  
 */
typedef RemStringList*		(*RemPPGetPloblistFunc)
	(RemPPPriv *pp_priv, const gchar *plid);

/**
 * RemLib notifies that a crucial error occured. As a result the PP should call
 * rem_server_down().
 * 
 * @param[in]  pp_priv	the PP's private data
 * @param[in]  err		a @p GError describing the occured error
 * 
 */
typedef void				(*RemPPNotifyErrorFunc)
	(RemPPPriv *pp_priv, GError *err);

/**
 * RemLib requests the PP to use (the contents of) the ploblist as the new
 * playlist.
 * 
 * @param[in]  pp_priv	the PP's private data
 * @param[in]  plid		the PLID of the ploblist to play
 * 
 * @remark If a media player has a global playlist, this means clearing the
 *         playlist and putting the contents of the ploblist @a plid into the
 *         global playlist. If a media player has @em no global playlist, this
 *         means switching to the ploblist @a plid.
 * 
 */
typedef void				(*RemPPPlayPloblistFunc)
	(RemPPPriv *pp_priv, const gchar *plid);

/**
 * RemLib requests the PIDs of the plobs that equal in the meta information
 * elements set in @a mask.
 * 
 * @param[in]  pp_priv	the PP's private data
 * @param[in]  mask		a RemPlob to use as the mask for the search
 * 
 * @return a list of the PIDs of the plobs that match @a mask
 * 
 * @remark This is a @em future @em feature (currently not implemented on
 *         client side - as of client version 0.6.0). So you do not need to
 *         bother with that at the moment. You cannot test it anyway.
 * 
 */
typedef RemStringList*		(*RemPPSearchFunc)
	(RemPPPriv *pp_priv, const RemPlob *mask);

/**
 * @ingroup dx_pp
 * RemLib requests the PP to do the simple control command @a command with
 * the parameter @a param. See RemSimpleControlCommand for what the individual
 * commands and possible parameters mean.
 * 
 * @param[in]  pp_priv	the PP's private data
 * @param[in]  cmd		the command to do
 * @param[in]  param	a parameter for @a command
 * 
 */
typedef void				(*RemPPSimpleControlFunc)
	(RemPPPriv *pp_priv, RemSimpleControlCommand cmd, gint param);

/**
 * @ingroup dx_pp
 * RemLib requests the PP to update (on player side) the meta information of a
 * plob.
 * 
 * @param[in]  pp_priv	the PP's private data
 * @param[in]  plob		the plob with the new meta information (which has been
 *                      set on a client)
 * 
 */
typedef void				(*RemPPUpdatePlobFunc)
	(RemPPPriv *pp_priv, const RemPlob *plob);

/**
 * @ingroup dx_pp
 * RemLib requests the PP to update (on player side) the contents of a ploblist.
 * 
 * @param[in]  pp_priv	the PP's private data
 * @param[in]  plid		the PLID of the ploblist to update
 * @param[in]  pids		a list of PIDs of plobs that make up the new ploblist
 * 
 * @remark This is a @em future @em feature (currently not implemented on
 *         client side - as of client version 0.6.0). So you do not need to
 *         bother with that at the moment. You cannot test it anyway.
 */
typedef void				(*RemPPUpdatePloblistFunc)
	(RemPPPriv *pp_priv, const gchar *plid, const RemStringList* pids);

///////////////////////////////////////////////////////////////////////////////

/**
 * The RemPPDescriptor struct describes miscellaneous characteristics of a
 * player proxy (PP) respectively the media player it connects.
 */
typedef struct {
	/** Name of the media player. */
	gchar			*player_name;
	/**
	 * The character set that is used for textual data passed from the PP
	 * to RemLib (set this to <code>NULL</code> if the current locale's
	 * character set is valid).
	 */
	gchar			*charset;
	/**
	 * The player/PP notifies when there are changes in player status,
	 * playlist or queue (if set to @p FALSE, RemLib will periodically check
	 * for changes autonously). 
	 *
	 * @see RemNotifyFlags
	 */
	gboolean		notifies_changes;
	/**
	 * Specifies who is responsible for running a GMainLoop.
	 * If @p TRUE, RemLib starts a GMainLoop when rem_server_up() gets called.
	 * As a result, rem_server_up() blocks and returns when rem_server_down()
	 * has been called. If @p FALSE, rem_server_up() returns immediately -
	 * but in this case the PP developer must ensure that there already is
	 * a GMainLoop running for the default GMainContext and that it @em will
	 * @em run at least until rem_server_down() gets called.
	 */
	gboolean		run_main_loop;
	/**
	 * Maximum rating value used by the media player (0 means the player/PP
	 * does not support rating).
	 */
	guint			max_rating_value;
	/** The player/PP supports seeking to a certain position in a plob. */
	gboolean		supports_seek;
	/** The player has a playlist and the PP can pass the content to RemLib. */
	gboolean		supports_playlist;
	/** The player/PP supports jumping to a certain position in the playlist. */
	gboolean		supports_playlist_jump;
	/** The player has a queue and the PP can pass the content to RemLib. */
	gboolean		supports_queue;
	/** The player/PP supports jumping to a certain position in the queue. */
	gboolean		supports_queue_jump;
	/**
	 * The player/PP supports tags (for instance like in XMMS2 where songs can
	 * be tagged with free choosen words). This only makes sense if the
	 * player/PP also supports edidting of plobs, i.e. the callback function
	 * RemPPCallbacks::update_plob is set.
	 */
	gboolean		supports_tags;
	/** The bitwise or'ed supported repeat modes. */
	RemRepeatMode	supported_repeat_modes;
	/** The bitwise or'ed supported shuffle modes. */
	RemShuffleMode	supported_shuffle_modes;
} RemPPDescriptor;

/**
 * @ingroup dx_pp
 * The RemPPCallbacks struct specifies callback functions to use by RemLib
 * to get information about the media player. Some functions are mandatory and
 * some optional - see the documentation of the individual fields. The meaning
 * of each functions is described in the corresponding function type definiton.
 * 
 */
typedef struct {
	
	/** Mandatory */
	RemPPSynchronizeFunc		synchronize;
	/** Optional */
	RemPPGetLibraryFunc			get_library;
	/** Mandatory */
	RemPPGetPlobFunc			get_plob;
	/** Optional */
	RemPPGetPloblistFunc		get_ploblist;
	/** Mandatory */
	RemPPNotifyErrorFunc		notify_error;
	/** Optional */
	RemPPPlayPloblistFunc		play_ploblist;
	/** Optional */
	RemPPSearchFunc				search;
	/** Mandatory */
	RemPPSimpleControlFunc		simple_ctrl;
	/** Optional */
	RemPPUpdatePlobFunc			update_plob;
	/** Optional */
	RemPPUpdatePloblistFunc		update_ploblist;
	
} RemPPCallbacks;

///////////////////////////////////////////////////////////////////////////////

/**
 * The RemPlayerProxy packs all player proxy related data.
 * 
 * @see rem_server_up()
 */
typedef struct {
	RemPPDescriptor	*desc;
	RemPPCallbacks	*callbacks;
	RemPPPriv		*priv;
} RemPlayerProxy;

/*@}*/

G_END_DECLS

#endif /*REMUCO_PP_H_*/
