#include "rem-sl.h"
#include "../../util/rem-util.h"

struct _RemStringList {
	GStringChunk	*chunk;
	GSList			*strings;
	GSList			*strings_last; // to avoid list iteration on append
	GSList			*strings_iterator;
	GSList			*strings_to_free;
	guint			len;
};

///////////////////////////////////////////////////////////////////////////////
//
// private functions: encoding conversion
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Creates a new string list with another character encoding.
 * 
 * @param sl
 * 	the string list to convert
 * @param ef
 * 	the encoding of the strings in 'sl'
 * @param et
 * 	the encoding of the strings in the new string list
 * @param force
 * 	if true, conversion will be done also on errors (in this case unsupported
 * 	characters get replaced with a fallback character); if false, conversion
 *  breaks on the first conversion error and returns NULL 
 * 
 * @return
 * 	a new string list in the target encoding
 * 	if 'force' is true, this may be NULL (when an error occred)
 * 
 */
static RemStringList*
priv_convert(const RemStringList *sl,
			 const gchar* ef,
			 const gchar* et,
			 gboolean force)
{
	gchar		*sf, *st;
	RemStringList	*sl_converted;
	GSList		*l;
	GError		*err;
	
	g_assert_debug(ef && et);

	LOG_NOISE("convert strings from %s to %s", ef, et);
	
	if (!sl) return NULL;

	sl_converted = rem_sl_new();
	
	l = sl->strings;
	err = NULL;
	
	if (force) {
		
		////////// convert with fallback characters //////////
		
		while (l) {
			
			sf = (gchar*) l->data;
			if (!sf) rem_sl_append(sl_converted, NULL);
			
			st = g_convert_with_fallback(sf, -1, et, ef, "?", NULL, NULL, &err);
			
			if (err) {
				
				g_assert_debug(!st);
				LOG_WARN("'%s' failed (%s) -> simply copy", sf, err->message);
				g_error_free(err);
				err = NULL;
				rem_sl_append_const(sl_converted, sf);
				
			} else {
				
				rem_sl_append(sl_converted, st);
				
			}
			
			l = g_slist_next(l);
		}
		
	} else {
		
		////////// optimisitc convert, break on error  //////////		
		
		while (l) {
			
			sf = (gchar*) l->data;
			if (!sf) rem_sl_append(sl_converted, NULL);

			st = g_convert(sf, -1, et, ef, NULL, NULL, NULL);
			
			if (st) {
				
				rem_sl_append(sl_converted, st);
				
			} else {
				
				rem_sl_destroy(sl_converted);
				return NULL;
				
			}
			
			l = g_slist_next(l);
		}
		
	}
	
	
	return sl_converted;
}

static inline gboolean
priv_str_is_ascii(const gchar *s)
{
	GError *err = NULL;
	gchar *st;
	st = g_convert(s, -1, "ASCII", "ASCII", NULL, NULL, &err);
	if (st) g_free(st);
	if (err) {
		g_error_free(err);
		return FALSE;
	} else {
		return TRUE;
	}
}


///////////////////////////////////////////////////////////////////////////////
//
// public functions
//
///////////////////////////////////////////////////////////////////////////////

RemStringList*
rem_sl_new()
{
	RemStringList	*sl;
	
	sl = g_slice_new0(RemStringList);
	
	sl->chunk = g_string_chunk_new(500);
	
	return sl;
}

void
rem_sl_append_const(RemStringList *sl, const gchar *str)
{
	gchar	*str_chunked = NULL;
	GSList	*l;

	rem_api_check(sl, "RemStringList is NULL");
	
	////////// 'chunk' the string //////////
	
	if (str) str_chunked = g_string_chunk_insert(sl->chunk, str);
	
	////////// append while avoiding list iteration //////////

	l = g_slist_append(NULL, str_chunked);
	
	if (sl->strings_last)
		sl->strings_last->next = l;
	else
		sl->strings = l;
	
	sl->strings_last = l;
	
	sl->len++;
}

