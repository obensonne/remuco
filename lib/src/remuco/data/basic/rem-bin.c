#include <string.h>

#define REM_NEED_SERIALIZATION_FUNCTIONS

#include "rem-bin.h"
#include "rem-bin-bac.h"
#include "rem-iv.h"


/**
 * Data packet:
 * 
 * BYTES	COMMENT
 * 4		message id/type
 * 4		size of following data in bytes (s)
 * s		message data
 * 
 * Message data
 * 
 * For every data type in the message data:
 * BYTES	COMMENT
 * 4		basic data type
 * 4		basic data count
 * 4		size in bytes (s)
 * s		basic data
 * 
 * Basic data:
 * 
 * Int:
 * Concatenation of 4 byte integers in net byte order
 * 
 * String:
 * Concatenation of null terminated strings in host encoding
 * 
 * String vector:
 * Concatenation of:
 * 	- null terminated encoding string
 * 	- for every strin in the vector:
 * 		- 1 byte indicating if the string is null (0) or non-null (1)
 * 		- if the previous byte was 1, than now a null-terminated string
 * 		  in the encoding specified before
 * 
 * 
 * 
 * Serialized data
 * 
 * 
 */


/**
 * Append subadata to the byte array container.
 * 
 * @param bac
 * 	the byte array container
 * @param dt
 * 	type of the subdata
 * @param dc
 * 	subdata elements count
 * @param ds (in/out param)
 * 	pointer to wihtin the data structure where the subdata elements are
 * 	stored. on return this pointer will point to the data which is after
 * 	the sub data data elements that will be converted here.
 * @param se
 * 	character encoding used by the host
 * @param pte
 *	possible target character encodings to use to convert textial data 
 */
static void
rem_bin_append_data_to_bac(rem_bac_t *bac,
			   guint dt, guint dc,
			   gconstpointer *ds,
			   const gchar *se,
			   const rem_sv_t *pte)
{
	g_assert_debug(bac && ds);
	
	guint		size, u, size_pos;
	gint32		*ds_ptr_int, *ia_nbo, i, *i_ptr;
	gchar		**ds_ptr_s;
	rem_iv_t	**ds_ptr_iv;
	rem_sv_t	*sv;
	gpointer	*ds_ptr_gen;
	GByteArray	*ba, **ds_ptr_ba;
	
	const guint8	byte_null = 0;
	const guint8	byte_one = 1;

	ba = NULL;
	size = 0;
	
	switch (dt) {
		
	case REM_BIN_DT_INT:
	
		size = 4 * dc;
		rem_bac_prepare_append(bac, dt, dc, size);
		
		ds_ptr_int = (gint32*) *ds;
		ia_nbo = g_malloc(size);
		for (u = 0; u < dc; u++) {
			ia_nbo[u] = g_htonl(ds_ptr_int[u]);
		}
		g_byte_array_append(bac->ba, (guint8*) ia_nbo, size);
		
		g_free(ia_nbo);
		
		*ds += dc * sizeof(gint32);
		
		break;
		
	case REM_BIN_DT_STR:
		
		ds_ptr_s = (gchar**) *ds;
		
		sv = rem_sv_new();
		for (u = 0; u < dc; u++) {
			rem_sv_append(sv, ds_ptr_s[u]);
		}

		ba = rem_sv_serialize(sv, se, pte);
		
		rem_sv_destroy_body(sv);

		rem_bac_prepare_append(bac, dt, dc, ba->len);

		g_byte_array_append(bac->ba, ba->data, ba->len);

		g_byte_array_free(ba, TRUE); 

		*ds += dc * sizeof(gpointer);
		
		break;
		
	case REM_BIN_DT_IV:
		
		ds_ptr_iv = (rem_iv_t**) *ds;
		for (u = 0; u < dc; u++) {
			size += 1 + (ds_ptr_iv[u] ? 4 + ds_ptr_iv[u]->l * 4 : 0);
		}	
		rem_bac_prepare_append(bac, dt, dc, size);
		for (u = 0; u < dc; u++) {
			if (ds_ptr_iv[u]) {
				ba = rem_iv_serialize(ds_ptr_iv[u]);
				g_byte_array_append(bac->ba, &byte_one, 1);
				i = g_htonl(ba->len);
				g_byte_array_append(bac->ba, (guint8*) &i, 4);			
				g_byte_array_append(bac->ba, ba->data, ba->len);			
				g_byte_array_free(ba, TRUE);
			} else {
				g_byte_array_append(bac->ba, &byte_null, 1);			
			}		
		}

		*ds += dc * sizeof(gpointer);
		
		break;
		
	case REM_BIN_DT_SV:
		
		size = 0;
		size_pos = rem_bac_prepare_append(bac, dt, dc, size);
						// we will set true size later
						
		ds_ptr_gen = (gpointer*) *ds;
		for (u = 0; u < dc; u++) {
			//LOG_DEBUG("process sv no %u at %p\n", u, ds_ptr_gen[u]);
			if (ds_ptr_gen[u]) {
				sv = (rem_sv_t*) ds_ptr_gen[u];
				ba = rem_sv_serialize(sv, se, pte);
				size += 1 + 4 + ba->len;
				g_byte_array_append(bac->ba, &byte_one, 1);
				i = g_htonl(ba->len);
				g_byte_array_append(bac->ba, (guint8*) &i, 4);			
				g_byte_array_append(bac->ba, ba->data, ba->len);			
				g_byte_array_free(ba, TRUE);
			} else {
				g_byte_array_append(bac->ba, &byte_null, 1);			
				size += 1;
			}		
		}
		
		// now set the size after extending the bac (since we did not
		// know the size before)
		
		i_ptr = (gint32*) (bac->ba->data + size_pos);
		*i_ptr = (gint32) g_htonl(size);

		*ds += dc * sizeof(gpointer);
		
		break;
		
	case REM_BIN_DT_BA:
		
		ds_ptr_ba = (GByteArray**) *ds;
		for (u = 0; u < dc; u++) {
			size += 1 + (ds_ptr_ba[u] ? 4 + ds_ptr_ba[u]->len : 0);
		}	
		rem_bac_prepare_append(bac, dt, dc, size);
		for (u = 0; u < dc; u++) {
			if (ds_ptr_ba[u]) {
				ba = ds_ptr_ba[u];
				g_byte_array_append(bac->ba, &byte_one, 1);
				i = g_htonl(ba->len);
				g_byte_array_append(bac->ba, (guint8*) &i, 4);			
				g_byte_array_append(bac->ba, ba->data, ba->len);			
			} else {
				g_byte_array_append(bac->ba, &byte_null, 1);			
			}		
		}

		*ds += dc * sizeof(gpointer);
		
		break;
		
	case REM_BIN_DT_IGNORE:
		
		*ds += dc * sizeof(gpointer);
		
		break;
		
	default:
	
		g_assert_not_reached_debug();
		
		break;
	}
	
}

