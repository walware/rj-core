#include "javaGD.h"
#include "jGDtalk.h"
#include "rjutil.h"
#include <Rdefines.h>

char *symbol2utf8(const char *c); /* from s2u.c */

/* Device Driver Actions */

#define jgdCheckExceptions chkX

#ifdef JGD_DEBUG
#define gdWarning(S) { printf("[javaGD warning] %s\n", S); jgdCheckExceptions(getJNIEnv()); }
#else
#define gdWarning(S)
#endif

#if R_VERSION < R_Version(2,11,0)
#error This JavaGD needs at least R version 2.11.0
#endif

#define constxt const


static void newJavaGD_Activate(NewDevDesc *dd);
static void newJavaGD_Circle(double x, double y, double r,
			  R_GE_gcontext *gc,
			  NewDevDesc *dd);
static void newJavaGD_Clip(double x0, double x1, double y0, double y1,
			NewDevDesc *dd);
static void newJavaGD_Close(NewDevDesc *dd);
static void newJavaGD_Deactivate(NewDevDesc *dd);
static void newJavaGD_Line(double x1, double y1, double x2, double y2,
			R_GE_gcontext *gc,
			NewDevDesc *dd);
static Rboolean newJavaGD_Locator(double *x, double *y, NewDevDesc *dd);
static void newJavaGD_MetricInfo(int c, 
			      R_GE_gcontext *gc,
			      double* ascent, double* descent,
			      double* width, NewDevDesc *dd);
static void newJavaGD_Mode(int mode, NewDevDesc *dd);
static void newJavaGD_NewPage(R_GE_gcontext *gc, NewDevDesc *dd);
static Rboolean newJavaGD_NewPageConfirm(NewDevDesc *dd);
static void newJavaGD_Polygon(int n, double *x, double *y,
			   R_GE_gcontext *gc,
			   NewDevDesc *dd);
static void newJavaGD_Polyline(int n, double *x, double *y,
			     R_GE_gcontext *gc,
			     NewDevDesc *dd);
static void newJavaGD_Rect(double x0, double y0, double x1, double y1,
			 R_GE_gcontext *gc,
			 NewDevDesc *dd);
static void newJavaGD_Size(double *left, double *right,
			 double *bottom, double *top,
			 NewDevDesc *dd);
static double newJavaGD_StrWidth(constxt char *str, 
			       R_GE_gcontext *gc,
			       NewDevDesc *dd);
static double newJavaGD_StrWidthUTF8(constxt char *str, 
			       R_GE_gcontext *gc,
			       NewDevDesc *dd);
static void newJavaGD_Text(double x, double y, constxt char *str,
			 double rot, double hadj,
			 R_GE_gcontext *gc,
			 NewDevDesc *dd);
static void newJavaGD_TextUTF8(double x, double y, constxt char *str,
			 double rot, double hadj,
			 R_GE_gcontext *gc,
			 NewDevDesc *dd);
static void newJavaGD_Raster(unsigned int *raster, int w, int h,
			   double x, double y, double width, double height,
			   double rot, Rboolean interpolate,
			   R_GE_gcontext *gc, NewDevDesc *dd);


static R_GE_gcontext lastGC; /** last graphics context. the API send changes, not the entire context, so we cache it for comparison here */

static JavaVM *jvm=0;
char *jarClassPath = ".";

static jclass jcGDInterface;
static jmethodID jmGDInterfaceActivate;
static jmethodID jmGDInterfaceCircle;
static jmethodID jmGDInterfaceClip;
static jmethodID jmGDInterfaceClose;
static jmethodID jmGDInterfaceDeactivate;
static jmethodID jmGDInterfaceGetPPI;
static jmethodID jmGDInterfaceInit;
static jmethodID jmGDInterfaceLocator;
static jmethodID jmGDInterfaceLine;
static jmethodID jmGDInterfaceMetricInfo;
static jmethodID jmGDInterfaceMode;
static jmethodID jmGDInterfaceNewPage;
static jmethodID jmGDInterfaceNewPageConfirm;
static jmethodID jmGDInterfaceOpen;
static jmethodID jmGDInterfacePolygon;
static jmethodID jmGDInterfacePolyline;
static jmethodID jmGDInterfaceRect;
static jmethodID jmGDInterfaceSize;
static jmethodID jmGDInterfaceStrWidth;
static jmethodID jmGDInterfaceText;
static jmethodID jmGDInterfaceRaster;
static jmethodID jmGDInterfaceSetColor;
static jmethodID jmGDInterfaceSetFill;
static jmethodID jmGDInterfaceSetLine;
static jmethodID jmGDInterfaceSetFont;