void
rem_sl_append(RemStringList *sl, gchar *str)
{
	GSList	*l;

	rem_api_check(sl, "RemStringList is NULL");
	
	////////// remember the string to free it later //////////
	
	if (str) sl->strings_to_free = g_slist_prepend(sl->strings_to_free, str);

	////////// append while avoiding list iteration //////////
	
	l = g_slist_append(NULL, str);
	
	if (sl->strings_last)
		sl->strings_last->next = l;
	else
		sl->strings = l;
	
	sl->strings_last = l;
	
	sl->len++;
}

void
rem_sl_clear(RemStringList *sl)
{
	GSList *l;

	rem_api_check(sl, "RemStringList is NULL");
	
	g_string_chunk_free(sl->chunk);
	
	sl->chunk = g_string_chunk_new(500);
	
	if (sl->strings) g_slist_free(sl->strings);
	
	sl->strings = NULL;
	sl->strings_last = NULL;
	sl->strings_iterator = NULL;
	
	if (sl->strings_to_free) {
		
		l = sl->strings_to_free;
		while (l) {
			g_free(l->data);
			l = g_slist_next(l);
		}
		
		g_slist_free(sl->strings_to_free);
	}
	
	sl->strings_to_free = NULL;

	sl->len = 0;

}

void
rem_sl_destroy(RemStringList *sl)
{
	GSList *l;
	
	if (!sl) return;
	
	g_string_chunk_free(sl->chunk);
	
	if (sl->strings) g_slist_free(sl->strings);
	
	if (sl->strings_to_free) {
		
		l = sl->strings_to_free;
		while (l) {
			g_free(l->data);
			l = g_slist_next(l);
		}
		
		g_slist_free(sl->strings_to_free);
	}

	g_slice_free(RemStringList, sl);

}

void
rem_sl_iterator_reset(const RemStringList *sl)
{
	rem_api_check(sl, "RemStringList is NULL");
	
	((RemStringList *) sl)->strings_iterator = sl->strings;
}

const gchar*
rem_sl_iterator_next(const RemStringList *sl)
{
	const gchar	*s;
	
	rem_api_check(sl, "RemStringList is NULL");
	
	if (!sl->strings_iterator) return NULL;
	
	s = (gchar*) sl->strings_iterator->data;
	
	if G_UNLIKELY(!s) LOG_WARN("iterator fails on NULL elements");
	
	((RemStringList *) sl)->strings_iterator = sl->strings_iterator->next;
	
	return s;
	
}

const gchar*
rem_sl_get(const RemStringList *sl, guint index)
{
	GSList	*l;
	
	rem_api_check(sl, "sl is NULL");
	
	l = g_slist_nth(sl->strings, index);
	
	rem_api_check(l, "index out of bounds");
	
	return (gchar*) l->data;
}

guint
rem_sl_length(const RemStringList *sl)
{
	rem_api_check(sl, "RemStringList is NULL");
	
	return sl->len;
}

guint
rem_sl_hash(const RemStringList *sl)
{
	GSList		*l;
	guint		hash, u;
	gchar		*s;
	
	if (!sl || !sl->strings) return 0;
	
	hash = sl->len;
	u = 0;
	
	l = sl->strings;
	while (l) {
		s = (gchar*) l->data;
		//hash ^= ((s ? g_str_hash(s) : 0) * u);
		hash = (hash << 5) - hash + (s ? g_str_hash(s) : 0);
		u++;
		l = g_slist_next(l);
	}
	
	return hash;
}

gboolean
rem_sl_equal(const RemStringList *sl1, const RemStringList *sl2)
{
	gchar	*s1, *s2;
	GSList	*l1, *l2;
	
	if (sl1 == sl2) return TRUE;
	if (sl1 == NULL || sl2 == NULL) return FALSE;
	if (sl1->len != sl2->len) return FALSE;
	if (sl1->len == 0 && sl2->len == 0) return TRUE;
	
	l1 = sl1->strings;
	l2 = sl2->strings;
	
	do {
		
		s1 = (gchar*) l1->data;
		s2 = (gchar*) l2->data;
		
		if (s1 == s2) continue;
		if (s1 == NULL || s2 == NULL) return FALSE;
		if (!g_str_equal(s1, s2)) return FALSE;
		
		l1 = l1->next;
		l2 = l2->next;
		
	} while (l1); // sl1 and sl1 are qeual in length, so l1 != 0 <=> l2 != 0
	
	return TRUE;
	
}

