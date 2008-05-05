#include "serial.h"
#include "util.h"

typedef enum {
	REM_SDT_NONE = 0,
	REM_SDT_Y,			// byte
	REM_SDT_I,			// int32
	REM_SDT_B,			// bool 
	REM_SDT_S,			// string
	REM_SDT_AY,			// byte array
	REM_SDT_AI,			// int array
	REM_SDT_AS			// string array
} RemSerialDataType;

/** Empty string array to use when a va_arg string array is NULL. */
static gchar	*as_empty[] = { NULL };

/**
 * Writes some data into a byte array.
 * 
 */
void
rem_serial_out(GByteArray *out, const gchar *enc_to, const gchar *fs, ...)
{
	gint			arg_i;
	gboolean		arg_b;
	gchar			*arg_s;
	GByteArray		*arg_ay;
	gchar			**arg_as;
	va_list			args;
	
	guint			u, v, l;
	guint8			i8;
	gint32			i32;
	gchar			*s_conv;
	gchar			**as_conv;
	
	////////// function specific macros //////////
	
	#define REM_SERIAL_WRITE_BYTE(_y)	\
	i8 = (guint8) (_y);					\
	g_byte_array_append(out, &i8, 1);

	#define REM_SERIAL_WRITE_INT(_i)				\
	i32 = (gint32) (_i);							\
	i32 = g_htonl(i32);								\
	g_byte_array_append(out, (guint8*) &i32, 4);
	
	////////// go .. //////////
	
	out->len = 0;
	
	va_start(args, fs);
	
	for (u = 0; fs[u]; u++) {
		
		if (fs[u] == ' ') {
			continue;
		}
		
		////////// basic types //////////
		
		if (fs[u] == 'i') {
			
			REM_SERIAL_WRITE_BYTE(REM_SDT_I);					// type
			arg_i = va_arg(args, gint);
			REM_SERIAL_WRITE_INT(arg_i);						// int
			
		} else if (fs[u] == 'b') {
			
			REM_SERIAL_WRITE_BYTE(REM_SDT_B);					// type
			arg_b = va_arg(args, gboolean);
			REM_SERIAL_WRITE_BYTE(arg_b ? 1 : 0);				// byte
			
		} else if (fs[u] == 's') {

			REM_SERIAL_WRITE_BYTE(REM_SDT_S);					// type
			arg_s = va_arg(args, gchar*);
			
			arg_s = arg_s ? arg_s : "";

			s_conv = NULL;
			rem_util_conv_s(arg_s, &s_conv, NULL, enc_to);
			
			if (s_conv) {
				l = strlen(s_conv);
				REM_SERIAL_WRITE_INT(l);						// len string
				g_byte_array_append(out, (guint8*) s_conv, l);	// string		
				g_free(s_conv);
			} else {
				l = strlen(arg_s);
				REM_SERIAL_WRITE_INT(l);						// len string
				g_byte_array_append(out, (guint8*) arg_s, l);	// string				
			}
		
		} else if (fs[u] == 'a') {
			
			////////// array types //////////

			u++; g_assert(fs[u]);
			
			if (fs[u] == 'y') {
						
				REM_SERIAL_WRITE_BYTE(REM_SDT_AY);				// type
				arg_ay = va_arg(args, GByteArray*);

				if (!arg_ay) {
					REM_SERIAL_WRITE_INT(0);					// no data
					continue;
				}

				REM_SERIAL_WRITE_INT(arg_ay->len);
				g_byte_array_append(out, arg_ay->data, arg_ay->len);
				
			} else if (fs[u] == 's') {
			
				REM_SERIAL_WRITE_BYTE(REM_SDT_AS);				// type
				arg_as = va_arg(args, gchar**);
				
				arg_as = arg_as ? arg_as : as_empty;
				
				as_conv = NULL;
				rem_util_conv_sv(arg_as, &as_conv, NULL, enc_to);

				l = g_strv_length(arg_as);
				REM_SERIAL_WRITE_INT(l);						// num strings

				if (as_conv) {
					for (v = 0; as_conv[v]; v++) {
						l = strlen(as_conv[v]);
						REM_SERIAL_WRITE_INT(l);				// len string
						g_byte_array_append(out, (guint8*) as_conv[v], l);	// string
					}
					g_strfreev(as_conv);
				} else {
					for (v = 0; arg_as[v]; v++) {
						l = strlen(arg_as[v]);
						REM_SERIAL_WRITE_INT(l);				// len string
						g_byte_array_append(out, (guint8*) arg_as[v], l);	// string					
					}
				}
				
			} else {
				g_assert_not_reached();
			}			
		} else {
			g_assert_not_reached();
		}
	}

	va_end(args);
}

