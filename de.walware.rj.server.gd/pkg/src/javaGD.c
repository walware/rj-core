/*
 *  R : A Computer Language for Statistical Data Analysis
 *  Copyright (C) 1995, 1996  Robert Gentleman and Ross Ihaka
 *  Copyright (C) 1997--2003  Robert Gentleman, Ross Ihaka and the
 *			      R Development Core Team
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

#define R_JAVAGD 1
#include "javaGD.h"
#include "jGDtalk.h"

double jGDdpiX = 100.0;
double jGDdpiY = 100.0;
double jGDasp  = 1.0;

/********************************************************/
/* If there are resources that are shared by all devices*/
/* of this type, you may wish to make them globals	*/
/* rather than including them in the device-specific	*/
/* parameters structure (especially if they are large !)*/
/********************************************************/

/* JavaGD Driver Specific parameters
 * with only one copy for all xGD devices */


/*  JavaGD Device Driver Arguments	:	*/
/*	1) display name			*/
/*	2) width (pixels)		*/
/*	3) height (pixels)		*/
/*	4) host to connect to		*/
/*	5) tcp port to connect to	*/

Rboolean newJavaGDDeviceDriver(NewDevDesc *dd,
			    char *disp_name,
			    double width,
			    double height,
                            double initps)
{
  newJavaGDDesc *xd;

#ifdef JGD_DEBUG
  printf("TD: newJavaGDDeviceDriver(\"%s\", %f, %f, %f)\n",disp_name,width,height,initps);
#endif

  xd = Rf_allocNewJavaGDDeviceDesc(initps);
  if (!newJavaGD_Open((NewDevDesc*)(dd), xd, disp_name, width, height)) {
    free(xd);
    return FALSE;
  }
  
  Rf_setNewJavaGDDeviceData((NewDevDesc*)(dd), 0.6, xd);
  
  return TRUE;
}

/**
  This fills the general device structure (dd) with the JavaGD-specific
  methods/functions. It also specifies the current values of the
  dimensions of the device, and establishes the fonts, line styles, etc.
 */
int
Rf_setNewJavaGDDeviceData(NewDevDesc *dd, double gamma_fac, newJavaGDDesc *xd)
{
#ifdef JGD_DEBUG
	printf("Rf_setNewJavaGDDeviceData\n");
#endif

    /*	Set up Data Structures. */
    setupJavaGDfunctions(dd);

    /* Set required graphics parameters. */

    /* Window Dimensions in Pixels */
    /* Initialise the clipping rect too */

    dd->left = dd->clipLeft = 0;			/* left */
    dd->right = dd->clipRight = xd->windowWidth;	/* right */
    dd->bottom = dd->clipBottom = xd->windowHeight;	/* bottom */
    dd->top = dd->clipTop = 0;			/* top */

    /* Nominal Character Sizes in Pixels */

    dd->cra[0] = 8;
    dd->cra[1] = 11;

    /* Character Addressing Offsets */
    /* These are used to plot a single plotting character */
    /* so that it is exactly over the plotting point */

    dd->xCharOffset = 0.4900;
    dd->yCharOffset = 0.3333;
    dd->yLineBias = 0.1;

    /* Inches per raster unit */

    dd->ipr[0] = 1/jGDdpiX;
    dd->ipr[1] = 1/jGDdpiY;
#if R_GE_version < 4
    dd->asp = jGDasp;

    /* Device capabilities */
    dd->canResizePlot = TRUE;
    dd->canChangeFont = TRUE;
    dd->canRotateText = TRUE;
    dd->canResizeText = TRUE;
#endif
    dd->canClip = TRUE;
    dd->canHAdj = 2;
    dd->canChangeGamma = FALSE;

    dd->startps = xd->basefontsize;
    dd->startcol = xd->col;
    dd->startfill = xd->fill;
    dd->startlty = LTY_SOLID;
    dd->startfont = 1;
    dd->startgamma = gamma_fac;

    dd->deviceSpecific = (void *) xd;

    dd->displayListOn = TRUE;

    return(TRUE);
}


/**
 This allocates an newJavaGDDesc instance  and sets its default values.
 */
newJavaGDDesc * Rf_allocNewJavaGDDeviceDesc(double ps)
{
    newJavaGDDesc *xd;
    /* allocate new device description */
    if (!(xd = (newJavaGDDesc*)calloc(1, sizeof(newJavaGDDesc))))
	return FALSE;

    /* From here on, if we need to bail out with "error", */
    /* then we must also free(xd). */

    /*	Font will load at first use.  */

    if (ps < 6 || ps > 24) ps = 12;
    xd->fontface = -1;
    xd->fontsize = -1;
    xd->basefontface = 1;
    xd->basefontsize = ps;

    return(xd);
}


typedef Rboolean (*JavaGDDeviceDriverRoutine)(NewDevDesc*, char*, 
					      double, double);

/*
static char *SaveString(SEXP sxp, int offset)
{
    char *s;
    if(!isString(sxp) || length(sxp) <= offset)
	error("invalid string argument");
    s = R_alloc(strlen(CHAR(STRING_ELT(sxp, offset)))+1, sizeof(char));
    strcpy(s, CHAR(STRING_ELT(sxp, offset)));
    return s;
} */