/** check exception for the given environment. The exception is printed only in JGD_DEBUG mode. */
static void chkX(JNIEnv *env)
{
    jthrowable t=(*env)->ExceptionOccurred(env);
    if (t) {
#ifndef JGD_DEBUG
		(*env)->ExceptionDescribe(env);
#endif
        (*env)->ExceptionClear(env);
    }
}

/** get java environment for the current thread or 0 if something goes wrong. */
static JNIEnv *getJNIEnv() {
    JNIEnv *env;
    jsize l;
    jint res = 0;
    
    if (!jvm) { /* we're hoping that the JVM pointer won't change :P we fetch it just once */
        res = JNI_GetCreatedJavaVMs(&jvm, 1, &l);
        if (res != 0) {
	  fprintf(stderr, "JNI_GetCreatedJavaVMs failed! (%d)\n", (int)res); return 0;
        }
        if (l<1) {
	  /* fprintf(stderr, "JNI_GetCreatedJavaVMs said there's no JVM running!\n"); */ return 0;
        }
	if (!jvm)
	  error("Unable to get JVM handle");
    }
    res = (*jvm)->AttachCurrentThread(jvm, (void**) &env, 0);
    if (res!=0) {
        fprintf(stderr, "AttachCurrentThread failed! (%d)\n", (int)res); return 0;
    }
    /* if (eenv!=env)
        fprintf(stderr, "Warning! eenv=%x, but env=%x - different environments encountered!\n", eenv, env); */
    return env;
}

#define checkGC(e,xd,gc) sendGC(e,xd,gc,0)

/** check changes in GC and issue corresponding commands if necessary */
static void sendGC(JNIEnv *env, newJavaGDDesc *xd, R_GE_gcontext *gc, int sendAll) {
    if (sendAll || gc->col != lastGC.col) {
		(*env)->CallVoidMethod(env, xd->talk, jmGDInterfaceSetColor, gc->col);
		chkX(env);
    }

    if (sendAll || gc->fill != lastGC.fill)  {
		(*env)->CallVoidMethod(env, xd->talk, jmGDInterfaceSetFill, gc->fill);
		chkX(env);
    }

    if (sendAll || gc->lwd != lastGC.lwd || gc->lty != lastGC.lty) {
		(*env)->CallVoidMethod(env, xd->talk, jmGDInterfaceSetLine, gc->lwd, gc->lty);
		chkX(env);
    }

    if (sendAll || gc->cex!=lastGC.cex || gc->ps!=lastGC.ps || gc->lineheight!=lastGC.lineheight || gc->fontface!=lastGC.fontface || strcmp(gc->fontfamily, lastGC.fontfamily)) {
        jstring s = (*env)->NewStringUTF(env, gc->fontfamily);
		(*env)->CallVoidMethod(env, xd->talk, jmGDInterfaceSetFont, gc->cex, gc->ps, gc->lineheight, gc->fontface, s);
		chkX(env);
    }
    memcpy(&lastGC, gc, sizeof(lastGC));
}

/* re-set the GC - i.e. send commands for all monitored GC entries */
static void sendAllGC(JNIEnv *env, newJavaGDDesc *xd, R_GE_gcontext *gc) {
    /*
    printf("Basic GC:\n col=%08x\n fill=%08x\n gamma=%f\n lwd=%f\n lty=%08x\n cex=%f\n ps=%f\n lineheight=%f\n fontface=%d\n fantfamily=\"%s\"\n\n",
	 gc->col, gc->fill, gc->gamma, gc->lwd, gc->lty,
	 gc->cex, gc->ps, gc->lineheight, gc->fontface, gc->fontfamily);
     */
    sendGC(env, xd, gc, 1);
}

