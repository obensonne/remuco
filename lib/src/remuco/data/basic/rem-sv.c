/**
 * Data type: String Vector
 * 
 * Offers methods to work on string vectors.
 * 
 * The generel contract is that the elements in rem_sv_t are read only
 * and altering the vector should only happen via the offered functions.
 * 
 * The functions never return NULL !
 */
 
///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include "rem-sv.h"

///////////////////////////////////////////////////////////////////////////////
//
// encoding conversion
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Convert a string from one character encoding to another.
 * 
 * @param sf
 * 	the string to convert
 * @param ef
 * 	the encoding of sf
 * @param et
 * 	the encoding of the new string
 * 
 * @return
 * 	a copy of sf in another encoding (if the conversion fails, this string
 * 	may be an exact copy of sf, or it may be sf with some characters
 * 	replaced with an '?', in the case some characters are not supported
 * 	in the target encoding)
 * 
 */
static inline gchar*
rem_sv_convert_string(const gchar *sf, const gchar* se, const gchar* te)
{
	g_assert_debug(se && te);
	
	GError	*err;
	gchar	*st;

	if (!sf) return NULL;
	
	st = NULL;
	err = NULL;
	st = g_convert(sf, -1, te, se, NULL, NULL, &err);

	if (err && err->code == G_CONVERT_ERROR_ILLEGAL_SEQUENCE) {
		
		LOG_WARN("the string %s contains chars, which cannot be "
			"converted to %s -> i try to replace these chars with "
			"'?'\n", sf, te);
			
		if (st) g_free(st);
		
		g_error_free(err);
		err = NULL;
		st = g_convert_with_fallback(
				sf, -1, te, se, "?", NULL, NULL, &err);
				
	}

	if (err) {
		
		LOG_WARN("converting %s failed (%s) -> keep string "
			"unconverted\n", sf, err->message);
			
		if (st) g_free(st);
		st = g_strdup(sf);
		g_error_free(err);
		
	}

//	#if LOGLEVEL >= LL_NOISE
//	guint u,l;
//
//	LOG_NOISE("unconverted: ");
//	l = strlen(sf);
//	for (u = 0; u < l; u++) {
//		printf("%hhX ", sf[u]); 
//	} 
//	printf("\n");
//
//	LOG_NOISE("converted  : ");
//	l = strlen(st);
//	for (u = 0; u < l; u++) {
//		printf("%hhX ", st[u]); 
//	} 
//	printf("\n");
//	#endif
	
	return st;
			
}

/**
 * Creates a new string vector with another character encoding.
 * 
 * @param sv
 * 	the string vector to convert
 * @param ef
 * 	the encoding of the strings in sv
 * @param et
 * 	the encoding of the strings in the new string vector
 * 
 * @return
 * 	a copy of sv with with the contained strings encoded in et (note: if
 * 	encoding fails at some point, the new vector may contain strings
 * 	still in the old encoding)
 * 
 */
static rem_sv_t*
rem_sv_convert(const rem_sv_t *sv, const gchar* se, const gchar* te)
{
	guint u;
	gchar *st;
	rem_sv_t *sv_converted;
	
	g_assert_debug(se && te);

	LOG_NOISE("convert strings from %s to %s\n", se, te);
	
	if (!sv) return NULL;

	sv_converted = rem_sv_new();
	
	for (u = 0; u < sv->l; u++) {
		
		st = rem_sv_convert_string(sv->v[u], se, te);
		
		rem_sv_append(sv_converted, st);
		
	}
	
	return sv_converted;
}

static inline gboolean
rem_sv_convert_check(const rem_sv_t *sv,
			  const gchar *se,
			  const gchar *te)
{
	g_assert_debug(se && te);
	
	GError	*err;
	gchar	*st;
	guint	u;

	if (!sv || sv->l == 0) return TRUE;

	for (u = 0; u < sv->l; u++) {
		
		st = NULL;
		err = NULL;
		
		if (sv->v[u])
			st = g_convert(sv->v[u], -1, te, se, NULL, NULL, &err);
		else
			continue; 
		
		if (st) g_free(st);
		
		if (err) {
			g_error_free(err);
			return FALSE;
		}
	}
	
	return TRUE;
}

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

static inline gboolean
rem_sv_str_is_ascii(const gchar *s)
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

/**
 * Serializes a string vector that has a specific encoding.
 * 
 * @param sv
 * 	the string vector
 * @param enc
 * 	the encoding of the strings in the string vector
 * 
 * @return
 * 	a byte array representation of the string vector
 * 
 */ 
static GByteArray*
rem_sv_serialize_priv(const rem_sv_t *sv, const gchar *enc)
{
	g_assert_debug(sv);
	g_assert_debug(rem_sv_str_is_ascii(enc));
	
	GByteArray *ba;
	guint u;
	gchar *s;
	const guint8 byte_null = 0;
	const guint8 byte_one = 1;

	//LOG_DEBUG("allocating %u bytes\n", sv->size);
	ba = g_byte_array_sized_new(1 + strlen(enc) + 1 + sv->size_bin);
	
	g_byte_array_append(ba, &byte_one, 1);
	g_byte_array_append(ba, (guint8*) enc, strlen(enc) + 1); // the encoding

	for (u = 0; u < sv->l; u++) {
		s = sv->v[u];
		if (s) {
			g_byte_array_append(ba, &byte_one, 1);
			g_byte_array_append(ba, (guint8*) s, strlen(s) + 1);
		} else {
			g_byte_array_append(ba, &byte_null, 1);
		}			
	}

	g_assert_debug(ba->len == 1 + strlen(enc) + 1 + sv->size_bin);
	
	return ba;
}

