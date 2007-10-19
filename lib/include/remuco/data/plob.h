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
 * rem_plob_meta_add_const(). In most cases you the predefined meta information
 * keys @p REM_PLOB_META_... will fit your needs.
 *   
 */

/*@{*/

/**
 * An opaque structure representing a playable object.
 * 
 * Use the rem_plob_new() to create and rem_plob_destroy() to free.
 * 
 */
typedef struct _rem_plob RemPlob;

///////////////////////////////////////////////////////////////////////////////
//
// constants
//
///////////////////////////////////////////////////////////////////////////////

/** Meta information name */
#define REM_PLOB_META_ALBUM			"Album"
/** Meta information name */
#define REM_PLOB_META_ARTIST		"Artist"
#define REM_PLOB_META_BITRATE		"Bitrate"
#define REM_PLOB_META_COMMENT		"Comment"
#define REM_PLOB_META_GENRE			"Genre"
#define REM_PLOB_META_LENGTH		"Length"
#define REM_PLOB_META_TITLE			"Title"
/** Meta information name (value means number within album) */
#define REM_PLOB_META_TRACK			"Track"
#define REM_PLOB_META_YEAR			"Year"
#define REM_PLOB_META_RATING		"Rating"
#define REM_PLOB_META_TAGS			"Tags"
/** Meta information name */
#define REM_PLOB_META_TYPE			"Type"
/** Meta information value for REM_PLOB_META_TYPE */
#define REM_PLOB_META_TYPE_AUDIO	"Audio"
/** Meta information value for REM_PLOB_META_TYPE */
#define REM_PLOB_META_TYPE_VIDEO	"Video"
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

const gchar*
rem_plob_meta_get_name(const RemPlob *plob, guint index);

const gchar*
rem_plob_meta_get_value(const RemPlob *plob, guint index);

const gchar*
rem_plob_meta_get(const RemPlob *plob, const gchar *name);

/*@}*/

G_END_DECLS

#endif /*REMUCO_PLOB_H_*/