RemStringList*
rem_sl_copy(const RemStringList *sl)
{
	GSList *l;
	RemStringList	*sl_copy;
	
	if (!sl) return NULL;
	
	sl_copy = rem_sl_new();
	
	l = sl->strings;
	while (l) {
		rem_sl_append_const(sl_copy, (gchar*) l->data);
		l = g_slist_next(l);
	}
	
	return sl_copy;
}

void
rem_sl_dump(const RemStringList *sl)
{
	GSList	*l;
	
	REM_DATA_DUMP_HDR("RemStringList", sl);
	
	if (sl && sl->len) {
		
		l = sl->strings;
		while (l) {
			REM_DATA_DUMP_FS("%s, ", (gchar*) l->data);
			l = g_slist_next(l);
		}
	
		REM_DATA_DUMP_FS("\b\b");
	}
	
	REM_DATA_DUMP_FTR;	
}

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Like rem_sl_destroy(), but does not free the strings added with
 * rem_sl_append().
 * Used internally and not visible to a player proxy.
 */
void
rem_sl_destroy_keep_strings(RemStringList *sl)
{
	if (!sl) return;
	
	g_string_chunk_free(sl->chunk);
	
	if (sl->strings) g_slist_free(sl->strings);
	
	g_slist_free(sl->strings_to_free);

	g_slice_free(RemStringList, sl);
}

/**
 * Compute the size of all strings (including null-pointer flag and terminating
 * null)
 */
static gsize
priv_serialized_size(const RemStringList *sl)
{
	GSList		*l;
	gsize		size;
	gchar		*s;
	
	if (!sl || !sl->strings) return 0;
	
	size = 0;
	
	l = sl->strings;
	while (l) {
		s = (gchar*) l->data;
		size += 1 + (s ? strlen(s) + 1 : 0);
		l = g_slist_next(l);
	}
	
	return size;
}

/**
 * Serializes a string list that has a specific encoding.
 * 
 * @param sl
 * 	the string list
 * @param enc
 * 	the encoding of the strings in the string list
 * 
 * @return
 * 	a byte array representation of the string list
 * 
 */ 
static GByteArray*
priv_serialize(const RemStringList *sl, const gchar *enc)
{
	g_assert_debug(sl);
	g_assert_debug(priv_str_is_ascii(enc));
	
	GByteArray		*ba;
	GSList			*l;
	gchar			*s;
	guint			size;
	const guint8	byte_null = 0;
	const guint8	byte_one = 1;

	size = 1 + strlen(enc) + 1 + priv_serialized_size(sl);
	ba = g_byte_array_sized_new(size);
	
	g_byte_array_append(ba, &byte_one, 1); // null pointer flag for encoding
	g_byte_array_append(ba, (guint8*) enc, strlen(enc) + 1); // the encoding

	l = sl->strings;
	while (l) {
		s = (gchar*) l->data;
		if (s) {
			g_byte_array_append(ba, &byte_one, 1);
			g_byte_array_append(ba, (guint8*) s, strlen(s) + 1);
		} else {
			g_byte_array_append(ba, &byte_null, 1);
		}			
		l = g_slist_next(l);
	}

	g_assert_debug(ba->len == size);
	
	return ba;
}

/**
 * Serializes a string list.
 * 
 * @param sl
 * 	the string list
 * @param ef
 * 	the encoding of the strings in the string list
 * @param pte
 *	A list of possible encodings to use for the serialized string list
 * 	(possible target encodings). The first enc in that list to which the
 * 	string list can succussfully get converted will be used. If none of
 * 	the possible target encodings offers a 'clean' conversion, a fallback
 * 	conversion is done while replacing problematic characters with '?'.
 * 
 * @return
 * 	the serialized string list
 */
