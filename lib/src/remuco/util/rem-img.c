///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include <wand/magick-wand.h>

#include "rem-img.h"

///////////////////////////////////////////////////////////////////////////////
//
// private functions
//
///////////////////////////////////////////////////////////////////////////////

static void
rem_img_handle_wand_error(MagickWand *mw)
{
	gchar		*msg;
	ExceptionType	severity;
	
	msg = MagickGetException(mw, &severity);
	
	LOG_ERROR("%s %s %ld %s\n", GetMagickModule(), msg);
	
	msg = (gchar *) MagickRelinquishMemory(msg);
 
 	DestroyMagickWand(mw);
	MagickWandTerminus();
}

static GByteArray*
rem_img_file_to_ba(const gchar *file)
{
	g_assert_debug(file);
	
	GError		*err;
	GByteArray	*ba;
	gchar		*data;
	gsize		length;
	
	data = NULL;
	err = NULL;
	g_file_get_contents(file, &data, &length, &err);
	
	if (err) {
		LOG_WARN("could not read file '%s' (%s)\n", file, err->message);
		if (data) g_free(data);
		return NULL;
	}
	
	g_assert_debug(data);

	if (!length) {
		LOG_WARN("file '%s' is empty\n", file);
		return NULL;
	}
	
	ba = g_byte_array_new();
	ba->data = (guint8*) data;
	ba->len = length;
	
	return ba;
}

///////////////////////////////////////////////////////////////////////////////
//
// publicfunctions
//
///////////////////////////////////////////////////////////////////////////////

GByteArray*
rem_img_get(const gchar *file, guint width_max, guint height_max)
{
	g_assert_debug(file);

	MagickBooleanType	magick_ret;
	MagickWand		*magick_wand;
	GByteArray		*ba;
	gfloat			scale;
	gulong			width_orig, height_orig;
	guint			width_target, height_target;
	gchar			*tmp_file;
 	
	// init Wand and read image

	MagickWandGenesis();
	magick_wand = NewMagickWand();
	  
	LOG_DEBUG("load image '%s'\n", file);
	
	magick_ret = MagickReadImage(magick_wand, file);
	if (magick_ret == MagickFalse) {
				
		LOG_WARN("loading image '%s' failed\n", file);
		rem_img_handle_wand_error(magick_wand);
		return NULL;
		
	}

	// resize the image

	LOG_DEBUG("get size..\n");

	MagickSetFirstIterator(magick_wand);

	width_orig = MagickGetImageWidth(magick_wand);
	height_orig = MagickGetImageHeight(magick_wand);

	if (width_orig == 0 || height_orig == 0) {
		LOG_WARN("image '%s' has 0 size\n", file);
		rem_img_handle_wand_error(magick_wand);
		return NULL;
	}

	LOG_DEBUG("size: %lu x %lu\n", width_orig, height_orig);

	scale = MAX(	(gfloat) width_orig / width_max,
			(gfloat) height_orig / height_max);
	
	width_target = width_orig / scale;
	height_target = height_orig / scale;
	
	LOG_DEBUG("scale, width_orig, width_target, width_max, height_orig, height_target, height_max == "
		"%f, %u, %u, %u, %u ,%u, %u\n",
		scale, (guint) width_orig, width_target, width_max, (guint) height_orig, height_target, height_max);
	g_assert_debug(width_target <= width_max && height_target <= height_max);

	MagickResizeImage(
		magick_wand, width_target, height_target, LanczosFilter, 1.0);
		
	// temporary write the resized image as PNG

	tmp_file = g_strdup_printf("%s/remuco-img-%s.png",
					g_get_tmp_dir(), g_get_user_name());

	magick_ret = MagickWriteImages(magick_wand, tmp_file, MagickTrue);
	if (magick_ret == MagickFalse) {
		
		LOG_WARN("could not write tmp image '%s'\n", tmp_file);
		
		g_free(tmp_file);
		
		rem_img_handle_wand_error(magick_wand);
		
		return NULL;
	}

	magick_wand = DestroyMagickWand(magick_wand);  // we are done with Magick
	MagickWandTerminus();

	// load the temporary image into a byte array
	
	ba = rem_img_file_to_ba(tmp_file);
	
	g_free(tmp_file);
	
	return ba;
	
}

