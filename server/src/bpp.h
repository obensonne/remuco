#ifndef BPP_H_
#define BPP_H_

#include <glib-object.h>

typedef struct _RemBasicProxyPriv RemBasicProxyPriv;

typedef struct {
	
	GObject				parent;
	
	gboolean			error;
	
	RemBasicProxyPriv	*priv;
	
} RemBasicProxy;

typedef struct
{
	GObjectClass parent_class;
	
} RemBasicProxyClass;

GType
rem_basic_proxy_get_type(void);

#define REM_BASIC_PROXY_TYPE		  	(rem_basic_proxy_get_type ())
#define REM_BASIC_PROXY(obj)		  	(G_TYPE_CHECK_INSTANCE_CAST ((obj), REM_BASIC_PROXY_TYPE, RemBasicProxy))
#define REM_BASIC_PROXY_CLASS(klass)	(G_TYPE_CHECK_CLASS_CAST ((klass), REM_BASIC_PROXY_TYPE, RemBasicProxyClass))
#define REM_IS_BASIC_PROXY(obj)	  		(G_TYPE_CHECK_INSTANCE_TYPE ((obj), REM_BASIC_PROXY_TYPE))
#define REM_IS_BASIC_PROXY_CLASS(klass)	(G_TYPE_CHECK_CLASS_TYPE ((klass), REM_BASIC_PROXY_TYPE))
#define REM_BASIC_PROXY_GET_CLASS(obj)  (G_TYPE_INSTANCE_GET_CLASS ((obj), REM_BASIC_PROXY_TYPE, RemBasicProxyClass))

///////////////////////////////////////////////////////////////////////////////
//
// dbus interface
//
///////////////////////////////////////////////////////////////////////////////

gboolean
rem_pp_control(RemBasicProxy *bpp,
			   guint control, gint paramI, gchar *paramS,
			   GError **err);

gboolean
rem_pp_request_plob(RemBasicProxy *bpp,
					gchar *id, GHashTable **meta,
					GError **err);

gboolean
rem_pp_request_ploblist(RemBasicProxy *bpp,
						gchar *id,
						gchar ***nested_ids, gchar ***nested_names,
						gchar ***ids, gchar ***names,
						GError **err);

gboolean
rem_pp_bye(RemBasicProxy *bpp, GError **err);

///////////////////////////////////////////////////////////////////////////////
//
// internal interface
//
///////////////////////////////////////////////////////////////////////////////

gboolean
rem_bpp_up(RemBasicProxy *bpp, const gchar *name, GMainLoop *ml);

void
rem_bpp_down(RemBasicProxy *bpp);


#endif /*BPP_H_*/
