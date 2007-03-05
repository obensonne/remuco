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

#ifndef REMUTIL_H_
#define REMUTIL_H_

///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include <time.h>	// nanosleep()
#include <sys/types.h>
#include "rem-log.h"

///////////////////////////////////////////////////////////////////////////////
//
// macros
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Kind of assertions.
 */
#ifdef REM_DO_TESTS
#define REM_TEST(_bool_expr, x) \
	if (!(_bool_expr)) LOG_ERROR("TEST FAILED > " x "\n")
#else
#define REM_TEST(_bool_expr, x)
#endif

/**
 * Sleep some milli seconds
 */
#define REM_SLEEP_MS(_ms) do {					\
	struct timespec ts;					\
	ts.tv_sec = (_ms) / 1000;				\
	ts.tv_nsec = ((_ms) % 1000) * 1000 * 1000;		\
	nanosleep(&ts, NULL);					\
} while(0)

//#define REM_SLEEP_MS(_ms) usleep((_ms) * 1000)

#define REM_MAX_STR_LEN		1024

///////////////////////////////////////////////////////////////////////////////
//
// methods - cmdline arg processing
//
///////////////////////////////////////////////////////////////////////////////

char* arg_get_val_string(int argc, char **args, char* arg);
int arg_get_val_int(int argc, char **args, char* arg, int* val);
int arg_get_val_uint8(int argc, char **args, char* arg, u_int8_t* val);
int arg_get_val_uint16(int argc, char **args, char* arg, u_int16_t* val);
int arg_is_set(int argc, char **args, char* arg);


#endif /*REMUTIL_H_*/
