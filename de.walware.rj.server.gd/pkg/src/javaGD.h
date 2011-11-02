#ifndef _DEV_JAVAGD_H
#define _DEV_JAVAGD_H

#define JAVAGD_VER 0x000505 /* JavaGD v0.5-5 */

#ifdef HAVE_CONFIG_H
# include <config.h>
#endif

#include <R.h>
#include <Rversion.h>
#include <Rinternals.h>
#include <R_ext/GraphicsEngine.h>
#include <R_ext/GraphicsDevice.h>
#include <jni.h>

#ifndef NewDevDesc
#define NewDevDesc DevDesc
#endif

/********************************************************/
/* Each driver can have its own device-specic graphical */
/* parameters and resources.  these should be wrapped	*/
/* in a structure (like the x11Desc structure below)	*/
/* and attached to the overall device description via	*/
/* the dd->deviceSpecific pointer			*/
/* NOTE that there are generic graphical parameters	*/
/* which must be set by the device driver, but are	*/
/* common to all device types (see Graphics.h)		*/
/* so go in the GPar structure rather than this device- */
/* specific structure					*/
/********************************************************/

typedef struct {
    /* Graphics Parameters */
    /* Local device copy so that we can detect */
    /* when parameter changes. */

    /* cex retained -- its a GRZ way of specifying text size, but
     * its too much work to change at this time (?)
     */
    double cex;				/* Character expansion */
    /* srt removed -- its a GRZ parameter and is not used in devX11.c
     */
    int lty;				/* Line type */
    double lwd;
    int col;				/* Color */
    /* fg and bg removed -- only use col and new param fill
     */
    int fill;
    int canvas;				/* Canvas */
    int fontface;			/* Typeface */
    int fontsize;			/* Size in points */
    int basefontface;			/* Typeface */
    int basefontsize;			/* Size in points */

    jobject talk; /* object associated with this graphics */
    jclass  talkClass; /* class of the talk object (cached) */
} newJavaGDDesc;

#endif

