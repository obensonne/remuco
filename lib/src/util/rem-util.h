#ifndef COMMON_H_
#define COMMON_H_

///////////////////////////////////////////////////////////////////////////////
//
// includes
//
///////////////////////////////////////////////////////////////////////////////

#include <remuco.h>

///////////////////////////////////////////////////////////////////////////////
//
// utility constans
//
///////////////////////////////////////////////////////////////////////////////

///////////////////////////////////////////////////////////////////////////////
//
// utility macros
//
///////////////////////////////////////////////////////////////////////////////

////////// a sleep function with milli seconds //////////

#define g_msleep(_ms)		g_usleep((_ms) * 1000) 

////////// extend glib logging with a noise level //////////

#define G_LOG_LEVEL_NOISE	(1 << G_LOG_LEVEL_USER_SHIFT)

#define g_noise(args...)	g_log(G_LOG_DOMAIN, G_LOG_LEVEL_NOISE, ##args)

////////// an 'if-then' boolean expression //////////

#define concl(_a, _b)		((!(_a)) || ((_a) && (_b)))

////////// assertions //////////

#if LOGLEVEL >= LL_DEBUG
#define g_assert_debug(_expr)	g_assert(_expr)
#define g_assert_not_reached_debug() g_assert_not_reached()
#else
#define g_assert_debug(_expr)
#define g_assert_not_reached_debug()
#endif

///////////////////////////////////////////////////////////////////////////////
//
// debug functions
//
///////////////////////////////////////////////////////////////////////////////

////////// dump macros used by remuco data types //////////

#define REM_DATA_DUMP_HDR(_t, _p)	LOG("DUMP(%s@%p):\n", _t, _p)
#define REM_DATA_DUMP_FTR			LOG("\n")

////////// dump binary data //////////

#if LOGLEVEL >= LL_NOISE
static void
rem_dump_ba(GByteArray *ba)
{
	LOG_NOISE("called\n");
	
	guint u;
	guint8 *d, *dd;
	
	#if LOGLEVEL >= LL_DEBUG
	LOG("Binary Data Dump: %p (%u bytes)\n", ba->data, ba->len);
	for (u = 0, d = ba->data, dd = ba->data + ba->len; d < dd; d++, u++) {
		LOG("%02hhX ", *d);
		if (u == 15) {
			u = -1;
			LOG("\n");
		}
	}   
	printf("\n");
	#endif
	
}
static void
rem_dump(guint8 data, guint len)
{
	GByteArray ba;
	ba.data = data;
	ba.len = len;
	dump_gba(&ba);
}
#else
#define rem_dump_ba(_ba)
#define rem_dump(_data, _len)
#endif


#endif /*COMMON_H_*/
