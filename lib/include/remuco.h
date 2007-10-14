#ifndef REMUCO_H_
#define REMUCO_H_

#include <glib.h>

#include <remuco/log.h>

#include <remuco/data/stringlist.h>
#include <remuco/data/pstatus.h>
#include <remuco/data/plob.h>
#include <remuco/data/library.h>
#include <remuco/data/misc.h>

#include <remuco/pp.h>

#include <remuco/server.h>


//////////////////////////////////////////////////////////////////////////////
//
// Everything below is for Doxygen.
//
//////////////////////////////////////////////////////////////////////////////

/**
 * @defgroup dx_dt Data Types
 * 
 * 
 *
 */

/**
 * @mainpage Remuco Library (RemLib)
 * 
 * The Remuco library (RemLib) implements a Bluetooth server for Remuco clients
 * and manages remote control interaction between clients and media players.
 * Media player specific functionalities are implemented by player proxies.
 * So these proxies (respectively their developers) are the users of RemLib.
 * 
 * - See @ref guide for how to write a Remuco player proxy.
 * - The server interface for player proxies is described in @ref dx_server.
 * - Remuco data types are described in @ref dx_dt.
 * 
 *  
 * 
 */

/**
 * @page guide Player Proxy Writing Guide
 * Hello.
 * xx.
 * @section label title
 * intro.
 * @subsection label title
 * intro.
 * x
 * yy
 * @section x
 * @section sec Na du?
 * Bla Bla
 * @section sec2 Ja watn?
 * Bla Bla
 * Ja und son sto ..
 */

/**
 * @page abr Glossar
 * @anchor PLOB
 * - @b PLOB : <i>Playable Object</i> - in most cases this means a song, but it
 *   could also be a video or a photo (as a slide show element)  
 * @anchor dx_PID
 * 	- PID <i>PLOD ID</i>
 * 	- @anchor dx_PLID <i>PLOB-list ID</i>
 * 
 */

#endif /*REMUCO_H_*/
