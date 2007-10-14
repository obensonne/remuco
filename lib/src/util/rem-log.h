/*
 * Copyright (C) 2006 Christian Buennig - See COPYING
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

#ifndef _REMUCO_LOGGING_H_
#define _REMUCO_LOGGING_H_

/**
 * A header providing logging preprocessor macros for user space programs.
 * To use it for kernel space code replace includes und use do_getoftime
 * from linux/time.h instead of time getting used here.
 */

#ifdef __KERNEL__
#ifndef printf
#define printf printk
#endif
#endif

#include <string.h>	// strerror()
#include <errno.h>	// errno
#include <stdio.h>
 
/* When compiling define LOGLEVEL with one of the following macros.
 * Then everything with an equal or lower loglevel will be logged */
#define LL_FATAL	0
#define LL_ERROR	1
#define LL_WARN		2
#define LL_INFO		3
#define LL_DEBUG	4
#define LL_NOISE	5

#ifndef LOGLEVEL
	#define LOGLEVEL LL_ERROR
#endif

/* If LOGTS gets defined at compile time, every log messages has a timestamp
 * prefix */
#ifdef LOGTS
	#include <sys/time.h>
	#define LOGTS1 do {			\
		struct timeval debug_ts;	\
		gettimeofday(&debug_ts, NULL);
	#define LOGTS2 "[%i.%6i] "
	#define LOGTS3 (int)debug_ts.tv_sec, (int)debug_ts.tv_usec, 
	#define LOGTS4 ;	\
		} while(0)
#else
/* If LOGTSJ gets defined at compile time, every log messages has a jiffies
 * prefix */
#ifdef LOGTSJ
	#include <stdio.h>
	#define PROC_JIFFIES "/proc/jiffies"
	#define LOGTS1 do {						\
		FILE *fp;						\
		unsigned long jiffies;					\
		fp = fopen(PROC_JIFFIES, "r");				\
		if (fp)							\
			fscanf(fp, "%lu", &jiffies);
	#define LOGTS2	"[ %lu ] "
	#define LOGTS3	jiffies,
	#define LOGTS4 ;	\
		} while(0)
#else
	#define LOGTS1
	#define LOGTS2
	#define LOGTS3
	#define LOGTS4
#endif
#endif

/* If LOGPID gets defined at compile time, every log messages has a pid
 * prefix */
#ifdef LOGPID
	#include <sys/types.h>
	#include <unistd.h>
	#define LOGPID1 "[ %5i ] "
	#define LOGPID2 getpid(),
#else
	#define LOGPID1
	#define LOGPID2
#endif

#define LABEL_FATAL	"[ FATAL ] "
#define LABEL_ERROR	"[ ERROR ] "
#define LABEL_WARN	"[ WARN  ] "
#define LABEL_INFO	"[ INFO  ] "
#define LABEL_DEBUG	"[ DEBUG ] "
#define LABEL_NOISE	"[ NOISE ] "

/* If LOGCOL gets defined at compile time, every log messages has a particular
 * color depending on its priority/level */
#ifdef LOGCOL
	#define COL_FATAL	"\033[41;3m"
	#define COL_ERROR	"\033[41;3m"
	#define COL_WARN	"\033[31;3m"
	#define COL_INFO	"\033[34;3m"
	#define COL_DEBUG	"\033[35;3m"
	#define COL_NOISE	"\033[37;3m"
	#define COL_END		"\033[0m"
#else
	#define COL_FATAL
	#define COL_ERROR
	#define COL_WARN
	#define COL_INFO
	#define COL_DEBUG
	#define COL_NOISE
	#define COL_END
#endif

#define FUNC_FORMAT_STR "%-25s"

/* These are the offered logging macros, which one will be used depends on the
 * defined loglevel */
#if LL_FATAL <= LOGLEVEL
        #define LOG_FATAL(x, args...) LOGTS1 printf( COL_FATAL LOGTS2 LABEL_FATAL \
        	LOGPID1 FUNC_FORMAT_STR ": " COL_END x, LOGTS3 LOGPID2 __FUNCTION__, ##args) LOGTS4
#else
        #define LOG_FATAL(x, args...)
#endif

#if LL_ERROR <= LOGLEVEL
	#define LOG_ERROR(x, args...) LOGTS1 printf( COL_ERROR LOGTS2 LABEL_ERROR \
		LOGPID1 FUNC_FORMAT_STR ": " COL_END x, LOGTS3 LOGPID2 __FUNCTION__, ##args) LOGTS4
	#define LOG_ERRNO(x, args...) LOG_ERROR(x ": %s\n", ##args, strerror(errno))
#else
	#define LOG_ERROR(x, args...)
	#define LOG_ERRNO(x, args...)
#endif

#if LL_WARN <= LOGLEVEL
        #define LOG_WARN(x, args...)  LOGTS1 printf( COL_WARN LOGTS2 LABEL_WARN \
        	LOGPID1 FUNC_FORMAT_STR ": " COL_END x, LOGTS3 LOGPID2 __FUNCTION__, ##args) LOGTS4
#else
        #define LOG_WARN(x, args...)
#endif

#if LL_INFO <= LOGLEVEL
        #define LOG_INFO(x, args...)  LOGTS1 printf( COL_INFO LOGTS2 LABEL_INFO \
        	LOGPID1 FUNC_FORMAT_STR ": " COL_END x, LOGTS3 LOGPID2 __FUNCTION__, ##args) LOGTS4
#else
        #define LOG_INFO(x, args...)
#endif

#if LL_DEBUG <= LOGLEVEL
        #define LOG_DEBUG(x, args...) LOGTS1 printf( COL_DEBUG LOGTS2 LABEL_DEBUG \
        	LOGPID1 FUNC_FORMAT_STR ": " COL_END x, LOGTS3 LOGPID2 __FUNCTION__, ##args) LOGTS4
#else
        #define LOG_DEBUG(x, args...)
#endif

#if LL_NOISE <= LOGLEVEL
        #define LOG_NOISE(x, args...) LOGTS1 printf( \
        	COL_NOISE LOGTS2 LABEL_NOISE \
        	LOGPID1 FUNC_FORMAT_STR ": " COL_END x, LOGTS3 LOGPID2 __FUNCTION__, ##args) LOGTS4
#else
        #define LOG_NOISE(x, args...)
#endif

#define LOG(x, args...) printf(x, ##args)

#endif //_REMUCO_LOGGING_H_
