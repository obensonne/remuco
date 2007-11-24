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
// types
//
///////////////////////////////////////////////////////////////////////////////

struct _RemPlob {
	gchar			*pid;
	RemStringList	*meta;
	GByteArray		*img;
};

///////////////////////////////////////////////////////////////////////////////
//
// create and destroy a plob
//
///////////////////////////////////////////////////////////////////////////////

RemPlob*
rem_plob_new(const gchar *pid)
{
	RemPlob* plob;
	
	plob = g_slice_new0(RemPlob);
	
	plob->pid = g_strdup(pid);
	
	plob->meta = rem_sl_new();
	
	return plob;
}

RemPlob*
rem_plob_new_unknown(const gchar *pid)
{
	RemPlob *plob;
	
	plob = rem_plob_new(pid);
	rem_plob_meta_add_const(plob, REM_PLOB_META_TITLE, "No Info");
	rem_plob_meta_add_const(plob, REM_PLOB_META_ARTIST, "No Info");
	rem_plob_meta_add_const(plob, REM_PLOB_META_ALBUM, "No Info");
	
	return plob;
}

void
rem_plob_destroy(RemPlob *plob)
{
	if (!plob) return;
	
	g_assert_debug(!plob->img);
	
	if (plob->pid) g_free(plob->pid);
	
	rem_sl_destroy(plob->meta);
	
	g_slice_free(RemPlob, plob);
}

///////////////////////////////////////////////////////////////////////////////
//
// working with a plob
//
///////////////////////////////////////////////////////////////////////////////

void
rem_plob_meta_add(RemPlob *plob, gchar *name, gchar *value)
{
	g_return_if_fail(plob && name);
	
	rem_sl_append(plob->meta, name);
	
	if (value)
		rem_sl_append(plob->meta, value);
	else
		rem_sl_append_const(plob->meta, "");
}

void
rem_plob_meta_add_const(RemPlob *plob, const gchar *name, const gchar *value)
{
	g_return_if_fail(plob && name);
	
	rem_sl_append_const(plob->meta, name);
	rem_sl_append_const(plob->meta, value ? value : "");
}

guint
rem_plob_meta_num(const RemPlob *plob)
{
	g_return_val_if_fail(plob, 0);
	
	return rem_sl_length(plob->meta) / 2;
}

const gchar*
rem_plob_meta_get_name(const RemPlob *plob, guint index)
{
	g_return_val_if_fail(plob, NULL);
	
	return rem_sl_get(plob->meta, index * 2);
}

const gchar*
rem_plob_meta_get_value(const RemPlob *plob, guint index)
{
	g_return_val_if_fail(plob, NULL);
	
	return rem_sl_get(plob->meta, index * 2 + 1);
}

void
rem_plob_meta_iter_reset(const RemPlob *plob)
{
	g_return_if_fail(plob);
	
	rem_sl_iterator_reset(plob->meta);
}

void
rem_plob_meta_iter_next(const RemPlob *plob,
						const gchar **name,
						const gchar **value)
{
	g_return_if_fail(plob);
	
	*name = rem_sl_iterator_next(plob->meta);
	*value = rem_sl_iterator_next(plob->meta);
}


const gchar*
rem_plob_meta_get(const RemPlob *plob, const gchar *name)
{
	const gchar	*n, *v;
	
	g_return_val_if_fail(plob && name, "");

	rem_sl_iterator_reset(plob->meta);
	
	n = rem_sl_iterator_next(plob->meta);
	v = rem_sl_iterator_next(plob->meta);

	while(n) {
		
		if (g_str_equal(name, n)) return v;
		
		n = rem_sl_iterator_next(plob->meta);
		v = rem_sl_iterator_next(plob->meta);		
		
	};
	
	
	return ""; // 'name' not found
	
}

///////////////////////////////////////////////////////////////////////////////
//
// serialization
//
///////////////////////////////////////////////////////////////////////////////

static const guint RemPlob_bfv[] = {
	REM_BIN_DT_STR, 1,
	REM_BIN_DT_SV, 1,
	REM_BIN_DT_BA, 1,
	REM_BIN_DT_NONE
};


GByteArray*
rem_plob_serialize(const RemPlob *plob,
				   const gchar *se,
				   const RemStringList *pte,
				   guint img_width_max,
				   guint img_height_max)
{
	const gchar	*img_file;
	RemPlob	*plob_tmp;
	GByteArray	*ba;
	
	plob_tmp = (RemPlob*) plob;
	
	img_file = rem_plob_meta_get(plob, REM_PLOB_META_ART);
	
	if (img_file && img_file[0]) {
		
		plob_tmp->img = rem_img_get(img_file, img_width_max, img_height_max);
				
	}
	
	ba = rem_bin_serialize(plob_tmp, RemPlob_bfv, se, pte);
		
	if (plob->img) {
		g_byte_array_free(plob->img, TRUE);
		plob_tmp->img = NULL;
	}
	
	return ba;
	
}

RemPlob*
rem_plob_unserialize(const GByteArray *ba, const gchar *te)
{
	RemPlob *plob;
	guint ret;

	plob = NULL;
	ret = rem_bin_unserialize(
			ba, sizeof(RemPlob), RemPlob_bfv, (gpointer) &plob, te);

	if (ret < 0 && plob) {
		rem_plob_destroy(plob);
		plob = NULL;
	}

	if (plob->img) {
		g_byte_array_free(plob->img, TRUE);
		LOG_WARN("client send art image within plob");
		plob->img = NULL;
	}
	
	return plob;
}

///////////////////////////////////////////////////////////////////////////////
//
// debug
//
///////////////////////////////////////////////////////////////////////////////

void
rem_plob_dump(const RemPlob *p)
{
	REM_DATA_DUMP_HDR("RemPlob", p);
	
	if (p) {
		REM_DATA_DUMP_FS("pid = %s\n", p->pid);
		REM_DATA_DUMP_FS("meta information:\n");
		rem_sl_dump(p->meta);
		REM_DATA_DUMP_FS("\nimage data at %p (%u bytes)",
						 p->img, p->img ? p->img->len : 0);
	}
	REM_DATA_DUMP_FTR;
}

