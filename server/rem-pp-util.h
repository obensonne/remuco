#ifndef REMPPUTIL_H_
#define REMPPUTIL_H_

//////////////////////////////////////////////////////////////////////////////
//
// functions
//
//////////////////////////////////////////////////////////////////////////////

int
rem_song_set_unknown(struct rem_pp_song *song);

int
rem_song_append_tag(struct rem_pp_song *song, const char *name, const char *val);



#endif /*REMPPUTIL_H_*/