#ifndef SERVER_IF_H_
#define SERVER_IF_H_

// needed header files should already be included when including this

///////////////////////////////////////////////////////////////////////////////
//
// Server interface functions (to be called by the PP)
//
///////////////////////////////////////////////////////////////////////////////

typedef struct _rem_server	rem_server_t;

typedef struct _rem_pp		rem_pp_t;

typedef enum {
	REM_SERVER_NF_PS_CHANGED		= 1 << 0,
	REM_SERVER_NF_PLOB_CHANGED		= 1 << 1,
	REM_SERVER_NF_PLAYLIST_CHANGED		= 1 << 2,
	REM_SERVER_NF_QUEUE_CHANGED		= 1 << 3,
	REM_SERVER_NF_ALL_CHANGED		= 0xFFFF
} rem_server_notify_flags; 

G_CONST_RETURN rem_server_t*
rem_server_start(const rem_pp_t *pp,
		 const rem_pinfo_t *pi,
		 gboolean pp_notifies_server,
		 GError **err);

void
rem_server_stop(rem_server_t *server);

void
rem_server_notify(rem_server_t *server, rem_server_notify_flags flags);

gboolean
rem_server_check_compatibility(guint major, guint minor);

#endif /*SERVER_IF_H_*/