GByteArray*
rem_bin_serialize(gconstpointer ds,
		  const guint *bfv,
		  const gchar *se,
		  const rem_sv_t *pte)
{
	g_assert_debug(ds && bfv);
	
	guint bfv_len, u, dt, dc;
	GByteArray *ba;
	rem_bac_t *bac;
	gconstpointer ds_walker;
	
	ds_walker = ds;
	
	ba = g_byte_array_new();
	bac = rem_bac_new(ba);
	
	bfv_len = 0;
	while (bfv[bfv_len] != REM_BIN_DT_NONE) bfv_len++;
	
	g_assert_debug(bfv_len % 2 == 0);
	
	for (u = 0; u < bfv_len; u += 2) {
		
		dt = bfv[u];	// type of data
		dc = bfv[u+1]; 	// number of data elements
		
		g_assert_debug(dt < REM_BIN_DT_COUNT);
		g_assert_debug(dc);
		
		rem_bin_append_data_to_bac(
					bac, dt, dc, &ds_walker, se, pte);
	}
	
	rem_bac_destroy(bac);

	return ba;	
}

static gint
rem_bin_unserialize_sba(rem_bac_sba_t *sba,
			     gpointer *tds,
			     gpointer tds_end,
			     const gchar* te)
{
	gint32		*tds_walker_int;
	guint		u;
	guint8		*sba_walker, *sba_end, *ptr_flag;
	gpointer	*tds_walker;
	GByteArray	ba;
	rem_sv_t	*sv;
	
	LOG_NOISE("unserialize %u data elements of type %u stored at %p as %u "
		"bytes\n", sba->dc, sba->dt, sba->ba->data,  sba->ba->len);

	sba_walker = sba->ba->data;
	sba_end = sba->ba->data + sba->ba->len;
	
	switch (sba->dt) {
	case REM_BIN_DT_INT:
		
		if (*tds + sba->dc * sizeof(gint32) > tds_end) {
			LOG_WARN("to much data elements for ds\n");
			return -1;
		}
		
		tds_walker_int = (gint32*) *tds;
		for (u = 0; u < sba->dc && sba_walker < sba_end; u++) {
			*tds_walker_int = g_ntohl( *((gint32*) (sba_walker)) );
			sba_walker += 4;
			tds_walker_int++;
		}
		*tds = tds_walker_int;

		break;

	case REM_BIN_DT_STR:
		
		if (*tds + sba->dc * sizeof(gpointer) > tds_end) {
			LOG_WARN("to much data elements for ds\n");
			return -1;
		}
		
		sv = rem_sv_unserialize(sba->ba, te);
		
		sba_walker = sba_end;
		
		if (sba->dc != sv->l) {
			LOG_WARN("string count differs\n");
			rem_sv_destroy_body(sv);
		}
		
		tds_walker = *tds;
		for (u = 0; u < sba->dc; u++, tds_walker++) {

			*tds_walker = (gpointer) sv->v[u];
			
		}
		
		rem_sv_destroy_body(sv);
		
		*tds = tds_walker;

		break;

	case REM_BIN_DT_IV:
		
		if (*tds + sba->dc * sizeof(gpointer) > tds_end) {
			LOG_WARN("to much data elements for ds\n");
			return -1;
		}
		
		tds_walker = *tds;
		for (u = 0; u < sba->dc && sba_walker < sba_end; u++) {
			
			ptr_flag = sba_walker;
			if (*ptr_flag) {
				ba.data = sba_walker + 1 + 4;
				ba.len = g_ntohl(*((gint32*)(sba_walker + 1)));
				*tds_walker = rem_iv_unserialize(&ba);
				if (!tds_walker)
					return -1;
				sba_walker += 1 + 4 + ba.len;
			} else {
				*tds_walker = NULL;
				sba_walker += 1;
			}
			tds_walker++;
		}
		*tds = tds_walker;

		break;

	case REM_BIN_DT_SV:
		
		if (*tds + sba->dc * sizeof(gpointer) > tds_end) {
			LOG_WARN("to much data elements for ds\n");
			return -1;
		}
		
		tds_walker = *tds;
		for (u = 0; u < sba->dc && sba_walker < sba_end; u++) {
			
			ptr_flag = sba_walker;
			if (*ptr_flag) {
				ba.data = sba_walker + 1 + 4;
				ba.len = g_ntohl(*((gint32*)(sba_walker + 1)));
				sv = rem_sv_unserialize(&ba, te);
				if (!sv)
					return -1;
				*tds_walker = sv;
				sba_walker += 1 + 4 + ba.len;
			} else {
				*tds_walker = NULL;
				sba_walker += 1;
			}
			tds_walker++;
		}
		*tds = tds_walker;

		break;

	case REM_BIN_DT_BA:
		
		if (*tds + sba->dc * sizeof(gpointer) > tds_end) {
			LOG_WARN("to much data elements for ds\n");
			return -1;
		}
		
		tds_walker = *tds;
		for (u = 0; u < sba->dc && sba_walker < sba_end; u++) {
			
			ptr_flag = sba_walker;
			if (*ptr_flag) {
				ba.data = sba_walker + 1 + 4;
				ba.len = g_ntohl(*((gint32*)(sba_walker + 1)));
				*tds_walker = g_byte_array_sized_new(ba.len);
				g_byte_array_append((GByteArray*) *tds_walker,
							ba.data, ba.len);
				sba_walker += 1 + 4 + ba.len;
			} else {
				*tds_walker = NULL;
				sba_walker += 1;
			}
			tds_walker++;
		}
		*tds = tds_walker;

		break;

	case REM_BIN_DT_IGNORE:
		
		*tds += sba->dc;
		
		break;

	default:
	
		g_assert_not_reached_debug();
		
		break;
	}
	
	if (sba_walker != sba_end) {
		LOG_DEBUG("sba_walker, sba_end = %p, %p, %i\n",
				sba_walker, sba_end, sba_end - sba_walker);
		LOG_WARN("binary data malformed - to much or less bytes\n");
		return -1; 
	}
	
	return 0;
}

