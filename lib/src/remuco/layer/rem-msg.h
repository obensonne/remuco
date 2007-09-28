#ifndef REMMSG_H_
#define REMMSG_H_

#include "../util/rem-common.h"

//#include <remuco/data/rem-data.h>

/**
 * Documentation about message IDs can be found in the client source (class
 * remuco.comm.Message)
 */
 
#define REM_MSG_ID_IGNORE 0
#define REM_MSG_ID_IFS_PINFO 1
#define REM_MSG_ID_IFS_STATE 2
#define REM_MSG_ID_IFS_CURPLOB 3
#define REM_MSG_ID_IFS_PLAYLIST 4
#define REM_MSG_ID_IFS_QUEUE 5
#define REM_MSG_ID_IFS_SRVDOWN 6
#define REM_MSG_ID_IFC_CINFO 7
#define REM_MSG_ID_CTL_SCTRL 8
#define REM_MSG_ID_CTL_UPD_PLOB 9
#define REM_MSG_ID_CTL_UPD_PLOBLIST 10 // FUTURE FEATURE
#define REM_MSG_ID_CTL_PLAY_PLOBLIST 11 // FUTURE FEATURE
#define REM_MSG_ID_REQ_PLOB 12
#define REM_MSG_ID_REQ_PLOBLIST 13
#define REM_MSG_ID_REQ_SEARCH 14
#define REM_MSG_ID_REQ_LIBRARY 15
#define REM_MSG_ID_COUNT 16

typedef struct {
	guint		id;
	gpointer	data;
} rem_msg_t;



//typedef struct {
//	rem_pinfo_t	*mai;
//	rem_ps_t	*mas;
//} rem_msg_HelloToClient_t;
//
//typedef struct {
//	rem_cinfo_t *cli;
//} rem_msg_HelloFromClient_t;
//
//typedef struct {
//	rem_data_medel_t *me;
//} rem_msg_CurrentMediaElement_t;
//
//typedef struct {
//	rem_data_medlist_t *ml;
//} rem_msg_CurrentMediaList_t;
//
//typedef struct {
//	rem_data_medlist_t *ml;
//} rem_msg_CurrentVoteList_t;
//
//typedef struct {
//} rem_msg_ServerDown_t;
//
//typedef struct {
//	rem_data_medel_t *me;
//} rem_msg_UpdateMediaElement_t;
//
//typedef struct {
//	rem_data_medlist_t *ml;
//} rem_msg_UpdateMediaList_t;
//
//
//
//typedef struct {
//} rem_msg__t;
//typedef struct {
//} rem_msg__t;
//typedef struct {
//} rem_msg__t;
//typedef struct {
//} rem_msg__t;

#endif /*REMMSG_H_*/
