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
 */

/*@{*/

/**
 * The RemServer struct is an opaque data structure to represent a Remuco
 * server.
 */
typedef struct _RemServer		RemServer;

/**
 * Starts a  Remuco server.
 * 
 * @param[in]  pp_desc		Descriptor of the player proxy that starts the server.
 * @param[in]  pp_callbacks	Callback functions to use by the server to interact
 * 							with the player proxy.
 * @param[in]  pp_priv		Player proxy private data. This will be used as
 * 							first parameter for the functions in @a pp_callbacks.
 * @param[out] err	Location to return error in case something fails starting
 *                  the server fails.
 * 
 * @return A RemServer which must be used as first parameter for
 *         rem_server_notify() and rem_server_down(). If an error occurs
 *         <code>NULL</code> will be returned and @a err will be set.
 */
RemServer*
rem_server_up(const RemPPDescriptor *pp_desc,
			  const RemPPCallbacks *pp_callbacks,
			  const RemPPPriv *pp_priv,
			  GError **err);

/**
 * Notifies a RemServer about changes in a player. The RemServer will later call
 * RemPPCallbacks::synchronize to be up to date. 
 * 
 * @param server a RemServer
 * 
 * @remark Multiple calls to this functions in a very short time don't
 *         necessarily result in multiple calls to RemPPCallbacks::synchronize.
 *         RemLib processes the changes when the GMainLoop is some kind of idle.
 *        
 */
void
rem_server_notify(RemServer *server);

/**
 * Shuts down a RemServer.
 * 
 * Disconnects all clients and frees all of the memory used by a RemServer.
 * 
 * @param rem a RemServer
 * 
 * @remark The function returns immediately but the shutdown process may take
 *         some more milli seconds. So please wait a bit before completely
 *         shutting down your process/app. @todo may be a callback 
 *         @p shutdown_finished() is a good idea
 * 
 * @remark If rem_server_up() has been called with @a run_main_loop @p == TRUE,
 *         then rem_server_up() will return as a result of calling this function.
 */
void
rem_server_down(RemServer* rem);

/*@}*/

G_END_DECLS

#endif /*REMUCO_SERVER_H_*/
