#ifndef __JGD_TALK_H__
#define __JGD_TALK_H__

#ifdef HAVE_CONFIG_H
# include <config.h>
#endif

#include "javaGD.h"

Rboolean createJavaGD(newJavaGDDesc *xd);
void initJavaGD(newJavaGDDesc *xd, double *width, double *height, int *unit, double *xpi, double *ypi);
void setupJavaGDfunctions(NewDevDesc *dd);
void openJavaGD(NewDevDesc *dd);

void getJavaGDPPI(NewDevDesc *dd, double *xpi, double *ypi);

#endif
