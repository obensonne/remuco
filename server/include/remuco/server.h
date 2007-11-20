#ifndef REMUCO_SERVER_H_
#define REMUCO_SERVER_H_

#ifndef REMUCO_H_
#error "Include <remuco.h> !"
#endif

G_BEGIN_DECLS

/**
 * @defgroup dx_server Server Interface
 *  
 * The RemLib server interface provides functions to be used by
 * player proxies to control and interact with the Remuco server.
 * Mainy this is
 * - setting up a server with rem_server_up(),
 * - notify the server about player status changes with rem_server_notify() or
 *   let the server automatically check for changes with rem_server_poll()
 * - shutting down the server with rem_server_down().
 */

/*@{*/

/**
 * The RemServer struct is an opaque data structure to represent a Remuco
 * server.
 */
typedef struct _RemServer		RemServer;

/**
 * Sets up a Remuco server. The server will start working if a GMainLoop has
 * been started for the GMainContext that is the default when calling this
 * function.
 * 
 * @param[in]  pp_desc
 * 				Descriptor of the player proxy that starts the server. The
 * 				server takes ownership of @a pp_desc and frees it (incl.
 * 				contained strings) when rem_server_down() is called.
 * @param[in]  pp_callbacks
 * 				Callback functions to use by the server to interact with the
 * 				player proxy. The server takes ownership of @a pp_callback and
 * 				frees it when rem_server_down() is called.
 * @param[in]  pp_priv
 * 				Player proxy private data. This will be used as first parameter
 * 				for the functions in @a pp_callbacks.
 * @param[out] err
 * 				Location to return an error in case starting the server fails.
 * 
 * @return	A RemServer which must be used as first parameter for
 *			rem_server_notify() and rem_server_down(). If an error occurs
 *			<code>NULL</code> will be returned and @a err @em may be set.
 * 
 * @remark	Server takes only ownership and frees @a pp_desc and @a pp_callbacks
 * 			if this function returns successfully, i.e. return value is not
 * 			@p NULL! This means on error, the caller (the PP) must care about
 * 			freeing @a pp_desc and @a pp_callbacks.
 */
RemServer*
rem_server_up(RemPPDescriptor *pp_desc,
			  RemPPCallbacks *pp_callbacks,
			  const RemPPPriv *pp_priv,
			  GError **err);

/**
 * Notifies a RemServer about changes in a player. The RemServer will later call
 * RemPPCallbacks::synchronize to be up to date. This function is the opposite
 * of rem_server_poll(). 
 * 
 * @param server a RemServer
 * 
 * @remark	Multiple calls to this functions in a very short time do not
 *			necessarily result in multiple calls to RemPPCallbacks::synchronize.
 *			The server will call @em once RemPPCallbacks::synchronize if the
 * 			main loop is idle, @em independent of how many times this functions
 * 			has been called before.
 */
void
rem_server_notify(RemServer *server);

/**
 * Instructs a RemServer to periodically check for player changes (by calling
 * RemPPCallbacks::synchronize). This function is the opposite of
 * rem_server_notify(). 
 * 
 * @param server a RemServer
 * 
 * @remark	Once called, any further call to this function has no effect.
 */
void
rem_server_poll(RemServer *server);


/**
 * Shuts down a RemServer.
 * 
 * Disconnects all clients and frees all of the memory used by a RemServer.
 * 
 * @param rem a RemServer
 * 
 * @remark	The function returns immediately but the shutdown process may take
 *			some more milli seconds. When shut down is finished,
 *			RemPPCallbacks::notify will be called with the event
 * 			@ref REM_SERVER_EVENT_DOWN.
 */
void
rem_server_down(RemServer* rem);

/*@}*/

G_END_DECLS

#endif /*REMUCO_SERVER_H_*/
