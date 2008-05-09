#ifndef SERVER_H_
#define SERVER_H_

#include <glib-object.h>

typedef struct _RemServerPriv RemServerPriv;

typedef struct {

	GObject			parent;
	
	RemServerPriv	*priv;
	
} RemServer;

typedef struct
{
	GObjectClass parent_class;
	
} RemServerClass;

GType
rem_server_get_type(void);

#define REM_SERVER_TYPE		  		(rem_server_get_type ())
#define REM_SERVER(obj)		  		(G_TYPE_CHECK_INSTANCE_CAST ((obj), REM_SERVER_TYPE, RemServer))
#define REM_SERVER_CLASS(klass)	  	(G_TYPE_CHECK_CLASS_CAST ((klass), REM_SERVER_TYPE, RemServerClass))
#define REM_IS_SERVER(obj)	  		(G_TYPE_CHECK_INSTANCE_TYPE ((obj), REM_SERVER_TYPE))
#define REM_IS_SERVER_CLASS(klass)	(G_TYPE_CHECK_CLASS_TYPE ((klass), REM_SERVER_TYPE))
#define REM_SERVER_GET_CLASS(obj)  	(G_TYPE_INSTANCE_GET_CLASS ((obj), REM_SERVER_TYPE, RemServerClass))

///////////////////////////////////////////////////////////////////////////////
//
// internal interface
//
///////////////////////////////////////////////////////////////////////////////

gboolean
rem_server_up(RemServer *server, GMainLoop *ml);

void
rem_server_down(RemServer *server);

///////////////////////////////////////////////////////////////////////////////
//
// dbus interface
//
///////////////////////////////////////////////////////////////////////////////

gboolean
rem_server_check(RemServer *server, guint version, GError **error);

gboolean
rem_server_hello(RemServer *server, gchar *player,
				 guint flags, guint rating,
				 GError **error);

gboolean
rem_server_update_state(RemServer *server, gchar *player,
						guint playback, guint volume, gboolean repeat,
						gboolean shuffle, guint position, gboolean queue,
						GError **err);

gboolean
rem_server_update_plob(RemServer *server, gchar *player,
					   gchar *id, gchar *img, GHashTable *meta,
					   GError **err);

gboolean
rem_server_update_playlist(RemServer *server, gchar *player,
						   gchar **ids, gchar **names,
						   GError **err);

gboolean
rem_server_update_queue(RemServer *server, gchar *player,
						gchar **ids, gchar **names,
						GError **err);

gboolean
rem_server_bye(RemServer *server, gchar *player,
			   GError **err);

#endif /*SERVER_H_*/