/*------- the R callbacks begin here ... ------------------------*/

static void newJavaGD_Activate(NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    JNIEnv *env = getJNIEnv();
    
    if(!env || !xd || !xd->talk) return;
	
	(*env)->CallVoidMethod(env, xd->talk, jmGDInterfaceActivate);
	chkX(env);
}

static void newJavaGD_Circle(double x, double y, double r,  R_GE_gcontext *gc,  NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    JNIEnv *env = getJNIEnv();
    
    if(!env || !xd || !xd->talk) return;

    checkGC(env,xd, gc);
	
	(*env)->CallVoidMethod(env, xd->talk, jmGDInterfaceCircle, x, y, r);
	chkX(env);
}

static void newJavaGD_Clip(double x0, double x1, double y0, double y1,  NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    JNIEnv *env = getJNIEnv();
    
    if (!env || !xd || !xd->talk) return;
    
	if (x0 > x1) {
		double tmp = x0;
		x0 = x1;
		x1 = tmp;
	}
	if (y0 > y1) {
		double tmp = y0;
		y0 = y1;
		y1 = tmp;
	}
	(*env)->CallVoidMethod(env, xd->talk, jmGDInterfaceClip, x0, x1, y0, y1);
	chkX(env);
}

static void newJavaGD_Close(NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    JNIEnv *env = getJNIEnv();
    
    if(!env || !xd || !xd->talk) return;
	
	(*env)->CallVoidMethod(env, xd->talk, jmGDInterfaceClose);
	chkX(env);
}

static void newJavaGD_Deactivate(NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    JNIEnv *env = getJNIEnv();
    
    if(!env || !xd || !xd->talk) return;
	
	(*env)->CallVoidMethod(env, xd->talk, jmGDInterfaceDeactivate);
	chkX(env);
}

static Rboolean newJavaGD_Locator(double *x, double *y, NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    JNIEnv *env = getJNIEnv();
    
    if(!env || !xd || !xd->talk) return FALSE;
	
	jobject o=(*env)->CallObjectMethod(env, xd->talk, jmGDInterfaceLocator);
	if (o) {
		jdouble *ac=(jdouble*)(*env)->GetDoubleArrayElements(env, o, 0);
		if (!ac) {
			handleJGetArrayError(env, o, "gdLocator");
		}
		*x=ac[0]; *y=ac[1];
		(*env)->ReleaseDoubleArrayElements(env, o, ac, 0);
		(*env)->DeleteLocalRef(env, o);
		chkX(env);
		return TRUE;
	}
	chkX(env);
	
	return FALSE;
}

static void newJavaGD_Line(double x1, double y1, double x2, double y2,  R_GE_gcontext *gc,  NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    JNIEnv *env = getJNIEnv();
    
    if(!env || !xd || !xd->talk) return;
    
    checkGC(env,xd, gc);
	
	(*env)->CallVoidMethod(env, xd->talk, jmGDInterfaceLine, x1, y1, x2, y2);
	chkX(env);
}

static void newJavaGD_MetricInfo(int c,  R_GE_gcontext *gc,  double* ascent, double* descent,  double* width, NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    JNIEnv *env = getJNIEnv();
    
    if(!env || !xd || !xd->talk) return;
    
    checkGC(env,xd, gc);
    
    if(c <0) c = -c;
	jobject o=(*env)->CallObjectMethod(env, xd->talk, jmGDInterfaceMetricInfo, c);
	if (o) {
		jdouble *ac=(jdouble*)(*env)->GetDoubleArrayElements(env, o, 0);
		if (!ac) {
			handleJGetArrayError(env, o, "gdMetricInfo");
		}
		*ascent=ac[0]; *descent=ac[1]; *width=ac[2];
		(*env)->ReleaseDoubleArrayElements(env, o, ac, 0);
		(*env)->DeleteLocalRef(env, o);
	}
	chkX(env);
}

static void newJavaGD_Mode(int mode, NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    JNIEnv *env = getJNIEnv();
    
    if(!env || !xd || !xd->talk) return;
	
	(*env)->CallVoidMethod(env, xd->talk, jmGDInterfaceMode, mode);
	chkX(env);
}

