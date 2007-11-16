#ifndef REMUCO_PLOB_H_
#define REMUCO_PLOB_H_

#ifndef REMUCO_H_
#error "Include <remuco.h> !"
#endif

G_BEGIN_DECLS

/**
 * @ingroup dx_dt
 * @defgroup dx_plob Plob
 * 
 * A plob is a playable object or in other words: anything you can play in a
 * player. In most cases a plob denotes a song. But videos, photos, slides, ..
 * can also be seen as plobs.
 * 
 * Plobs have a plob ID (PID), some meta information and optionally an image
 * (e.g. album art). The PID is set when creating a plob with rem_plob_new().
 * Meta information is technically a list of key-value associations.
 * You can set meta information about a plob with rem_plob_meta_add() and
 * rem_plob_meta_add_const(). In most cases the predefined meta information
 * keys (e.g. @ref REM_PLOB_META_ARTIST) will fit your needs.
 *   
 */

/*@{*/

/**
 * An opaque structure representing a playable object.
 * 
 * Use the rem_plob_new() to create and rem_plob_destroy() to free.
 * 
 */
typedef struct _RemPlob RemPlob;

///////////////////////////////////////////////////////////////////////////////
//
// constants
//
///////////////////////////////////////////////////////////////////////////////

/** Meta information name */
#define REM_PLOB_META_ALBUM			"album"
/** Meta information name */
#define REM_PLOB_META_ARTIST		"artist"
#define REM_PLOB_META_BITRATE		"bitrate"
#define REM_PLOB_META_COMMENT		"comment"
#define REM_PLOB_META_GENRE			"genre"
/** Meta information name (value must be length in seconds) */
#define REM_PLOB_META_LENGTH		"length"
#define REM_PLOB_META_TITLE			"title"
/** Meta information name (value means number within album) */
#define REM_PLOB_META_TRACK			"track"
#define REM_PLOB_META_YEAR			"year"
/** Meta information name (value must be a non-negative number) */
#define REM_PLOB_META_RATING		"rating"
#define REM_PLOB_META_TAGS			"tags"
/** Meta information name */
#define REM_PLOB_META_TYPE			"__type__"
/** Meta information value for REM_PLOB_META_TYPE */
#define REM_PLOB_META_TYPE_AUDIO	"audio"
/** Meta information value for REM_PLOB_META_TYPE */
#define REM_PLOB_META_TYPE_VIDEO	"video"
/** Meta information value for REM_PLOB_META_TYPE */
#define REM_PLOB_META_TYPE_OTHER	"other"
/** Meta information name (value must be an image's filename) */
#define REM_PLOB_META_ART			"__art__"
#define REM_PLOB_META_ANY			"__any__"


///////////////////////////////////////////////////////////////////////////////
//
// create and destroy plobs
//
///////////////////////////////////////////////////////////////////////////////

RemPlob*
rem_plob_new(const gchar *pid);

/**
 * Creates a new plob with artist and title set to 'unknown'.
 * 
 * @param pid the PDI of the reqeuested plob
 * 
 * @return the requested plob
 * 
 * @remark Use this function to create a plob to return if you cannot deliver a
 *         plob requested by RemPPGetPlobFunc().
 */
RemPlob*
rem_plob_new_unknown(const gchar *pid);

void
rem_plob_destroy(RemPlob *p);

///////////////////////////////////////////////////////////////////////////////
//
// working with plobs
//
///////////////////////////////////////////////////////////////////////////////

void
rem_plob_set_img(RemPlob *p, const gchar *file);

void
rem_plob_meta_add(RemPlob *p, gchar *name, gchar *value);

void
rem_plob_meta_add_const(RemPlob *p, const gchar *name, const gchar *value);

guint
rem_plob_meta_num(const RemPlob *plob);

//const gchar*
//rem_plob_meta_get_name(const RemPlob *plob, guint index);
//
//const gchar*
//rem_plob_meta_get_value(const RemPlob *plob, guint index);

void
rem_plob_meta_iter_reset(const RemPlob *plob);

/**
 * 
 * @remark The @p const decalration of @a plob means the content of the plob
 *         list, not the meta information iterator state! 
 */
void
rem_plob_meta_iter_next(const RemPlob *plob,
						const gchar **name,
						const gchar **value);

/**
 * Get the value of a meta inforation.
 * 
 * @param plob	a RemPlob
 * @param name	the meta information's name (use one of @p REM_PLOB_META_... -
 *              e.g. @ref REM_PLOB_META_ARTIST)
 * 
 * @return the meta information value or '' (empty string) if the meta
 *         information does not exist -> so this method @em never returns
 *         <code>NULL</code>
 *  
 * @remark The @p const decalration of @a plob means the content of the plob
 *         list, not the meta information iterator state! 
 */
const gchar*
rem_plob_meta_get(const RemPlob *plob, const gchar *name);

/*@}*/

G_END_DECLS

#endif /*REMUCO_PLOB_H_*/