gint
rem_bin_unserialize(const GByteArray *ba,
			 guint tds_size,
			 const guint *bfv,
			 gpointer *tds,
			 const gchar *te)
{
	g_assert_debug(ba && bfv && tds_size && tds);
	
	rem_bac_t *bac;
	rem_bac_sba_t *sba;
	gpointer tds_walker, tds_end;
	gint ret;
	guint u, bfv_len, dt, dc;
	
	bac = rem_bac_new((GByteArray *) ba);
	
	*tds = g_malloc0(tds_size);
	tds_walker = *tds;
	tds_end = *tds + tds_size;
	
	bfv_len = 0;
	while (bfv[bfv_len] != REM_BIN_DT_NONE) bfv_len++;

	for (u = 0; u < bfv_len; u += 2) {
		
		dt = bfv[u];	// type of data
		dc = bfv[u+1]; 	// number of data elements
		
		g_assert_debug(dt < REM_BIN_DT_COUNT);
		g_assert_debug(dc);
		
		sba = rem_bac_next_sba(bac);
		
		if (!sba) {
			LOG_WARN("binary data too small\n");
			rem_bac_destroy(bac);
			return -1;
		}
		
		if (sba->dt != dt || sba->dc != dc) {
			LOG_WARN("binary data malformed - dt or dc differs\n");
			rem_bac_destroy(bac);
			return -1;
		}

		ret = rem_bin_unserialize_sba(
						sba, &tds_walker, tds_end, te);
		
		rem_sba_destroy(sba);
		
		if (ret < 0) {
			rem_bac_destroy(bac);
			return -1;
		}
	}
	
	rem_bac_destroy(bac);
	return 0;

}