static void newJavaGD_NewPage(R_GE_gcontext *gc, NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    JNIEnv *env = getJNIEnv();
    
    if(!env || !xd || !xd->talk) return;
	
	(*env)->CallVoidMethod(env, xd->talk, jmGDInterfaceNewPage, gc->fill);
	chkX(env);
	
	if (R_ALPHA(gc->fill) != 0) { // bg
		int savedCol = gc->col;
		gc->col = 0x00ffffff;
		sendAllGC(env, xd, gc);
		gc->col = savedCol;
		
		(*env)->CallVoidMethod(env, xd->talk, jmGDInterfaceRect, 
				dd->left, dd->top, dd->right, dd->bottom);
		chkX(env);
		
		checkGC(env, xd, gc);
	} else {
		sendAllGC(env, xd, gc);
	}
}

static Rboolean newJavaGD_NewPageConfirm(NewDevDesc *dd)
{
	newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
	JNIEnv *env = getJNIEnv();
	
	if (!env || !xd || !xd->talk) return FALSE;
	
	jboolean handled = (*env)->CallBooleanMethod(env, xd->talk, jmGDInterfaceNewPageConfirm);
	
	return (handled == JNI_TRUE) ? TRUE : FALSE;
}

static jarray newDoubleArrayPoly(JNIEnv *env, int n, double *ct)
{
    jdoubleArray da = (*env)->NewDoubleArray(env,n);
	if (!da) {
		handleJNewArrayError(env, "gdPoly*");
	}
    if (n>0) {
        jdouble *dae;
        dae = (*env)->GetDoubleArrayElements(env, da, 0);
		if (!dae) {
			handleJGetArrayError(env, da, "gdPoly*");
		}
        memcpy(dae,ct,sizeof(double)*n);
        (*env)->ReleaseDoubleArrayElements(env, da, dae, 0);
    }
	chkX(env);
    return da;
}

static void newJavaGD_Polygon(int n, double *x, double *y,  R_GE_gcontext *gc,  NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    JNIEnv *env = getJNIEnv();
    jarray xa, ya;
    
    if(!env || !xd || !xd->talk) return;

    checkGC(env,xd, gc);

    xa=newDoubleArrayPoly(env, n, x);
    ya=newDoubleArrayPoly(env, n, y);
	
	(*env)->CallVoidMethod(env, xd->talk, jmGDInterfacePolygon, n, xa, ya);
    (*env)->DeleteLocalRef(env, xa); 
    (*env)->DeleteLocalRef(env, ya);
	chkX(env);
}

static void newJavaGD_Polyline(int n, double *x, double *y,  R_GE_gcontext *gc,  NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    JNIEnv *env = getJNIEnv();
    jarray xa, ya;
    
    if(!env || !xd || !xd->talk) return;
    
    checkGC(env,xd, gc);
    
    xa=newDoubleArrayPoly(env, n, x);
    ya=newDoubleArrayPoly(env, n, y);
	
	(*env)->CallVoidMethod(env, xd->talk, jmGDInterfacePolyline, n, xa, ya);
    (*env)->DeleteLocalRef(env, xa); 
    (*env)->DeleteLocalRef(env, ya);
	chkX(env);
}

static void newJavaGD_Rect(double x0, double y0, double x1, double y1,  R_GE_gcontext *gc,  NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    JNIEnv *env = getJNIEnv();
    
    if(!env || !xd || !xd->talk) return;
    
    checkGC(env,xd, gc);
	
	(*env)->CallVoidMethod(env, xd->talk, jmGDInterfaceRect, x0, y0, x1, y1);
	chkX(env);
}

static void newJavaGD_Size(double *left, double *right,  double *bottom, double *top,  NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    JNIEnv *env = getJNIEnv();
    
    if(!env || !xd || !xd->talk) return;
	
	jobject o=(*env)->CallObjectMethod(env, xd->talk, jmGDInterfaceSize);
	if (o) {
		jdouble *ac=(jdouble*)(*env)->GetDoubleArrayElements(env, o, 0);
		if (!ac) {
			handleJGetArrayError(env, o, "gdSize");
		}
		*left=ac[0]; *right=ac[1]; *bottom=ac[2]; *top=ac[3];
		(*env)->ReleaseDoubleArrayElements(env, o, ac, 0);
		(*env)->DeleteLocalRef(env, o);
	} else gdWarning("gdSize: gdSize returned null");
	chkX(env);
}

