///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include <glib/gstdio.h>	// for S_IRWXU
#include <errno.h>

#include "util.h"

#define REM_DEF_ENC	"UTF-8"

/** Only used for ht2sv(). */
struct svx_t {
	gchar		**sv;
	guint		len, iter;
	gboolean	copy;
};

///////////////////////////////////////////////////////////////////////////////
//
// private functions
//
///////////////////////////////////////////////////////////////////////////////

/**
 * @param force
 * 	if true, conversion will be done also on errors (in this case unsupported
 * 	characters get replaced with a fallback character); if false, conversion
 *  breaks on the first conversion error and returns NULL
 */ 
static gchar**
conv(gchar **src, const gchar *from, const gchar *to, gboolean force)
{
	gchar		**dst;
	guint		len, u;
	GError		*err;
	
	LOG_DEBUG("convert strings from %s to %s (%s)", from, to,
			  force ? "forced" : "optimistc");
	
	len = g_strv_length(src);
	
	dst = g_new0(gchar*, len + 1);
	
	err = NULL;
	
	if (force) {
		
		////////// convert with fallback characters //////////
		
		for (u = 0; u < len; u++) {
			
			err = NULL;
			dst[u] = g_convert_with_fallback(src[u], -1, to, from, "?",
											 NULL, NULL, &err);
			
			if (err) {
				LOG_WARN_GERR(err, "failed to convert '%s' from %s to %s",
						 src[u], from, to);
				dst[u] = g_strdup(src[u]);
			}
		}
		
	} else {
		
		////////// optimisitc convert, break on error  //////////		
		
		for (u = 0; u < len; u++) {
			
			dst[u] = g_convert(src[u], -1, to, from, NULL, NULL, NULL);
			
			if (!dst[u]) {
				g_strfreev(dst);
				return NULL;
			}
		}
	}
	
	return dst;
}

static inline gboolean
str_is_ascii(const gchar *s)
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

void
rem_util_dump_ba(GByteArray *ba)
{
	GString	*dump;
	guint	u;
	guint8	*walker, *ba_end;

	dump = g_string_sized_new(ba->len * 4);
	
	g_string_printf(dump, "Binary Data: %p (%u bytes)", ba->data, ba->len);
	ba_end = ba->data + ba->len;
	for (u = 0, walker = ba->data; walker < ba_end; u = (u+1) % 16, walker++) {
		if (u == 0) {
			g_string_append(dump, "\n");			
		}
		g_string_append_printf(dump, "%02hhX ", *walker);
	}
	
	LOG_DEBUG("%s", dump->str);
	
	g_string_free(dump, TRUE);
}

void
rem_util_dump(guint8 *data, guint len)
{
	GByteArray ba;
	ba.data = data;
	ba.len = len;
	rem_util_dump_ba(&ba);
}

void
rem_util_dump_sv(const gchar *prefix, gchar **sv)
{
	GString	*s;
	guint	u;
	
	if (!sv || !sv[0]) {
		LOG_DEBUG("");
		return;
	}
	
	s = g_string_sized_new(100);
	g_string_append(s, prefix);
	g_string_append(s, ": ");
	
	for (u = 0; sv[u]; u++) {
		
		g_string_append(s, sv[u]);
		if (sv[u+1])
			g_string_append(s, ", ");
	}
	
	LOG_DEBUG("%s", s->str);

	g_string_free(s, TRUE);
}

/**
 * Get a length limited version of a string vector.
 * 
 * @param sv
 * 		the string vector to truncate (if needed)
 * @param len
 * 		if the length of @p sv exceeds this, a truncated vector will be returned
 * @param sv_const
 * 		whether @p sv should be handled as constant data or not (if @a TRUE,
 * 		the returned vector will always be a new vector and @p sv will
 * 		not be freed - if @a FALSE, the returned vector will be either be @p sv
 * 		itself or a new vector - in that case @p sv will be freed)
 * @return
 * 		if truncation was done, a new string vector with length @p len + 1,
 * 		otherwise either @p sv itself ot a copy of @p sv (see above)  
 */
gchar**
rem_util_strv_trunc(gchar **sv, guint len, gboolean sv_const)
{
	guint len_sv, u;
	gchar **sv_new;
	
	if (!sv)
		return NULL;
	
	len_sv = g_strv_length(sv);
	
	if (len_sv <= len) {
		if (sv_const) {
			return g_strdupv(sv);
		} else {
			return sv;
		}
	}
	
	sv_new = g_new0(gchar*, len + 2);
	
	for (u = 0; u < len; u++) {
		sv_new[u] = g_strdup(sv[u]);
	}
	sv_new[len] = g_strdup("-- CUT --");
	
	if (!sv_const)
		g_strfreev(sv);
	
	return sv_new;
}

gboolean
rem_util_strv_equal_length(gchar **sv1, gchar **sv2)
{
	if ((!sv1 && sv2) || (sv1 && !sv2))
		return FALSE;
	
	if (sv1 == sv2)
		return TRUE;
	
	if (g_strv_length(sv1) != g_strv_length(sv2))
		return FALSE;
	
	return TRUE;
}


gboolean
rem_util_strv_equal(gchar **sv1, gchar **sv2)
{
	guint len1, len2, u;
	
	if ((!sv1 && sv2) || (sv1 && !sv2))
		return FALSE;
	
	if (sv1 == sv2)
		return TRUE;
	
	len1 = g_strv_length(sv1);
	len2 = g_strv_length(sv2);
	
	if (len1 != len2)
		return FALSE;
	
	for (u = 0; u < len1; u++)
		if (!g_str_equal(sv1[u], sv2[u]))
			return FALSE;
	
	return TRUE;
}

