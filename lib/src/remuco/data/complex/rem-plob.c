///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include "rem-plob.h"
#include "../../util/rem-img.h"
#include "../basic/rem-bin.h"

///////////////////////////////////////////////////////////////////////////////
//
// create and destroy a plob
//
///////////////////////////////////////////////////////////////////////////////

rem_plob_t*
rem_plob_new(gchar *pid)
{
	rem_plob_t* p;
	
	p = g_malloc(sizeof(rem_plob_t));
	
	p->pid = pid;
	
	p->meta = rem_sv_new();
	
	p->img = NULL;
	
	return p;
}

rem_plob_t*
rem_plob_new_unknown(const gchar *pid)
{
	rem_plob_t *p;
	
	p = rem_plob_new(g_strdup(pid));
	rem_plob_meta_add(p, g_strdup(REM_PLOB_META_TITLE), g_strdup("No Info"));
	rem_plob_meta_add(p, g_strdup(REM_PLOB_META_ARTIST), g_strdup("No Info"));
	rem_plob_meta_add(p, g_strdup(REM_PLOB_META_ALBUM), g_strdup("No Info"));
	
	return p;
}

void
rem_plob_destroy(rem_plob_t *p)
{
	if (!p) return;
	
	if (p->pid) g_free(p->pid);
	
	rem_sv_destroy(p->meta);
	
	if (p->img) g_free(p->img);
	
	g_free(p);
}

///////////////////////////////////////////////////////////////////////////////
//
// working with a plob
//
///////////////////////////////////////////////////////////////////////////////

/**
 * @param mtn
 * 	meta information tag name
 * @param mtv
 * 	meta information tag value
 */
void
rem_plob_meta_add(rem_plob_t *p, gchar *mtn, gchar *mtv)
{
	g_assert(p && mtn && mtv);
	
	rem_sv_append(p->meta, mtn);
	rem_sv_append(p->meta, mtv);
}

G_CONST_RETURN gchar*
rem_plob_meta_get(rem_plob_t *p, const gchar *mtn)
{
	g_assert(p && mtn);
	
	guint u;
	
	for (u = 0; u < p->meta->l; u += 2)
		
		if (g_str_equal(mtn, p->meta->v[u]))
		
			return p->meta->v[u + 1];
	
	return "";
	
}

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

static const guint rem_data_medel_t_bfv[] = {
	REM_BIN_DT_STR, 1,
	REM_BIN_DT_SV, 1,
	REM_BIN_DT_BA, 1,	// actually a string, but before serialization
				// this pointer is set to a GByteArray
	REM_BIN_DT_NONE
};


GByteArray*
rem_plob_serialize(const rem_plob_t *plob,
		   const gchar *se,
		   const rem_sv_t *pte,
		   guint img_width_max,
		   guint img_height_max)
{
	gchar		*img_file;
	GByteArray	*ba;
	
	
	if (plob->img) {
		
		// temporary replace img file by the resized img data
		
		img_file = plob->img;
		
		((rem_plob_t*) plob)->img = (gchar*) rem_img_get(
				img_file, img_width_max, img_height_max);
				
		ba = rem_bin_serialize(plob, rem_data_medel_t_bfv, se, pte);
		
		if (plob->img)
			g_byte_array_free((GByteArray*) plob->img, TRUE);
	
		((rem_plob_t*) plob)->img = img_file;

	} else {
		
		ba = rem_bin_serialize(plob, rem_data_medel_t_bfv, se, pte);
		
	}
	
	return ba;
	
}

rem_plob_t*
rem_plob_unserialize(const GByteArray *ba, const gchar *te)
{
	rem_plob_t *plob;
	guint ret;

	plob = NULL;
	ret = rem_bin_unserialize(ba, sizeof(rem_plob_t),
				rem_data_medel_t_bfv, (gpointer) &plob, te);

	if (ret < 0 && plob) {
		rem_plob_destroy(plob);
		plob = NULL;
	}

	g_assert(plob->img == NULL); // plobs from client have no img data
	
	return plob;
}

///////////////////////////////////////////////////////////////////////////////
//
// debug
//
///////////////////////////////////////////////////////////////////////////////

void
rem_plob_dump(const rem_plob_t *p)
{
	DUMP_HDR("rem_plob_t", p);
	
	LOG("pid = %s\n", p->pid);
	LOG("meta information:\n");
	rem_sv_dump(p->meta);
	LOG_INFO("image = '%s'\n", p->img);
	
	DUMP_FTR;
}

