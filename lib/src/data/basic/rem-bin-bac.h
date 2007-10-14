#ifndef REMDATABAC_H_
#define REMDATABAC_H_

#include "../../util/rem-util.h"

#include "rem-sl.h"

typedef struct {
	GByteArray	*ba;
	guint8		dt, dc;
} rem_bac_sba_t;

typedef struct {
	GByteArray	*ba;
	guint8		*pos_ptr;
} rem_bac_t;

static inline rem_bac_t*
rem_bac_new(GByteArray *ba)
{
	g_assert_debug(ba);
	
	rem_bac_t *bac;
	
	bac = g_slice_new(rem_bac_t);
	
	bac->ba = ba;
	bac->pos_ptr = bac->ba->data;
	
	return bac;
}

static inline void
rem_bac_destroy(rem_bac_t *bac)
{
	g_slice_free(rem_bac_t, bac);		
}


static inline gboolean
rem_bac_has_more_sba(rem_bac_t *bac) {
	return bac->pos_ptr != bac->ba->data + bac->ba->len;
}	

static inline void
rem_bac_reset(rem_bac_t *bac)
{
	bac->pos_ptr = bac->ba->data;
}

static inline rem_bac_sba_t*
rem_bac_next_sba(rem_bac_t *bac)
{	
	rem_bac_sba_t *sba;
	guint dt, dc, size;
	gint32 i_nbo;
	
	if (bac->pos_ptr == bac->ba->data + bac->ba->len)
		return NULL;

	if (bac->pos_ptr + 6 > bac->ba->data + bac->ba->len) {
		LOG_WARN("uncomplete sba in bac\n");
		return NULL;
	}
	
	dt = *(bac->pos_ptr);
	bac->pos_ptr++;
	
	dc = *(bac->pos_ptr);
	bac->pos_ptr++;

	i_nbo = *((gint32*) bac->pos_ptr);
	size = g_ntohl(i_nbo);
	bac->pos_ptr += 4;
	
	if (bac->pos_ptr + size > bac->ba->data + bac->ba->len) {
		LOG_WARN("uncomplete sba in bac\n");
		return NULL;
	}		
	
	sba = g_slice_new(rem_bac_sba_t);
	
	sba->ba = g_byte_array_new();
	sba->ba->data = bac->pos_ptr;
	sba->ba->len = size;
	sba->dt =dt;
	sba->dc =dc;
	
	bac->pos_ptr += size;
	
	return sba;
}

static inline guint
rem_bac_prepare_append(rem_bac_t *bac, guint dt, guint dc, guint size)
{
	gint32 i_nbo;
	guint size_pos;
	guint8	u8;
	
	u8 = (guint8) dt;
	g_byte_array_append(bac->ba, &u8, 1);
	
	u8 = (guint8) dc;
	g_byte_array_append(bac->ba, &u8, 1);

	size_pos = bac->ba->len;
	
	i_nbo = g_htonl(size);
	g_byte_array_append(bac->ba, (guint8*) &i_nbo, 4);
	
	return size_pos;
}

static inline void
rem_bac_append_sba(rem_bac_t *bac, rem_bac_sba_t *sba)
{

	rem_bac_prepare_append(bac, sba->dt, sba->dc, sba->ba->len);
	
	g_byte_array_append(bac->ba, sba->ba->data, sba->ba->len);
}

static inline void
rem_sba_destroy(rem_bac_sba_t *sba)
{
	g_byte_array_free(sba->ba, FALSE);
	
	g_slice_free(rem_bac_sba_t, sba);
}
#endif /*REMDATABAC_H_*/
