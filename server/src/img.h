#ifndef REM_IMG_H_
#define REM_IMG_H_

#include "common.h"

typedef struct _RemImg RemImg;

RemImg*
rem_img_up(const gchar *type);

GByteArray*
rem_img_get(RemImg *ri, const gchar *file, guint width_max, guint height_max);

void
rem_img_down(RemImg *ri);

#endif /*REM_IMG_H_*/