GByteArray*
rem_sl_serialize(const RemStringList *sl, const gchar *ef, const RemStringList *pte)
{
	g_assert_debug(sl);
	g_assert_debug(ef);
	g_assert_debug(concl(pte, pte->strings && pte->strings->data));
	
	RemStringList	*sl_converted;
	gchar		*et;
	GByteArray	*ba;
	GSList		*l;

	////////// check if conversion is needed //////////
	
	if (!pte) return priv_serialize(sl, ef);

	l = pte->strings;
	while (l) {
		if (l->data && g_str_equal(ef, (gchar*) l->data)) break;
		l = g_slist_next(l);
	}
	
	if (l) return priv_serialize(sl, ef); // 'ef' is in 'pte' !
	
	////////// we must convert //////////
	
	LOG_NOISE("conversion needed (from %s)", ef);

	l = pte->strings;
	
	while (l) {
		
		et = (gchar*) l->data;
		
		sl_converted = priv_convert(sl, ef, et, FALSE);
		
		if (sl_converted) {
			
			ba = priv_serialize(sl_converted, et);
			
			rem_sl_destroy(sl_converted);
			
			return ba;
		}
		
		l = g_slist_next(l);
	}
	
	////////// must convert with fallback characters //////////
	
	// if here, none of the possible target encs was ok to convert the
	// string list from the given source enc -> use the first pte as a
	// fallback (with backdraw that some chars are misdisplayed)
	
	et = (gchar*) pte->strings->data;
	
	LOG_DEBUG("must convert from enc %s to fallback enc %s (with character "
		"replacement)", ef, et);
	
	sl_converted = priv_convert(sl, ef, et, TRUE);
	
	ba = priv_serialize(sl_converted, et);
	
	rem_sl_destroy(sl_converted);
	
	return ba;
	
}

/**
 * Unserializes a string vector (in byte array representation).
 * 
 * @param ba
 * 	the string vector as byte array representation
 * 
 * @return
 * 	the unserialized string vector
 * 
 */ 
RemStringList*
rem_sl_unserialize(const GByteArray *ba, const gchar *et)
{
	gchar *s, *ef;
	guint8* ba_end, *ptr_flag;
	RemStringList *sl, *sl_converted;
	
	g_assert_debug(ba);

	ba_end = ba->data + ba->len;
	
	////////// do some checks //////////

	// data present ?
	if (ba->len == 0) {
		
		LOG_WARN("data malformed (no data)");
		return NULL;
	}
	
	// data null-terminated ?
	if (*(ba_end-1) != 0) {
				
		LOG_WARN("data malformed (last byte not zero)");
		return NULL;
	}
	
	// valid encoding ?
	ef = (gchar*) (ba->data + 1);
	if (!ba->data[0] || !priv_str_is_ascii(ef)) {
				
		LOG_WARN("data malformed (no valid encoding specifier)");
		return NULL;
	}
	
	////////// unserialize //////////	

	sl = rem_sl_new();
	
	ptr_flag = ba->data + 1 + strlen(ef) + 1;
	
	s = (gchar*) (ptr_flag + 1);
	
	while (ptr_flag < ba_end) {
		
		if (*ptr_flag) {	// there is a string
			
			if (ptr_flag + 1 == ba_end) {
				LOG_WARN("data malformed (wrong structure)");
				rem_sl_destroy(sl);
				return NULL;
			}
			rem_sl_append_const(sl, s);
			ptr_flag += 1 + strlen(s) + 1;
			
		} else {			// null, no string
			
			rem_sl_append(sl, NULL);
			ptr_flag++;
		}
		
		s = (gchar*) (ptr_flag + 1);
		
	}

	g_assert_debug(ptr_flag == ba_end);

	////////// do encoding conversion if needed //////////

	if (!et || g_str_equal(ef, et)) return sl;
	
	sl_converted = priv_convert(sl, ef, et, TRUE);
	
	rem_sl_destroy(sl);
	
	return sl_converted;
}
