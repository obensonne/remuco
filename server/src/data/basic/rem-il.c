/**
 * Data type: Integer Vector
 * 
 * Offers methods to work on integer vectors.
 * 
 * The generel contract is that the elements in RemIntList are read only
 * and altering the vector should only happen via the offered functions.
 * 
 * The functions never return NULL !
 */
 
///////////////////////////////////////////////////////////////////////////////
//
// Includes
//
///////////////////////////////////////////////////////////////////////////////

#include "rem-il.h"

///////////////////////////////////////////////////////////////////////////////
//
// working with int vectors
//
///////////////////////////////////////////////////////////////////////////////

RemIntList*
rem_il_new()
{
	RemIntList *iv;
	
	iv = g_slice_new0(RemIntList);
	
	return iv;
}

RemIntList*
rem_il_new_with_values(const gint32 *vals, guint num)
{
	RemIntList *iv;
	guint u;
	
	iv = g_slice_new(RemIntList);
	
	iv->l = num;
	iv->v = g_malloc(sizeof(gint32) * iv->l); // no slice, cause v has no fixed size
	
	for (u = 0; u < num; u++) {
		iv->v[u] = vals[u];
	}
	
	return iv;
}

void
rem_il_append(RemIntList *iv, gint32 i)
{
	g_assert_debug(iv);
	
	iv->l++;
	iv->v = g_realloc(iv->v, iv->l * sizeof(gint32));
	
	iv->v[iv->l - 1] = i;
}

void
rem_il_clear(RemIntList *iv)
{
	if (!iv)
		return;

	if (iv->v) g_free(iv->v); // no slice, cause v has no fixed size
	iv->v = NULL;
	iv->l = 0;
}

void
rem_il_destroy(RemIntList *iv)
{
	if (!iv) return;
	
	if (iv->v) g_free(iv->v); // no slice, cause v has no fixed size
	g_slice_free(RemIntList, iv);
}

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

GByteArray*
rem_il_serialize(const RemIntList *iv)
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

RemIntList*
rem_il_unserialize(const GByteArray *ba)
{
	g_assert_debug(ba);
	
	RemIntList *iv;
	gint32 *i_nbo, i_hbo, *ba_end;
	
	if (ba->len % 4 != 0) {
		
		//LOG_WARN("data malformed - ignore last (broken) integer\n");
		//ba_end = (gint32*) (ba->data + ba->len - ba->len % 4);

		LOG_WARN("data malformed\n");
		return NULL;
		
	} else {
		
		ba_end = (gint32*) (ba->data + ba->len);
		
	}
	
	iv = rem_il_new();

	for (i_nbo = (gint32*) ba->data; i_nbo < ba_end; i_nbo++) {
		
		i_hbo = g_ntohl(*i_nbo);
		rem_il_append(iv, i_hbo);
		
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
rem_il_dump(const RemIntList *iv)
{
	guint u;
	
	REM_DATA_DUMP_HDR("RemIntList", iv);
	
	if (iv && iv->l) {
		
		for (u = 0; u < iv->l; u++) {
			REM_DATA_DUMP_FS("%i, ", iv->v[u]);
		}
	
		REM_DATA_DUMP_FS("\b\b");
	}
	
	REM_DATA_DUMP_FTR;	
}

gboolean
rem_il_assert_equals(RemIntList *iv1, RemIntList *iv2)
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




