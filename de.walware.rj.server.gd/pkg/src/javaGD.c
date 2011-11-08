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

double jGDasp  = 1.0;

/********************************************************/
/* If there are resources that are shared by all devices*/
/* of this type, you may wish to make them globals	*/
/* rather than including them in the device-specific	*/
/* parameters structure (especially if they are large !)*/
/********************************************************/


static int setNewJavaGDDeviceData(NewDevDesc *dd, newJavaGDDesc *xd,
		double width, double height, double gamma);


/* JavaGD Driver Specific parameters
 * with only one copy for all xGD devices */

static Rboolean newJavaGDDeviceDriver(NewDevDesc *dd, char *display,
		double width, double height, int sizeUnit,
		double xpi, double ypi, int canvas,
		double pointsize, double gamma)
{
	newJavaGDDesc *xd;
	
#ifdef JGD_DEBUG
  printf("TD: newJavaGDDeviceDriver(\"%s\", %f, %f, %f)\n", disp_name, width, height, pointsize);
#endif
	
	// allocate new device description
	if (!(xd = (newJavaGDDesc*) calloc(1, sizeof(newJavaGDDesc)))) {
		return FALSE;
	}
	// from here on, if we need to bail out with "error", then we must also free(xd).
	if (!createJavaGD(xd)) {
		free(xd);
		return FALSE;
	}
	
	xd->fill = 0xffffffff; /* transparent */
	xd->col = R_RGB(0, 0, 0);
	xd->canvas = canvas;
	
	initJavaGD(xd, &width, &height, &sizeUnit, &xpi, &ypi);
	
	/* Font will load at first use. */
	if (pointsize < 6 || pointsize > 24) pointsize = 12;
	xd->fontface = -1;
	xd->fontsize = -1;
	xd->basefontface = 1;
	xd->basefontsize = pointsize;
	
	setNewJavaGDDeviceData((NewDevDesc*)(dd), xd,
			width, height, gamma);
	
	return TRUE;
}

/**
  This fills the general device structure (dd) with the JavaGD-specific
  methods/functions. It also specifies the current values of the
  dimensions of the device, and establishes the fonts, line styles, etc.
 */
static int
setNewJavaGDDeviceData(NewDevDesc *dd, newJavaGDDesc *xd,
		double width, double height, double gamma)
{
#ifdef JGD_DEBUG
	printf("setNewJavaGDDeviceData\n");
#endif

    dd->deviceSpecific = (void *) xd;

    /*	Set up Data Structures. */
    setupJavaGDfunctions(dd);

    /* Set required graphics parameters. */

    /* Window Dimensions in Pixels */
    /* Initialise the clipping rect too */
    dd->left = dd->clipLeft = 0;
    dd->right = dd->clipRight = width - 1.0;
    dd->top = dd->clipTop = 0;
    dd->bottom = dd->clipBottom = height - 1.0;

    /* Character Addressing Offsets */
	dd->xCharOffset = 0.4900;
	dd->yCharOffset = 0.3333;
	dd->yLineBias = 0.2;
	
	{	double xpi;
		double ypi;
		getJavaGDPPI(dd, &xpi, &ypi);
		
		/* Inches per raster unit */
		dd->ipr[0] = 1.0/xpi;
		dd->ipr[1] = 1.0/ypi;
		
		/* Nominal Character Sizes in Pixels */
		dd->cra[0] = 0.9 * xd->basefontsize / 72.0 * xpi;
		dd->cra[1] = 1.2 * xd->basefontsize / 72.0 * ypi;
	}
	
    dd->canChangeGamma = FALSE;

    dd->startps = xd->basefontsize;
    dd->startcol = xd->col;
    dd->startfill = xd->fill;
    dd->startlty = LTY_SOLID;
    dd->startfont = 1;
    dd->startgamma = gamma;

    dd->displayListOn = TRUE;

    return(TRUE);
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
addJavaGDDevice(char *display, double width, double height, int sizeUnit,
		double xpinch, double ypinch, int canvas,
		double pointsize, double gamma)
{
    NewDevDesc *dev = NULL;
    GEDevDesc *dd;
    
    char *devname="rj.GD";

    R_GE_checkVersionOrDie(R_GE_version);
    R_CheckDeviceAvailable();
#ifdef BEGIN_SUSPEND_INTERRUPTS
    BEGIN_SUSPEND_INTERRUPTS {
#endif
	/* Allocate and initialize the device driver data */
	if (!(dev = (NewDevDesc*) calloc(1, sizeof(NewDevDesc))))
	    return 0;
	/* Took out the GInit because MOST of it is setting up
	 * R base graphics parameters.  
	 * This is supposed to happen via addDevice now.
	 */
	if (!newJavaGDDeviceDriver(dev, display, width, height, sizeUnit,
			xpinch, ypinch, canvas,
			pointsize, gamma )) {
		free(dev);
		error("unable to start device %s", devname);
		return 0;
	}
	dd = GEcreateDevDesc(dev);
	GEaddDevice2(dd, devname);
#ifdef JGD_DEBUG
	printf("JavaGD> devNum=%d, dd=%lx\n", ndevNumber(dd), (unsigned long)dd);
#endif
	openJavaGD(dev);
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

SEXP newJavaGD(SEXP sName, SEXP sWidth, SEXP sHeight, SEXP sSizeUnit,
		SEXP sXpinch, SEXP sYpinch, SEXP sCanvasColor,
		SEXP sPointsize, SEXP sGamma) {
	double width = Rf_asReal(sWidth);
	double height = Rf_asReal(sHeight);
	if (!R_FINITE(width) || width < 0.0) {
		error("Illegal argument: width");
	}
	if (!R_FINITE(height) || height < 0.0) {
		error("Illegal argument: height");
	}
	int sizeUnit = Rf_asInteger(sSizeUnit);
	
	double xpinch = Rf_asReal(sXpinch);
	double ypinch = Rf_asReal(sYpinch);
	if (!R_FINITE(xpinch) || xpinch <= 0.0) {
		xpinch = 0.0;
		ypinch = 0.0;
	} else if (!R_FINITE(ypinch)) {
		ypinch = xpinch;
	}
	
	int canvas = Rf_RGBpar(sCanvasColor, 0);
	
	double pointsize = Rf_asReal(sPointsize);
	
	double gamma = Rf_asReal(sGamma);
	if (!R_FINITE(gamma)) {
		gamma = 1.0;
	}
	
	addJavaGDDevice("", width, height, sizeUnit,
			xpinch, ypinch, canvas,
			pointsize, gamma );
	return R_NilValue;
}

void javaGDsetDisplayParam(double *par) {
	jGDasp  = par[2];
}

void javaGDversion(int *ver) {
	*ver=JAVAGD_VER;
}
