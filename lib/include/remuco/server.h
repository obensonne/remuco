#ifndef SERVER_H_
#define SERVER_H_

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
 * A bitwise combination representing changes in a player.
 * 
 * @see rem_server_notify()
 */
typedef enum {
	/** The status changed, that is one of the attributes in RemPlayerStatus. */
	REM_NF_STATUS_CHANGED	= 1 << 0,
	/** The playlist content changed. */
	REM_NF_PLAYLIST_CHANGED	= 1 << 1,
	/** The queue content changed. */
	REM_NF_QUEUE_CHANGED	= 1 << 2,
	/** Anything that can change has changed. */
	REM_NF_ALL_CHANGED		= 0xFFFF
} RemNotifyFlags; 

/**
 * The RemServer struct is an opaque data structure to represent a Remuco
 * server.
 */
typedef struct _RemServer		RemServer;

/**
 * Starts a  Remuco server.
 * 
 * @param[in]  pp	a RemPlayerProxy
 * @param[out] err	location to return error in case something fails starting
 *                  the server fails
 * 
 * @return A RemServer which must be used as first parameter for
 *         rem_server_notify() and rem_server_down(). If an error occurs
 *         <code>NULL</code> will be returned and @a err will be set.
 */
RemServer*
rem_server_up(const RemPlayerProxy *pp, GError **err);

/**
 * Notifies a RemServer about changes in a player.
 * 
 * @param[in]  server	a RemServer
 * @param[in]  flags	flags describing what changed
 */
void
rem_server_notify(RemServer* server, RemNotifyFlags flags);

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

#endif /*SERVER_H_*/
