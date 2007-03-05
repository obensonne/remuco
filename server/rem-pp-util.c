#include <stdlib.h>

#include "rem-log.h"
#include "rem-pp.h"
#include "rem-util.h"
#include "rem-pp-util.h"
#include "rem.h"

int
rem_song_set_unknown(struct rem_pp_song *song)
{
	int i;
	char *str[4];
	memset(str, 0, 4 * sizeof(char*));
	for (i = 0; i < song->tag_count; i++) {
		free(song->tag_names[i]);
		song->tag_names[i] = NULL;
		free(song->tag_values[i]);
		song->tag_values[i] = NULL;
	}
	str[0] = malloc(strlen(REM_TAG_NAME_ARTIST) + 1);
	str[1] = malloc(strlen("The Unknown Artist") + 1);
	str[2] = malloc(strlen(REM_TAG_NAME_COMMENT) + 1);
	str[3] = malloc(strlen("Sorry, no song information available") + 1);
	if (!(str[0] && str[1] && str[2] && str[3])) {
		LOG_ERRNO("malloc failed\n");
		free(str[0]); free(str[1]); free(str[2]); free(str[3]);
		return -1;
	}
	sprintf(str[0], REM_TAG_NAME_ARTIST);
	sprintf(str[1], "The Unknown Artist");
	sprintf(str[2], REM_TAG_NAME_COMMENT);
	sprintf(str[3], "Sorry, no song information available");
	song->tag_values[0] = str[0];
	song->tag_names[0] = str[1];
	song->tag_values[1] = str[2];
	song->tag_names[1] = str[3];
	song->tag_count = 2;
	return 1;
}

int
rem_song_append_tag(struct rem_pp_song *song, const char *name, const char *val)
{
	REM_TEST(song, "song");
	
	if(song->tag_count + 1 >= REM_MAX_TAGS) {
		LOG_WARN("to much tags, discard new tag\n");
		return -1;
	}
	
	LOG_NOISE("append '%s'='%s'\n", name, val);

	song->tag_names[song->tag_count] = strdup(name);
	song->tag_values[song->tag_count] = strdup(val);

	if (!song->tag_names[song->tag_count] ||
					!song->tag_values[song->tag_count]) {
		LOG_ERRNO("strdup failed");
		return -2;
	}
		
	song->tag_count++;
	
	return song->tag_count;
}
