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
///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include "rem-util.h"

///////////////////////////////////////////////////////////////////////////////
//
// functions - cmdline arg processing
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Get a the value of an argument.
 * @param argc:
 * 	number of arguments
 * @param args:
 * 	argument list
 * @param arg:
 * 	argument to get value from
 * @return
 * 	ptr to argument value or NULL if arg or val is not found
 */
char* arg_get_val_string(int argc, char **args, char* arg)
{
	int i;
	
	for (i = 0; i < argc - 1; i++) {
		if (strcmp(args[i], arg) == 0) {
			return args[i+1];
			break;
		}
	}
	
	return NULL;
}

/**
 * Get a the value of an argument as an integer.
 * @param argc:
 * 	number of arguments
 * @param args:
 * 	argument list
 * @param arg:
 * 	argument to get value from
 * @param val:
 * 	ptr to store the int value in
 * @return
 * 	 1 : arg found and ok
 * 	-1 : arg not present
 * 	-2 : value is no integer
 */
int arg_get_val_int(int argc, char **args, char* arg, int* val)
{
	char* val_c;
	int ret;
	
	val_c = arg_get_val_string(argc, args, arg);
	
	if (val_c == NULL)
		return -1;
		
	ret = sscanf(val_c, "%i", val);
	if (ret != 1) {
		return -2;
	} else {
		return 1;
	}
}

int arg_get_val_uint8(int argc, char **args, char* arg, u_int8_t* val)
{
	char* val_c;
	int ret, tmp;
	
	val_c = arg_get_val_string(argc, args, arg);
	
	if (val_c == NULL)
		return -1;
		
	ret = sscanf(val_c, "%i", &tmp);
	if (ret != 1 || ret > 255 || ret < 0) {
		return -2;
	} else {
		*val = (u_int8_t) tmp;
		return 1;
	}
}

int arg_get_val_uint16(int argc, char **args, char* arg, u_int16_t* val)
{
	char* val_c;
	int ret, tmp;
	
	val_c = arg_get_val_string(argc, args, arg);
	
	if (val_c == NULL)
		return -1;
		
	ret = sscanf(val_c, "%i", &tmp);
	if (ret != 1 || ret > 65535 || ret < 0) {
		return -2;
	} else {
		*val = (u_int16_t) tmp;
		return 1;
	}
}

/**
 * Check if an argument/option is set.
 * @param argc:
 * 	number of arguments
 * @param args:
 * 	argument list
 * @param arg:
 * 	argument to check
 * @return
 * 	0 : is not set
 * 	1 : is set
 */
int arg_is_set(int argc, char **args, char* arg)
{
	int i;
	for (i = 0; i < argc; i++) {
		if (strcmp(args[i], arg) == 0) {
			return 1;
		}
	}
	
	return 0;
	
}

