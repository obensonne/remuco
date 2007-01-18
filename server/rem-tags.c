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
// Summary
//
// Functions to read meta-data from music files. Supportet meta-data types
// are listed by constants REM_TAGS_TYPE_... .
//
///////////////////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include <id3tag.h>		// from libid3tag devel
#include <stdlib.h>

#include "rem-pp.h"
#include "rem-pp-util.h"
#include "rem-log.h"
#include "rem-tags.h"
#include "rem-util.h"

///////////////////////////////////////////////////////////////////////////////
//
// macros and constants
//
///////////////////////////////////////////////////////////////////////////////

#define REM_TAGS_TYPE_UNKNOWN	0
#define REM_TAGS_TYPE_ID3	1

#ifndef ID3_FRAME_TLEN
#define ID3_FRAME_TLEN	"TLEN"
#endif

///////////////////////////////////////////////////////////////////////////////
//
// function prototypes
//
///////////////////////////////////////////////////////////////////////////////

static char*
rem_tags_id3_get_tag(const struct id3_tag *tag,	const char *frame_name);

static int
rem_tags_id3_read(const char *file, const char **tag_names,
				const int tag_count, struct rem_pp_song *song);
						
static const char*
rem_tags_id3_map_tag_to_frame_name(const char *tag_name);

static int
rem_tags_get_tag_type(const char *file);

///////////////////////////////////////////////////////////////////////////////
//
// global vars
//
///////////////////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////////////////////////
//
// functions - interface
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Trys to read meta information specified by 'tag_names' from 'file' and
 * appends the found information as tag-name and -value pairs into 'song'.
 * @return
 * 	 0 : ok
 * 	-1 : error (song is left unchanged)
 */
int
rem_tags_read(const char *file, const char **tag_names, const int tag_count,
						struct rem_pp_song *song)
{
	int file_type;
	
	LOG_NOISE("reading tags of '%s'\n", file);
	
	file_type = rem_tags_get_tag_type(file);
	
	switch (file_type) {
		case REM_TAGS_TYPE_ID3:
			return rem_tags_id3_read(file, tag_names, tag_count,
									song);
		default:
			LOG_ERROR("unsupported file type\n");
			return -1;
	}
}

///////////////////////////////////////////////////////////////////////////////
//
// functions - internal (id3)
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Determines type of tags contained 'file'.
 * @return
 * 	on of REM_TAGS_TYPE_...
 */
static int
rem_tags_get_tag_type(const char *file)
{
	int pos;
	char *file_type;
	
	// determine file type (based on file name sufix)
	pos = strlen(file);
	while (pos > 0 && file[pos-1] != '.')
		pos--;
	
	file_type = (char*) (file + pos);
	
	LOG_NOISE("file type is '%s'\n", file_type);
	
	if (strcasecmp("mp3", file_type) == 0)
		return REM_TAGS_TYPE_ID3;
	
	return REM_TAGS_TYPE_UNKNOWN;
}

///////////////////////////////////////////////////////////////////////////////
//
// functions - internal (id3)
//
///////////////////////////////////////////////////////////////////////////////

static int
rem_tags_id3_read(const char *file, const char **tag_names, const int tag_count,
						struct rem_pp_song *song)
{
	LOG_NOISE("called\n");
	
	REM_TEST(file, "file");
	REM_TEST(tag_names, "tag_names");
	REM_TEST(song, "song");
	
	char *val, null_char = '\0';
	const char* frame;
	int i;
	
	struct id3_file *id3_file = NULL;
	struct id3_tag *id3_tag = NULL;
	

	id3_file = id3_file_open(file, ID3_FILE_MODE_READONLY);
	if (id3_file == NULL) {
		LOG_ERRNO("open '%s' for reading id3 failed", file);
		return -1;
	} else {
		id3_tag = id3_file_tag(id3_file);
		if (id3_tag == NULL) {
			LOG_ERRNO("read id3-data from '%s' failed", file);
			id3_file_close(id3_file); // XXX free id3_file ???
			return -1;
		}
	}
	
	for (i = 0; i < tag_count; i++) {
		frame = rem_tags_id3_map_tag_to_frame_name(tag_names[i]);
		val = rem_tags_id3_get_tag(id3_tag, frame);
		val = val ? val : &null_char;
		rem_song_append_tag(song, tag_names[i], val);
		if (val != &null_char) free(val);
	}

	id3_file_close(id3_file); // XXX free id3_file ???
	//free (id3_tag); <- this causes a seg-fault
	
	return 0;
}

/**
 * Get the value of a certain element of an id3 tag.
 * @param *tag
 * 	The tag of an id3 song file.
 * @param *frame_name
 * 	The element, a tag name. See ID3_FRAME_.. in id3tag.h .
 * @return
 * 	The tags value as utf8 string (you get your own copy, do what you want
 * 	with that piece of data, but do not forget to free it).
 */
static char*
rem_tags_id3_get_tag(const struct id3_tag *tag, const char *frame_name)
{
	char* rtn;
	const id3_ucs4_t *string;
	struct id3_frame *frame;
	union id3_field *field;

	frame = id3_tag_findframe (tag, frame_name, 0);
	if (!frame) return NULL;

	if (frame_name == ID3_FRAME_COMMENT) {
		field = id3_frame_field (frame, 3);
	} else {
		field = id3_frame_field (frame, 1);
	}

	if (!field) return NULL;

	if (frame_name == ID3_FRAME_COMMENT) {
		string = id3_field_getfullstring (field); 
	} else {
		string = id3_field_getstrings (field, 0); 
	}

	if (!string) return NULL;

	if (frame_name == ID3_FRAME_GENRE)  {
		 string = id3_genre_name (string);
	}

	rtn = id3_ucs4_utf8duplicate (string);
	return rtn;
}

/**
 * Maps the tag names as used in Remuco to ID3 frame names.
 * @return
 * 	the corresponding frame name or NULL if no mapping is possible
 */
static const char*
rem_tags_id3_map_tag_to_frame_name(const char *tag_name)
{
	if (strcmp(tag_name, REM_TAG_NAME_ARTIST) == 0)
		return ID3_FRAME_ARTIST;
	if (strcmp(tag_name, REM_TAG_NAME_ALBUM) == 0)
		return ID3_FRAME_ALBUM;
	if (strcmp(tag_name, REM_TAG_NAME_COMMENT) == 0)
		return ID3_FRAME_COMMENT;
	if (strcmp(tag_name, REM_TAG_NAME_GENRE) == 0)
		return ID3_FRAME_GENRE;
	if (strcmp(tag_name, REM_TAG_NAME_TITLE) == 0)
		return ID3_FRAME_TITLE;
	if (strcmp(tag_name, REM_TAG_NAME_TRACK) == 0)
		return ID3_FRAME_TRACK;
	if (strcmp(tag_name, REM_TAG_NAME_LENGTH) == 0)
		return ID3_FRAME_TLEN;
	if (strcmp(tag_name, REM_TAG_NAME_YEAR) == 0)
		return ID3_FRAME_YEAR;
	
	return NULL;
}