gboolean
rem_serial_in(GByteArray *in, const gchar *enc_from, const gchar *fs, ...)
{
	gint		*arg_i_p;
	gboolean	*arg_b_p;
	gchar		**arg_s_p;
	gchar		***arg_as_p;
	GByteArray	**arg_ay_p;
	va_list		args;
	
	guint8		*ba_walker, *ba_end;
	guint		u, v, l, l_as;
	
	gint32		i32;
	gint8		i8;
	gchar		*s, *s_conv, **as, **as_conv;
	
	////////// function specific macros //////////

	#define REM_SERIAL_IN_CHECK_SIZE(_size)	\
	if (ba_walker + (_size) > ba_end) {		\
		LOG_WARN("not enough bytes");		\
		return FALSE;						\
	}
	
	#define REM_SERIAL_IN_CHECK_TYPE(_t)									\
	REM_SERIAL_IN_CHECK_SIZE(1);											\
	if (*(ba_walker++) != (_t)) {											\
		LOG_WARN("type mismatch (exp: %u, real: %u)", (_t), *(ba_walker-1));\
		return FALSE;														\
	}
	
	#define REM_SERIAL_IN_READ_INT(_i)	\
	REM_SERIAL_IN_CHECK_SIZE(4);		\
	i32 = *((gint32*) ba_walker);		\
	_i = g_ntohl(i32);  				\
	ba_walker += 4;

	////////// go .. //////////

	va_start(args, fs);
	
	ba_walker = in->data;
	ba_end = in->data + in->len;
	
	for (u = 0; fs[u]; u++) {
		
		if (fs[u] == ' ') {
			continue;
		}

		////////// basic types //////////

		if (fs[u] == 'i') {
			
			REM_SERIAL_IN_CHECK_TYPE(REM_SDT_I);
			
			arg_i_p = va_arg(args, gint32*);
			
			REM_SERIAL_IN_READ_INT(*arg_i_p);

		} else if (fs[u] == 'b') {
			
			REM_SERIAL_IN_CHECK_TYPE(REM_SDT_B);
			
			arg_b_p = va_arg(args, gboolean*);
			
			REM_SERIAL_IN_CHECK_SIZE(1);
			i8 = *((gint8*) ba_walker);							// bool
			*arg_b_p = i8 ? TRUE : FALSE;
			
			ba_walker += 1;

		} else if (fs[u] == 's') {
		
			REM_SERIAL_IN_CHECK_TYPE(REM_SDT_S);

			arg_s_p = va_arg(args, gchar**);

			REM_SERIAL_IN_READ_INT(l);							// len string
			
			REM_SERIAL_IN_CHECK_SIZE(l);
			s = g_strndup((gchar*) ba_walker, l);				// string
			s_conv = NULL;
			rem_util_conv_s(s, &s_conv, enc_from, NULL);
			if (s_conv) {
				*arg_s_p = s_conv;				
				g_free(s);
			} else {
				*arg_s_p = s;
			}

			ba_walker += l;
			
		} else if (fs[u] == 'a') {
			
			////////// array types //////////

			u++; g_assert(fs[u]);
			
			if (fs[u] == 'y') {
						
				REM_SERIAL_IN_CHECK_TYPE(REM_SDT_AY);
				
				arg_ay_p = va_arg(args, GByteArray**);
				
				REM_SERIAL_IN_READ_INT(l);						// len ba
				
				if (!l) {										// no data
					*arg_ay_p = NULL;
					continue;
				}
				
				*arg_ay_p = g_byte_array_sized_new(l);
				g_byte_array_append(*arg_ay_p, ba_walker, l);	// ba

				ba_walker += l;
				
			} else if (fs[u] == 's') {
			
				REM_SERIAL_IN_CHECK_TYPE(REM_SDT_AS);
						
				arg_as_p = va_arg(args, gchar***);

				REM_SERIAL_IN_READ_INT(l_as);					// num strings
				
				as = g_new0(gchar*, l_as + 1);
				
				for (v = 0; v < l_as; v++) {
					
					REM_SERIAL_IN_READ_INT(l);					// len string
					REM_SERIAL_IN_CHECK_SIZE(l);
					as[v] = g_strndup((gchar*) ba_walker, l);	// string
					ba_walker += l;
				}

				LOG_DEBUG("incoming string in %s", enc_from);
				
				as_conv = NULL;
				rem_util_conv_sv(as, &as_conv, enc_from, NULL);
				if (as_conv) {
					*arg_as_p = as_conv;
					g_strfreev(as);
				} else {
					*arg_as_p = as;
				}
				
			} else {
				g_assert_not_reached();
			}			
		} else {
			g_assert_not_reached();
		}
	}

	va_end(args);
	
	g_assert(ba_walker <= ba_end);
	
	if (ba_walker < ba_end) {
		LOG_WARN("too much bytes");
		return FALSE;
	}
	
	return TRUE;
}

/**
 * Resets all values of the given (variable length) arguments to type specific
 * init values (0, FALSE, NULL, ...). To call with the same (variable length)
 * arguments used for rem_serial_in() when this function has returned with
 * FALSE.
 */
void
rem_serial_reset(const gchar *fs, ...)
{
	gint		*arg_i_p;
	gboolean	*arg_b_p;
	gchar		**arg_s_p;
	gchar		***arg_as_p;
	GByteArray	**arg_ay_p;
	va_list		args;
	
	guint		u;
	
	va_start(args, fs);
	
	for (u = 0; fs[u]; u++) {
		
		if (fs[u] == ' ') {
			continue;
		}

		////////// basic types //////////

		if (fs[u] == 'i') {
			
			arg_i_p = va_arg(args, gint32*);
			
			*arg_i_p = 0;
		
		} else if (fs[u] == 'b') {
			
			arg_b_p = va_arg(args, gboolean*);
			
			*arg_b_p = FALSE;
			
		} else if (fs[u] == 's') {
		
			arg_s_p = va_arg(args, gchar**);

			if (*arg_s_p)
				g_free(*arg_s_p);
			
			*arg_s_p = NULL;
			
		} else if (fs[u] == 'a') {
			
			////////// array types //////////

			u++; g_assert(fs[u]);
			
			if (fs[u] == 'y') {
						
				arg_ay_p = va_arg(args, GByteArray**);

				if (*arg_ay_p)
					g_byte_array_free(*arg_ay_p, TRUE);
				
				*arg_ay_p = NULL;
				
			} else if (fs[u] == 's') {
			
				arg_as_p = va_arg(args, gchar***);

				if (*arg_as_p)
					g_strfreev(*arg_as_p);
				
				*arg_as_p = NULL;
				
			} else {
				g_assert_not_reached();
			}			
		} else {
			g_assert_not_reached();
		}
	}

	va_end(args);
	
	return;	
}
