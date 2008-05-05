///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include <wand/magick-wand.h>

#include "img.h"

struct _RemImg {
	
	MagickWand	*wand;
	gchar		*scaled;
	
};

///////////////////////////////////////////////////////////////////////////////
//
// private functions
//
///////////////////////////////////////////////////////////////////////////////

static void
handle_wand_error(MagickWand *wand)
{
	gchar			*msg;
	ExceptionType	severity;
	
	msg = MagickGetException(wand, &severity);
	
	LOG_ERROR("%s %s %ld %s", GetMagickModule(), msg);
	
	msg = (gchar *) MagickRelinquishMemory(msg);

//	MagickClearException(wand);
	
	ClearMagickWand(wand);
}

static GByteArray*
file_to_ba(const gchar *file)
{
	g_assert(file);
	
	GError		*err;
	GByteArray	*ba;
	gchar		*data;
	gsize		length;
	
	data = NULL;
	err = NULL;
	g_file_get_contents(file, &data, &length, &err);
	
	if (err) {
		LOG_WARN_GERR(err, "failed to read '%s'", file);
		return NULL;
	}
	
	g_assert(data);

	if (!length) {
		LOG_WARN("file '%s' is empty", file);
		return NULL;
	}
	
	ba = g_byte_array_new();
	ba->data = (guint8*) data;
	ba->len = length;
	
	return ba;
}

///////////////////////////////////////////////////////////////////////////////
//
// public functions
//
///////////////////////////////////////////////////////////////////////////////

/** Returns never NULL */
RemImg*
rem_img_up(const gchar *type)
{
	RemImg	*ri;
	gchar	*basename;
	
	ri = g_slice_new0(RemImg);
	
	MagickWandGenesis();
	
	ri->wand = NewMagickWand();
	
	//MagickSetCompressionQuality(ri->wand, 0);
	
	basename = g_strdup_printf("scaled.%s", type);
	
	ri->scaled = g_build_filename(g_get_user_cache_dir(), "remuco", basename,
								  NULL);
	
	g_free(basename);
	
	return ri;
}

void
rem_img_down(RemImg *ri)
{
	if (!ri)
		return;
	
	if (ri->wand) {
		DestroyMagickWand(ri->wand);  // we are done with Magick
	}
	
	MagickWandTerminus();		

	g_free(ri->scaled);
	
	g_slice_free(RemImg, ri);
}

GByteArray*
rem_img_get(RemImg *ri, const gchar *file, guint width_max, guint height_max)
{
	MagickBooleanType	magick_ret;
	GByteArray			*ba;
	gfloat				scale;
	gulong				width_orig, height_orig;
	guint				width_target, height_target;
 	
	g_assert(file);
	g_assert(file[0]);

	////////// init Wand and read image //////////

	LOG_DEBUG("load image '%s'", file);
	
	magick_ret = MagickReadImage(ri->wand, file);
	if (magick_ret == MagickFalse) {
				
		LOG_WARN("load image '%s' failed", file);
		handle_wand_error(ri->wand);
		return NULL;
		
	}

	////////// resize the image //////////

	LOG_DEBUG("get dimensions..");

	MagickSetFirstIterator(ri->wand);

	width_orig = MagickGetImageWidth(ri->wand);
	height_orig = MagickGetImageHeight(ri->wand);

	if (width_orig == 0 || height_orig == 0) {
		LOG_WARN("image '%s' has 0 size", file);
		handle_wand_error(ri->wand);
		return NULL;
	}

	LOG_DEBUG("dimensions: %lu x %lu", width_orig, height_orig);

	scale = MAX((gfloat) width_orig / width_max,
				(gfloat) height_orig / height_max);
	
	width_target = width_orig / scale;
	height_target = height_orig / scale;
	
	LOG_DEBUG("scale, "
			  "width_orig, width_target, width_max, "
			  "height_orig, height_target, height_max == "
			  "%f, %u, %u, %u, %u ,%u, %u",
			  scale,
			  (guint) width_orig, width_target, width_max,
			  (guint) height_orig, height_target, height_max);
	
	g_assert(width_target <= width_max && height_target <= height_max);

	magick_ret = MagickResizeImage(ri->wand, width_target, height_target,
								   LanczosFilter, 1.0);
	
	if (magick_ret == MagickFalse) {
		
		LOG_WARN("failed to scale image '%s'", file);
		
		handle_wand_error(ri->wand);
		
		return NULL;
	}
	
	////////// temporary write the resized image as PNG //////////
	
	magick_ret = MagickWriteImages(ri->wand, ri->scaled, MagickTrue);
	
	if (magick_ret == MagickFalse) {
		
		LOG_WARN("failed to write '%s'", ri->scaled);
		
		handle_wand_error(ri->wand);
		
		return NULL;
	}

	ClearMagickWand(ri->wand);
//	MagickRemoveImage(ri->wand);
	
	////////// load the temporary image into a byte array //////////
	
	ba = file_to_ba(ri->scaled);
	
	return ba;
	
}