static constxt char *convertToUTF8(constxt char *str, R_GE_gcontext *gc)
{
    if (gc->fontface == 5) /* symbol font needs re-coding to UTF-8 */
	str = symbol2utf8(str);
#ifdef translateCharUTF8
    else { /* first check whether we are dealing with non-ASCII at all */
	int ascii = 1;
	constxt unsigned char *c = (constxt unsigned char*) str;
	while (*c) { if (*c > 127) { ascii = 0; break; } c++; }
	if (!ascii) /* non-ASCII, we need to convert it to UTF8 */
	    str = translateCharUTF8(mkCharCE(str, CE_NATIVE));
    }
#endif
    return str;
}

static double newJavaGD_StrWidth(constxt char *str,  R_GE_gcontext *gc,  NewDevDesc *dd)
{
    return newJavaGD_StrWidthUTF8(convertToUTF8(str, gc), gc, dd);
}

static double newJavaGD_StrWidthUTF8(constxt char *str,  R_GE_gcontext *gc,  NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    JNIEnv *env = getJNIEnv();
    jstring s;
	double width;
    
    if(!env || !xd || !xd->talk) return 0.0;
    
    checkGC(env,xd, gc);

    s = (*env)->NewStringUTF(env, str);
	if (!s) {
		handleJNewStringError(env, "gdStrWidth");
	}
	width = (*env)->CallDoubleMethod(env, xd->talk, jmGDInterfaceStrWidth, s);
    /* s not released! */
	chkX(env);
    return width;
}

static void newJavaGD_Text(double x, double y, constxt char *str,  double rot, double hadj,  R_GE_gcontext *gc,  NewDevDesc *dd)
{
    newJavaGD_TextUTF8(x, y, convertToUTF8(str, gc), rot, hadj, gc, dd);
}

static void newJavaGD_TextUTF8(double x, double y, constxt char *str,  double rot, double hadj,  R_GE_gcontext *gc,  NewDevDesc *dd)
{
    newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
    JNIEnv *env = getJNIEnv();
    jstring s;
    
    if(!env || !xd || !xd->talk) return;
        
    checkGC(env,xd, gc);
    
    s = (*env)->NewStringUTF(env, str);
	if (!s) {
		handleJNewStringError(env, "gdText");
	}
	(*env)->CallVoidMethod(env, xd->talk, jmGDInterfaceText, x, y, s, rot, hadj);
	(*env)->DeleteLocalRef(env, s);  
	chkX(env);
}

static void newJavaGD_Raster(unsigned int *raster, int w, int h,
		double x, double y, double width, double height,
		double rot, Rboolean interpolate,
		R_GE_gcontext *gc, NewDevDesc *dd) {
	newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
	JNIEnv *env = getJNIEnv();
	jbyteArray jData;
	jbyte* ba;
	int i, j;
	
	if (!env || !xd || !xd->talk) return;
	
	int withAlpha = 0;
	int count = w * h;
	jData = (*env)->NewByteArray(env, count * 4);
	if (!jData) {
		handleJNewArrayError(env, "gdRaster");
	}
	ba = (*env)->GetByteArrayElements(env, jData, 0);
	if (!ba) {
		handleJGetArrayError(env, jData, "gdRaster");
	}
	for (i = 0, j = 0; i < count; i++, j+=4) {
		ba[j] = R_BLUE(raster[i]);
		ba[j+1] = R_GREEN(raster[i]);
		ba[j+2] = R_RED(raster[i]);
		if ((ba[j+3] = R_ALPHA(raster[i])) != (jbyte) 0xff) {
			withAlpha = 1;
		}
	}
	(*env)->ReleaseByteArrayElements(env, jData, ba, 0);
	(*env)->CallVoidMethod(env, xd->talk, jmGDInterfaceRaster, jData, (withAlpha) ? JNI_TRUE : JNI_FALSE,
			w, h, x, y, width, height, rot, interpolate );
	(*env)->DeleteLocalRef(env, jData);
	chkX(env);
}