static GEDevDesc* 
Rf_addJavaGDDevice(char *display, double width, double height, double initps)
{
    NewDevDesc *dev = NULL;
    GEDevDesc *dd;
    
    char *devname="JavaGD";

    R_CheckDeviceAvailable();
#ifdef BEGIN_SUSPEND_INTERRUPTS
    BEGIN_SUSPEND_INTERRUPTS {
#endif
	/* Allocate and initialize the device driver data */
	if (!(dev = (NewDevDesc*)calloc(1, sizeof(NewDevDesc))))
	    return 0;
	/* Do this for early redraw attempts */
#if R_GE_version < 4
	dev->displayList = R_NilValue;
	dev->newDevStruct = 1;
	/* Make sure that this is initialised before a GC can occur.
	 * This (and displayList) get protected during GC
	 */
	dev->savedSnapshot = R_NilValue;
#endif
	/* Took out the GInit because MOST of it is setting up
	 * R base graphics parameters.  
	 * This is supposed to happen via addDevice now.
	 */
	if (!newJavaGDDeviceDriver(dev, display, width, height, initps))
	  {
	    free(dev);
		error("unable to start device %s", devname);
	    return 0;
	  }
	gsetVar(install(".Device"), mkString(devname), R_NilValue);
	dd = GEcreateDevDesc(dev);
	GEaddDevice(dd);
	GEinitDisplayList(dd);
#ifdef JGD_DEBUG
	printf("JavaGD> devNum=%d, dd=%lx\n", ndevNumber(dd), (unsigned long)dd);
#endif
#ifdef BEGIN_SUSPEND_INTERRUPTS
    } END_SUSPEND_INTERRUPTS;
#endif
    
    return(dd);
}

void resizedJavaGD(NewDevDesc *dd);

void reloadJavaGD(int *dn) {
	GEDevDesc *gd= GEgetDevice(*dn);
	if (gd) {
		NewDevDesc *dd=gd->dev;
#ifdef JGD_DEBUG
		printf("reloadJavaGD: dn=%d, dd=%lx\n", *dn, (unsigned long)dd);
#endif
		if (dd) resizedJavaGD(dd);
	}
}

SEXP javaGDobjectCall(SEXP dev) {
  int ds=NumDevices();
  int dn;
  GEDevDesc *gd;
  void *ptr=0;

  if (!isInteger(dev) || LENGTH(dev)<1) return R_NilValue;
  dn = INTEGER(dev)[0];
  if (dn<0 || dn>=ds) return R_NilValue;
  gd=GEgetDevice(dn);
  if (gd) {
    NewDevDesc *dd=gd->dev;
    if (dd) {
      newJavaGDDesc *xd=(newJavaGDDesc*) dd->deviceSpecific;
      if (xd) ptr = xd->talk;
    }
  }
  if (!ptr) return R_NilValue;
  return R_MakeExternalPtr(ptr, R_NilValue, R_NilValue);
}

void javaGDresize(int dev) {
    int ds=NumDevices();
    int i=0;
    if (dev>=0 && dev<ds) { i=dev; ds=dev+1; }
    while (i<ds) {
        GEDevDesc *gd=GEgetDevice(i);
        if (gd) {
            NewDevDesc *dd=gd->dev;
#ifdef JGD_DEBUG
            printf("javaGDresize: device=%d, dd=%lx\n", i, (unsigned long)dd);
#endif
            if (dd) {
#ifdef JGD_DEBUG
                printf("dd->size=%lx\n", (unsigned long)dd->size);
#endif
                dd->size(&(dd->left), &(dd->right), &(dd->bottom), &(dd->top), dd);
                GEplayDisplayList(gd);
            }
        }
        i++;
    }
}

void resizedJavaGD(NewDevDesc *dd) {
	int devNum;
	/* newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific; */
#ifdef JGD_DEBUG
	printf("dd->size=%lx\n", (unsigned long)dd->size);
#endif
	dd->size(&(dd->left), &(dd->right), &(dd->bottom), &(dd->top), dd);
	devNum = ndevNumber(dd);
	if (devNum > 0)
		GEplayDisplayList(GEgetDevice(devNum));
}

void newJavaGD(char **name, double *w, double *h, double *ps) {
	Rf_addJavaGDDevice(*name, *w, *h, *ps);  
}

void javaGDgetSize(int *dev, double *par) {
    int ds=NumDevices();
    if (*dev<0 || *dev>=ds) return;
    {
        GEDevDesc *gd=GEgetDevice(*dev);
        if (gd) {
            NewDevDesc *dd=gd->dev;
            /*
             if (dd) {
                 newJavaGDDesc *xd=(newJavaGDDesc*) dd->deviceSpecific;
                 if (xd) *obj=(int) xd->talk;
             }
             */
			if (dd) {
				par[0]=dd->left;
				par[1]=dd->top;
				par[2]=dd->right;
				par[3]=dd->bottom;
				par[4]=jGDdpiX;
				par[5]=jGDdpiY;
			} else {
#ifdef JGD_DEBUG
				printf("sizefailed>> device=%d, gd=%lx, dd=%lx\n",*dev,
				       (unsigned long)gd, (unsigned long)dd);
#endif
			}	
        }
    }
}

void javaGDsetDisplayParam(double *par) {
	jGDdpiX = par[0];
	jGDdpiY = par[1];
	jGDasp  = par[2];
}

void javaGDgetDisplayParam(double *par) {
	par[0] = jGDdpiX;
	par[1] = jGDdpiY;
	par[2] = jGDasp;
}

void javaGDversion(int *ver) {*
	ver=JAVAGD_VER;
}
