#ifndef BPPL_H_
#define BPPL_H_

#include "common.h"

typedef struct _RemBasicProxyLauncher RemBasicProxyLauncher;

RemBasicProxyLauncher*
rem_bppl_up(void);

void
rem_bppl_down(RemBasicProxyLauncher *bppl);

#endif /*BPPL_H_*/