/*-----------------------------------------------------------------------*/

/** fill the R device structure with callback functions */
void setupJavaGDfunctions(NewDevDesc *dd) {
    dd->close = newJavaGD_Close;
    dd->activate = newJavaGD_Activate;
    dd->deactivate = newJavaGD_Deactivate;
    dd->mode = newJavaGD_Mode;
    dd->size = newJavaGD_Size;
    dd->newPage = newJavaGD_NewPage;
    dd->canClip = TRUE;
    dd->clip = newJavaGD_Clip;
    dd->rect = newJavaGD_Rect;
    dd->circle = newJavaGD_Circle;
    dd->line = newJavaGD_Line;
    dd->polyline = newJavaGD_Polyline;
    dd->polygon = newJavaGD_Polygon;
	dd->raster = newJavaGD_Raster;
	
	dd->canHAdj = 2;
	dd->useRotatedTextInContour = TRUE;
	dd->metricInfo = newJavaGD_MetricInfo;
	dd->strWidth = newJavaGD_StrWidth;
	dd->text = newJavaGD_Text;
	dd->hasTextUTF8 = TRUE;
	dd->wantSymbolUTF8 = TRUE;
	dd->strWidthUTF8 = newJavaGD_StrWidthUTF8;
	dd->textUTF8 = newJavaGD_TextUTF8;
	
	dd->locator = newJavaGD_Locator;
	dd->newFrameConfirm = newJavaGD_NewPageConfirm;
}

/*--------- Java Initialization -----------*/

#ifdef Win32
#define PATH_SEPARATOR ';'
#else
#define PATH_SEPARATOR ':'
#endif
#define USER_CLASSPATH "."

#ifdef JNI_VERSION_1_2 
static JavaVMInitArgs vm_args;
static JavaVMOption *vm_options;
#else
#warning "** Java/JNI 1.2 or higher is required **"
** ERROR: Java/JNI 1.2 or higher is required **
/* we can't use #error to signal this on Windows due to a bug in the way dependencies are generated */
#endif

int initJVM(char *user_classpath) {
    JNIEnv *env;
    jint res;
    char *classpath;
    int total_num_properties, propNum = 0;
    
    if(!user_classpath)
        /* use the CLASSPATH environment variable as default */
        user_classpath = (char*) getenv("CLASSPATH");
    if(!user_classpath) user_classpath = "";
    
    vm_args.version = JNI_VERSION_1_2;
    if(JNI_GetDefaultJavaVMInitArgs(&vm_args) != JNI_OK)
      error("Java/JNI 1.2 or higher is required");
        
    total_num_properties = 3; /* leave room for classpath and optional jni debug */
        
    vm_options = (JavaVMOption *) calloc(total_num_properties, sizeof(JavaVMOption));
    vm_args.version = JNI_VERSION_1_2;
    vm_args.options = vm_options;
    vm_args.ignoreUnrecognized = JNI_TRUE;
    
    classpath = (char*) calloc(strlen("-Djava.class.path=") + strlen(user_classpath)+1, sizeof(char));
    sprintf(classpath, "-Djava.class.path=%s", user_classpath);
        
    vm_options[propNum++].optionString = classpath;   
    
    /* vm_options[propNum++].optionString = "-verbose:class,jni"; */
    vm_args.nOptions = propNum;
    /* Create the Java VM */
    res = JNI_CreateJavaVM(&jvm,(void **)&env, &vm_args);

    if (res != 0 || env == NULL) {
      error("Cannot create Java Virtual Machine");
      return -1;
    }
    return 0;
}

/*---------------- R-accessible functions -------------------*/

SEXP RJgd_initLib(SEXP cp) {
	JNIEnv *env = getJNIEnv();
	int i, l;
	
	if (!jvm) {
		initJVM(jarClassPath);
		env = getJNIEnv();
	}
	if (!env) error("missing JNIEnv");
	
	if (!isString(cp)) {
		error("cp is not a string vector");
	}
	l = LENGTH(cp);
	for (i = 0; i < l; i++) {
		SEXP elt = STRING_ELT(cp, i);
		if (elt != R_NaString) {
			addJClassPath(env, CHAR_UTF8(elt));
		}
	}
	
	return R_NilValue;
}

