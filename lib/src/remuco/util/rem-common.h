#ifndef COMMON_H_
#define COMMON_H_

#include <glib.h>
#include "rem-log.h"

#define REM_LIB_MAJOR		0
#define REM_LIB_MINOR		5

#define REM_PROTO_VERSION	0x05

#define g_msleep(_ms)		g_usleep((_ms) * 1000) 

#if LOGLEVEL >= LL_DEBUG
#define g_assert_debug(_expr)	g_assert(_expr)
#define g_assert_not_reached_debug() g_assert_not_reached()
#else
#define g_assert_debug(_expr)
#define g_assert_not_reached_debug()
#endif

#define G_LOG_LEVEL_NOISE	(1 << G_LOG_LEVEL_USER_SHIFT)

#define g_noise(args...)	g_log(G_LOG_DOMAIN, G_LOG_LEVEL_NOISE, ##args)

#define concl(_a, _b)		((!(_a)) || ((_a) && (_b)))

#define REM_MAX_CLIENTS		50

#define DUMP_HDR(_t, _p)	LOG("DUMP(%s@%p):\n", _t, _p)
#define DUMP_FTR		LOG("\n")

//#ifdef DEBUG
//static void
//dump_gba(GByteArray *ba)
//{
//	LOG_NOISE("called\n");
//	
//	guint u;
//	guint8 *d, *dd;
//	
//	#if LOGLEVEL >= LL_DEBUG
//	LOG("byte array: %p (%u bytes)\n", ba->data, ba->len);
//	for (u = 0, d = ba->data, dd = ba->data + ba->len; d < dd; d++, u++) {
//		LOG("%02hhX ", *d);
//		if (u == 15) {
//			u = -1;
//			LOG("\n");
//		}
//		//usleep(10);
//	}   
//	printf("\n");
//	#endif
//	
//}
//#else
#define dump_gba(_ba)
//#endif


#endif /*COMMON_H_*/