/**
 * @param[in]  src	strings to convert
 * @param[out] dst	will point to the converted string vector or will be
 * 					@NULL when conversion was not needed because @p from
 * 					equals @p to
 * @param[in]  from	encoding of the strings in @p src (may be @a NULL - in that
 *                  case UTF-8 will be used as the from-encoding)
 * @param[in]  to	encoding to convert to (may be @a NULL - in that
 *                  case UTF-8 will be used as the to-encoding)
 */
void
rem_util_conv_sv(gchar **src, gchar ***dst, const gchar *from, const gchar *to)
{
	g_assert(dst && !*dst);
	g_assert((to && to[0]) || !to);
	
	if (!src)
		return;

	to = to ? to : REM_DEF_ENC;
	
	from = from ? from : REM_DEF_ENC;
		
	////////// check if conversion is needed //////////

	if (g_str_equal(from, to))
		return;
	
	////////// try lossless conversion //////////
	
	*dst = conv(src, from, to, TRUE);
	
}

/**
 * @param[in]  src	string to convert
 * @param[out] dst	will point to the converted string or will be @NULL when
 *                  conversion was not needed because @p from equals @p to
 * @param[in]  from	encoding of the strings in @p src (may be @a NULL - in that
 *                  case UTF-8 will be used as the from-encoding)
 * @param[in]  to	encoding to convert to (may be @a NULL - in that
 *                  case UTF-8 will be used as the to-encoding)
 */
void
rem_util_conv_s(gchar *src, gchar **dst, const gchar *from, const gchar *to)
{
	gchar		*sv[2];
	gchar		**sv_dst;
	
	g_assert(dst && !*dst);
	
	if (!src)
		return;
	
	sv[0] = src;
	sv[1] = NULL;
	
	sv_dst = NULL;
	
	rem_util_conv_sv(sv, &sv_dst, from, to);
	
	if (sv_dst) {
		*dst = sv_dst[0];
		g_free(sv_dst);
	} else {
		*dst = NULL;
	}
}


/**
 * @param ht	a hash table (keys and values must be strings and must not be
 * 				@p NULL)
 * @param copy	if TRUE, the strings in the hash table get duplicated and
 * 				the returned string vector must be freed with g_strfreev(); if
 * 				FALSE, the returned vector only contains references to the
 * 				strings in the hash table and the vector must be freed with
 * 				g_free()
 * 
 * @return
 * 	A string vector in this format: (key1, value1, key2, value2, ..., NULL).
 */
gchar**
rem_util_ht2sv(GHashTable *ht, gboolean copy)
{
	GList		*lk, *lv, *elk, *elv;
	guint		len, u;
	gchar		*s, **sv;
	
	len = g_hash_table_size(ht);
	
	sv = g_new0(gchar*, 2 * len + 1);
	
	lk = g_hash_table_get_keys(ht);
	lv = g_hash_table_get_values(ht);
	
	for (elk = lk, elv = lv, u = 0; elk && elv;
		 elk = elk->next, elv = elv->next, u += 2) {
		
		s = (gchar*) elk->data;
		s = s ? s : "";
		sv[u] = copy ? g_strdup(s) : s;

		s = (gchar*) elv->data;
		s = s ? s : "";
		sv[u+1] = copy ? g_strdup(s) : s;
	}
	
	g_list_free(lk);
	g_list_free(lv);

	return sv;
}

/**
 * @param ht	a hash table (keys must be strings and must not be @p NULL)
 * @param copy	if TRUE, the strings in the hash table get duplicated and
 * 				the returned string vector must be freed with g_strfreev(); if
 * 				FALSE, the returned vector only contains references to the
 * 				strings in the hash table and the vector must be freed with
 * 				g_free()
 * 
 * @return
 * 	A string vector in this format: (key1, key2, ..., NULL).
 */
gchar**
rem_util_htk2sv(GHashTable *ht, gboolean copy)
{
	GList		*l, *el;
	guint		len, u;
	gchar		*s, **sv;
	
	len = g_hash_table_size(ht);
	
	sv = g_new0(gchar*, len + 1);
	
	l = g_hash_table_get_keys(ht);
	
	for (el = l, u = 0; el; el = el->next, u++) {
		
		s = (gchar*) el->data;
		s = s ? s : "";
		sv[u] = copy ? g_strdup(s) : s;
	}
	
	g_list_free(l);

	return sv;
}

gboolean
rem_util_s2b(const gchar *s)
{
	if (!s)
		return FALSE;
	
	return	g_str_equal(s, "true") || g_str_equal(s, "TRUE") ||
			g_str_equal(s, "yes") || g_str_equal(s, "YES") ||
			g_str_equal(s, "on") || g_str_equal(s, "ON") ||
			g_str_equal(s, "y") || g_str_equal(s, "Y");
}

rem_util_create_cache_dir(GError **err)
{
	gchar		*cache_dir;
	gboolean	ok;
	gint		ret;
	
	cache_dir = g_build_filename(g_get_user_cache_dir(), "remuco", NULL);
	
	ret = g_mkdir_with_parents(cache_dir, S_IRWXU);
	if (ret < 0) {
		g_set_error(err, 0, 0, "mkdir on '%s' failed: %s", cache_dir,
					g_strerror(errno));
	}
	
	g_free(cache_dir);
	
	return;	
}