Rboolean createJavaGD(newJavaGDDesc *xd) {
	jclass jc = 0;
	jobject jo = 0;
	
	JNIEnv *env=getJNIEnv();
	
    if (!jvm) {
        initJVM(jarClassPath);
        env=getJNIEnv();
    }
    
    if (!env) return FALSE;
    
	char *customClass = getenv("RJGD_CLASS_NAME");
	if (!customClass) { 
		//customClass = "org.rosuda.javaGD.JavaGD";
		customClass = "de.walware.rj.server.gd.JavaGD";
	}
	
	if (!jcGDInterface) {
		jclass jc = getJClass(env, "org.rosuda.javaGD.GDInterface", (RJ_ERROR_RERROR | RJ_GLOBAL_REF));
		jmGDInterfaceActivate = getJMethod(env, jc, "gdActivate", "()V", RJ_ERROR_RERROR);
		jmGDInterfaceCircle = getJMethod(env, jc, "gdCircle", "(DDD)V", RJ_ERROR_RERROR);
		jmGDInterfaceClip = getJMethod(env, jc, "gdClip", "(DDDD)V", RJ_ERROR_RERROR);
		jmGDInterfaceClose = getJMethod(env, jc, "gdClose", "()V", RJ_ERROR_RERROR);
		jmGDInterfaceDeactivate = getJMethod(env, jc, "gdDeactivate", "()V", RJ_ERROR_RERROR);
		jmGDInterfaceGetPPI = getJMethod(env, jc, "gdPPI", "()[D", RJ_ERROR_RERROR);
		jmGDInterfaceInit = getJMethod(env, jc, "gdInit", "(DDIDDI)[D", RJ_ERROR_RERROR);
		jmGDInterfaceLocator = getJMethod(env, jc, "gdLocator", "()[D", RJ_ERROR_RERROR);
		jmGDInterfaceLine = getJMethod(env, jc, "gdLine", "(DDDD)V", RJ_ERROR_RERROR);
		jmGDInterfaceMetricInfo = getJMethod(env, jc, "gdMetricInfo", "(I)[D", RJ_ERROR_RERROR);
		jmGDInterfaceMode = getJMethod(env, jc, "gdMode", "(I)V", RJ_ERROR_RERROR);
		jmGDInterfaceNewPage = getJMethod(env, jc, "gdNewPage", "()V", RJ_ERROR_RERROR);
		jmGDInterfaceNewPageConfirm = getJMethod(env, jc, "gdNewPageConfirm", "()Z", RJ_ERROR_RERROR);
		jmGDInterfaceOpen = getJMethod(env, jc, "gdOpen", "(I)V", RJ_ERROR_RERROR);
		jmGDInterfacePolygon = getJMethod(env, jc, "gdPolygon", "(I[D[D)V", RJ_ERROR_RERROR);
		jmGDInterfacePolyline = getJMethod(env, jc, "gdPolyline", "(I[D[D)V", RJ_ERROR_RERROR);
		jmGDInterfaceRect = getJMethod(env, jc, "gdRect", "(DDDD)V", RJ_ERROR_RERROR);
		jmGDInterfaceSize = getJMethod(env, jc, "gdSize", "()[D", RJ_ERROR_RERROR);
		jmGDInterfaceStrWidth = getJMethod(env, jc, "gdStrWidth", "(Ljava/lang/String;)D", RJ_ERROR_RERROR);
		jmGDInterfaceText = getJMethod(env, jc, "gdText", "(DDLjava/lang/String;DD)V", RJ_ERROR_RERROR);
		jmGDInterfaceRaster = getJMethod(env, jc, "gdRaster", "([BZIIDDDDDZ)V", RJ_ERROR_RERROR);
		jmGDInterfaceSetColor = getJMethod(env, jc, "gdcSetColor", "(I)V", RJ_ERROR_RERROR);
		jmGDInterfaceSetFill = getJMethod(env, jc, "gdcSetFill", "(I)V", RJ_ERROR_RERROR);
		jmGDInterfaceSetLine = getJMethod(env, jc, "gdcSetLine", "(DI)V", RJ_ERROR_RERROR);
		jmGDInterfaceSetFont = getJMethod(env, jc, "gdcSetFont", "(DDDILjava/lang/String;)V", RJ_ERROR_RERROR);
		jcGDInterface = jc;
	}
	
	jc = getJClass(env, customClass, (RJ_ERROR_RERROR | RJ_GLOBAL_REF));
	{	jmethodID jm = (*env)->GetMethodID(env, jc, "<init>", "()V");
		if (!jm) {
			(*env)->DeleteLocalRef(env, jc);  
			handleJError(env, RJ_ERROR_RERROR, "Cannot find default constructor for GD class '%s'.", customClass);
		}
		jo = (*env)->NewObject(env, jc, jm);
		if (!jo) {
			(*env)->DeleteLocalRef(env, jc);  
			handleJError(env, RJ_ERROR_RERROR, "Cannot instantiate object of GD class '%s'.", customClass);
		}
	}
	
	xd->talk = (*env)->NewGlobalRef(env, jo);
	(*env)->DeleteLocalRef(env, jo);
	xd->talkClass = jc;
	
	if (!xd->talk) {
		chkX(env);
		gdWarning("Rjgd_NewDevice: talk is null");
		return FALSE;
	}
	
	return TRUE;
}