/**
 * Serializes a string vector.
 * 
 * @param sv
 * 	the string vector
 * @param se
 * 	the encoding of the strings in the string vector
 * @param pte
 *	A list of possible encodings to use for the serialized string vector
 * 	(possible target encodings). The first enc in that list to which the
 * 	string vector can succussfully get converted will be used. If none of
 * 	the possible target encodings offers a 'clean' conversion, a fallback
 * 	conversion is done while replacing problematic characters with '?'.
 * 
 * @return
 * 	the serialized string vector
 */
GByteArray*
rem_sv_serialize(const rem_sv_t *sv,
		      const gchar *se,
		      const rem_sv_t *pte)
{
	g_assert_debug(sv);
	g_assert_debug(concl(pte, pte->l && pte->v[0][0] != 0));
	
	rem_sv_t *sve;
	gboolean ok;
	GByteArray *ba;
	guint u;

	// check is conversion is needed
	
	if (!pte) return rem_sv_serialize_priv(sv, se);

	for (u = 0; u < pte->l; u++) {
		if (g_str_equal(se, pte->v[u])) break;
	}
	
	if (u < pte->l)	return rem_sv_serialize_priv(sv, se);
	
	// if here, we have to convert
	
	LOG_NOISE("conversion needed (from %s), try target enc ", se);

	for (u = 0; u < pte->l; u++) {
		
		ok = rem_sv_convert_check(sv, se, pte->v[u]);
		
		#if LOGLEVEL >= LL_NOISE
		LOG("%s, ", pte->v[u]);
		#endif
		
		if (ok) {
			
			#if LOGLEVEL >= LL_NOISE
			LOG("ok\n");
			#endif
			
			sve = rem_sv_convert(sv, se, pte->v[u]);
			
			ba = rem_sv_serialize_priv(sve, pte->v[u]);
			
			rem_sv_destroy(sve);
			
			return ba;
		}
	}
	
	// if here, none of the possible target encs was ok to convert the
	// string vector from the given source enc -> use the first pte as a
	// fallback (with backdraw that some chars are misdisplayed)
	
	#if LOGLEVEL >= LL_NOISE
	LOG("all failed\n");
	#endif
	
	LOG_DEBUG("must convert from enc %s to fallback enc %s (with character "
		"replacement)\n", se, pte->v[0]);
	
	sve = rem_sv_convert(sv, se, pte->v[0]);
	
	ba = rem_sv_serialize_priv(sve, pte->v[0]);
	
	rem_sv_destroy(sve);
	
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
rem_sv_t*
rem_sv_unserialize(const GByteArray *ba, const gchar *te)
{
	gchar *s, *se;
	guint8* ba_end, *ptr_flag;
	rem_sv_t *sv, *sve;
	
	g_assert_debug(ba);

	ba_end = ba->data + ba->len;
	
	// do some checks

	if (ba->len == 0) {
		
		LOG_WARN("data malformed (no data)\n");
		return NULL;
	}	
	if (*(ba_end-1) != 0) {
				
		LOG_WARN("data malformed (last byte not zero)\n");
		return NULL;
	}
	
	se = (gchar*) (ba->data + 1);
	if (!ba->data[0] || !rem_sv_str_is_ascii(se)) {
				
		LOG_WARN("data malformed (no valid encoding specifier)\n");
		return NULL;
	}
	
	// unserialize	

	sv = rem_sv_new();
	
	ptr_flag = ba->data + 1 + strlen(se) + 1;
	s = (gchar*) (ptr_flag + 1);
	while (ptr_flag < ba_end) {
		
		if (*ptr_flag) {
			if (ptr_flag + 1 == ba_end) {
				LOG_WARN("data malformed (wrong structure)");
				rem_sv_destroy(sv);
				return NULL;
			}
			rem_sv_append(sv, g_strdup(s));
			ptr_flag += 1 + strlen(s) + 1;
		} else {
			rem_sv_append(sv, NULL);
			ptr_flag++;
		}
		
		s = (gchar*) (ptr_flag + 1);
		
	}

	g_assert_debug(ptr_flag == ba_end);

	// do encoding conversion if needed

	if (!te || g_str_equal(se, te)) return sv;
	
	sve = rem_sv_convert(sv, se, te);
	
	rem_sv_destroy(sv);
	
	return sve;
}

///////////////////////////////////////////////////////////////////////////////
//
// misc
//
///////////////////////////////////////////////////////////////////////////////


///////////////////////////////////////////////////////////////////////////////
//
// debug
//
///////////////////////////////////////////////////////////////////////////////

/**
 * Print out a string vector.
 * 
 * @param sv
 * 	the string vector
 * 
 */

void
rem_sv_dump(const rem_sv_t *sv)
{
	guint u;
	
	LOG("rem_sv_t@%p: ", sv);
	
	if (!sv) { LOG("\n"); return; }
	
	for (u = 0; u < sv->l; u++) {
		LOG("%s, ", sv->v[u]);
	}
	
	LOG("\n");
	
}

