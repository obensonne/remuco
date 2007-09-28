/**
 * Data type: Integer Vector
 * 
 * Offers methods to work on integer vectors.
 * 
 * The generel contract is that the elements in rem_iv_t are read only
 * and altering the vector should only happen via the offered functions.
 * 
 * The functions never return NULL !
 */
 
///////////////////////////////////////////////////////////////////////////////
//
// Includes
//
///////////////////////////////////////////////////////////////////////////////

#include "rem-iv.h"

///////////////////////////////////////////////////////////////////////////////
//
// working with int vectors
//
///////////////////////////////////////////////////////////////////////////////

rem_iv_t*
rem_iv_new()
{
	rem_iv_t *iv;
	
	iv = g_malloc0(sizeof(rem_iv_t));
	
	return iv;
}

rem_iv_t*
rem_iv_new_with_values(const gint32 *vals, guint num)
{
	rem_iv_t *iv;
	guint u;
	
	iv = g_malloc(sizeof(rem_iv_t));
	
	iv->l = num;
	iv->v = g_malloc(sizeof(gint32) * iv->l);
	
	for (u = 0; u < num; u++) {
		iv->v[u] = vals[u];
	}
	
	return iv;
}

void
rem_iv_append(rem_iv_t *iv, gint32 i)
{
	g_assert_debug(iv);
	
	iv->l++;
	iv->v = g_realloc(iv->v, iv->l * sizeof(gint32));
	
	iv->v[iv->l - 1] = i;
}

void
rem_iv_clear(rem_iv_t *iv)
{
	if (!iv)
		return;

	if (iv->v) g_free(iv->v);
	iv->v = NULL;
	iv->l = 0;
}

void
rem_iv_destroy(rem_iv_t *iv)
{
	if (!iv) return;
	
	if (iv->v) g_free(iv->v);
	g_free(iv);
}

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

GByteArray*
rem_iv_serialize(const rem_iv_t *iv)
{
	g_assert_debug(iv);

	GByteArray *ba;
	guint u;
	gint32 i_nbo;
	
	ba = g_byte_array_sized_new(4 * iv->l);
	
	for (u = 0; u < iv->l; u++) {
		
		i_nbo = g_htonl(iv->v[u]);
		g_byte_array_append(ba, (guint8*) &i_nbo, 4);
		
	}
	
	return ba;
}

rem_iv_t*
rem_iv_unserialize(const GByteArray *ba)
{
	g_assert_debug(ba);
	
	rem_iv_t *iv;
	gint32 *i_nbo, i_hbo, *ba_end;
	
	if (ba->len % 4 != 0) {
		
		//LOG_WARN("data malformed - ignore last (broken) integer\n");
		//ba_end = (gint32*) (ba->data + ba->len - ba->len % 4);

		LOG_WARN("data malformed\n");
		return NULL;
		
	} else {
		
		ba_end = (gint32*) (ba->data + ba->len);
		
	}
	
	iv = rem_iv_new();

	for (i_nbo = (gint32*) ba->data; i_nbo < ba_end; i_nbo++) {
		
		i_hbo = g_ntohl(*i_nbo);
		rem_iv_append(iv, i_hbo);
		
	}
	
	g_assert_debug(i_nbo == ba_end);
	
	return iv;
}

///////////////////////////////////////////////////////////////////////////////
//
// debug
//
///////////////////////////////////////////////////////////////////////////////

void
rem_iv_dump(const rem_iv_t *iv)
{
	guint u;
	
	LOG("rem_iv_t@%p: ", iv);
	
	if (!iv) { LOG("\n"); return; }
	
	for (u = 0; u < iv->l; u++) {
		LOG("%i, ", iv->v[u]);
	}
	
	LOG("\n");

}

gboolean
rem_iv_assert_equals(rem_iv_t *iv1, rem_iv_t *iv2)
{
	guint u;
	
	if (!iv1 && !iv2)
		return TRUE;
		
	if (!iv1 || !iv2)
		return FALSE;
	
	if (iv1->l != iv2->l) {
		return FALSE;
	}
	for (u = 0 ; u < iv1->l; u++) {
		if (iv1->v[u] != iv2->v[u])
			return FALSE;
	}
	return TRUE;
}