void initJavaGD(newJavaGDDesc *xd, double *width, double *height, int *unit, double *xpi, double *ypi) {
	JNIEnv *env = getJNIEnv();
	jobject jo;
	
	if(!env || !xd || !xd->talk) return;
	
	jo = (*env)->CallObjectMethod(env, xd->talk, jmGDInterfaceInit,
			(jdouble) *width, (jdouble) *height, (jint) *unit,
			(jdouble) *xpi, (jdouble) *ypi, xd->canvas);
	if (jo) {
		jdouble *ac = (jdouble*)(*env)->GetDoubleArrayElements(env, jo, 0);
		if (!ac) {
			if (*unit != 1) {
				*width = 672.0;
				*height = 672.0;
			}
			handleJGetArrayError(env, jo, "init");
		}
		*width = ac[0];
		*height = ac[1];
		(*env)->ReleaseDoubleArrayElements(env, jo, ac, 0);
		(*env)->DeleteLocalRef(env, jo);
	} else {
		gdWarning("gdInit: method returned null");
		if (*unit != 1) {
			*width = 672.0;
			*height = 672.0;
		}
	}
	chkX(env);
}

void openJavaGD(NewDevDesc *dd)
{	
	newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
	int devNr = ndevNumber(dd);
	
	JNIEnv *env = getJNIEnv();
	
	if (!env || !xd || !xd->talk || !jmGDInterfaceOpen) return;
	
	(*env)->CallVoidMethod(env, xd->talk, jmGDInterfaceOpen, (jint) devNr);
	chkX(env);
}

void getJavaGDPPI(NewDevDesc *dd, double *xpi, double *ypi) {
	newJavaGDDesc *xd = (newJavaGDDesc *) dd->deviceSpecific;
	JNIEnv *env = getJNIEnv();
	jobject jo;
	
	if (!env || !xd || !xd->talk) return;
	
	*xpi = 96.0;
	*ypi = 96.0;
	
	jo = (*env)->CallObjectMethod(env, xd->talk, jmGDInterfaceGetPPI);
	if (jo) {
		jdouble *ac = (jdouble*)(*env)->GetDoubleArrayElements(env, jo, 0);
		if (!ac) {
			handleJGetArrayError(env, jo, "getPPI");
		}
		if (ac[0] > 0.0 && ac[1] > 0.0) {
			*xpi = ac[0];
			*ypi = ac[1];
		}
		(*env)->ReleaseDoubleArrayElements(env, jo, ac, 0);
		(*env)->DeleteLocalRef(env, jo);
	} else {
		gdWarning("getPPI: method returned null, using default");
	}
	chkX(env);
}
