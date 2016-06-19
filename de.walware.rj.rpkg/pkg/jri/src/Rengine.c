/* Rengine - implements native rni methods called from the Rengine class */
#include <stdio.h>

#include "jri.h"
#include "org_rosuda_JRI_Rengine.h"
#include "rjava.h"
#include <Rversion.h>
#include <R_ext/Parse.h>
#include <errno.h>

#ifndef WIN32 /* this is for interrupt handing since GD uses R_interrupts_pending */
#include <R_ext/GraphicsEngine.h>
/* Before R 2.7.0 R_interrupts_pending was not included, though */
#if R_VERSION < R_Version(2,7,0)
LibExtern int R_interrupts_pending;
#endif
#endif

/* the # of arguments to R_ParseVector changed since R 2.5.0 */
#if R_VERSION < R_Version(2,5,0)
#define RS_ParseVector R_ParseVector
#else
#define RS_ParseVector(A,B,C) R_ParseVector(A,B,C,R_NilValue)
#endif

#include "Rcallbacks.h"
#include "Rinit.h"
#include "globals.h"
#include "Rdecl.h"

#ifdef Win32
#include <windows.h>
#ifdef _MSC_VER
__declspec(dllimport) int UserBreak;
#else
#ifndef WIN64
#define UserBreak     (*_imp__UserBreak)
#endif
extern int UserBreak;
#endif
#else

/* for R_runHandlers */
#include <R_ext/eventloop.h>
#include <signal.h>
#include <unistd.h>

#include <Rinternals.h>

#endif

#include <R_ext/GraphicsEngine.h>


static const jint FLAG_UNBOUND_P= 0x00000010;


JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniGetVersion
(JNIEnv *env, jclass clazz)
{
    return (jlong) JRI_API;
}

JNIEXPORT jint JNICALL Java_org_rosuda_JRI_Rengine_rniSetupR(
		JNIEnv *env, jobject this, jobjectArray a, jlong stackSize)
{
    int initRes;
    char *fallbackArgv[] = {"Rengine",0};
    char **argv = fallbackArgv;
    int argc = 1;

#ifdef JRI_DEBUG
    printf("rniSetupR\n");
#endif

    if (!env) {
        return 10101;
    }
    engineObj = (*env)->NewGlobalRef(env, this);
    engineClass = (*env)->NewGlobalRef(env, (*env)->GetObjectClass(env, engineObj));
    eenv = env;
    
    if (a) { /* retrieve the content of the String[] and construct argv accordingly */
        int len = (int)(*env)->GetArrayLength(env, a);
        if (len > 0) {
            int i = 0;
            argv = (char**) malloc(sizeof(char*)*(len+2));
            argv[0] = fallbackArgv[0];
            while (i < len) {
                jobject o = (*env)->GetObjectArrayElement(env, a, i);
                i++;
                if (!o) {
                    return 10201;
                }
                const char *c = (*env)->GetStringUTFChars(env, o, 0);
                if (!c) {
                    return 10202;
                }
                argv[i] = strdup(c);
                (*env)->ReleaseStringUTFChars(env, o, c);
            }
            argc = len+1;
            argv[argc] = 0;
        }
    }

    if (argc == 2 && !strcmp(argv[1], "--zero-init")) {/* special case for direct embedding (exp!) */
        initRinside();
        return 0;
    }
    
    initRes = initR(argc, argv, (unsigned long) stackSize);
    /* we don't release the argv in case R still needs it later (even if it shouldn't), but it's not really a significant leak */
    
    if (initRes != 0) {
        fflush(stderr);
    }
    return initRes;
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniParse
  (JNIEnv *env, jobject this, jstring str, jint parts)
{
      ParseStatus ps;
      SEXP pstr, cv;

      PROTECT(cv=jri_getString(env, str));
#ifdef JRI_DEBUG
      printf("parsing \"%s\"\n", CHAR(STRING_ELT(cv,0)));
#endif
      pstr=RS_ParseVector(cv, parts, &ps);
#ifdef JRI_DEBUG
      printf("parse status=%d, result=%x, type=%d\n", ps, (int) pstr, (pstr!=0)?TYPEOF(pstr):0);
#endif
      UNPROTECT(1);

      return SEXP2L(pstr);
}

/** 
 * Evaluates one expression or a list of expressions
 *
 * @param exp long reflection of the expression to evaluate
 * @param rho long reflection of the environment where to evaluate
 * 
 * @return 0 if an evaluation error ocurred or exp is 0
 */
JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniEval
  (JNIEnv *env, jobject this, jlong exp, jlong rho)
{
	SEXP es = R_NilValue, exps = L2SEXP(exp);
	SEXP eval_env = L2SEXP(rho);
	int er = 0;
	int i = 0, l;
	
	/* invalid (NULL) expression (parse error, ... ) */
	if (!exp) return 0;
	
	PROTECT(exps);
	if (TYPEOF(exps) == EXPRSXP) { 
		/* if the object is a list of exps, eval them one by one */
		l = LENGTH(exps);
		while (i < l) {
			es = R_tryEval(VECTOR_ELT(exps, i), eval_env, &er);
			
			/* an error occured, no need to continue */
			if (er) break;
			i++;
		}
	} else {
		es = R_tryEval(exps, eval_env, &er);
	}
	UNPROTECT(1);
	
	/* er is just a flag - on error return 0 */
	if (er) return 0;
	
	return SEXP2L(es);
}


JNIEXPORT void JNICALL Java_org_rosuda_JRI_Rengine_rniProtect
(JNIEnv *env, jobject this, jlong exp)
{
	PROTECT(L2SEXP(exp));
}

JNIEXPORT void JNICALL Java_org_rosuda_JRI_Rengine_rniUnprotect
(JNIEnv *env, jobject this, jint count)
{
	UNPROTECT(count);
}

JNIEXPORT void JNICALL Java_org_rosuda_JRI_Rengine_rniRelease
(JNIEnv *env, jobject this, jlong exp)
{
	if (exp) R_ReleaseObject(L2SEXP(exp));
}

JNIEXPORT void JNICALL Java_org_rosuda_JRI_Rengine_rniPreserve
(JNIEnv *env, jobject this, jlong exp)
{
	if (exp) R_PreserveObject(L2SEXP(exp));
}

JNIEXPORT void JNICALL Java_org_rosuda_JRI_Rengine_rniPrintValue
(JNIEnv *env, jobject this, jlong exp)
{
	Rf_PrintValue(exp ? L2SEXP(exp) : R_NilValue);
}


/*--- Env ---*/

JNIEXPORT jboolean JNICALL Java_org_rosuda_JRI_Rengine_rniIsNamespaceEnv(
		JNIEnv *env, jobject this, jlong rhoP) {
	SEXP rhoS= L2SEXP(rhoP);
	return (R_IsNamespaceEnv(rhoS)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL Java_org_rosuda_JRI_Rengine_rniGetNamespaceEnvName(
		JNIEnv *env, jobject this, jlong rhoP) {
	SEXP rhoS= L2SEXP(rhoP);
	return (R_IsNamespaceEnv(L2SEXP(rhoP))) ?
			jri_putString(env, R_NamespaceEnvSpec(rhoS), 0) :
			0;
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniParentEnv
(JNIEnv *env, jobject this, jlong exp)
{
  return SEXP2L(ENCLOS(exp ? L2SEXP(exp) : R_GlobalEnv));
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniListEnv
(JNIEnv *env, jobject this, jlong rho, jboolean all)
{
	return SEXP2L(R_lsInternal(rho ? L2SEXP(rho) : R_GlobalEnv, all));
}

struct safeGetVar_s {
	SEXP rhoS, symS, valS;
};

static void safeGetVar(void *data) {
	struct safeGetVar_s *s= (struct safeGetVar_s*) data;
	
	s->valS= Rf_findVarInFrame3(s->rhoS, s->symS, TRUE);
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniGetVar(
		JNIEnv *env, jobject this,
		jstring name, jlong rhoP) {
	struct safeGetVar_s s;
	
	s.symS= jri_installString(env, name);
	if (!s.symS || s.symS == R_NilValue) {
		return 0;
	}
	s.rhoS= (rhoP) ? L2SEXP(rhoP) : R_GlobalEnv;
	
	return (R_ToplevelExec(safeGetVar, (void*) &s)
					&& s.valS != R_UnboundValue) ?
			SEXP2L(s.valS) : 0;
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniGetVarBySym(
		JNIEnv *env, jobject this,
		jlong nameP, jlong rhoP, jint flags) {
	struct safeGetVar_s s;
	
	s.symS= L2SEXP(nameP);
	s.rhoS= (rhoP) ? L2SEXP(rhoP) : R_GlobalEnv;
	
	return (R_ToplevelExec(safeGetVar, (void*) &s)
					&& ((flags & FLAG_UNBOUND_P) || s.valS != R_UnboundValue) ) ?
			SEXP2L(s.valS) : 0;
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniFindVar
(JNIEnv *env, jobject this, jstring symName, jlong rho)
{
	SEXP sym = jri_installString(env, symName);
	if (!sym || sym == R_NilValue) return 0;

	return SEXP2L(Rf_findVar(sym, rho ? L2SEXP(rho) : R_GlobalEnv));
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniFindFunBySym(
		JNIEnv *env, jobject this,
		jlong nameP, jlong rhoP) {
	struct safeGetVar_s s;
	
	s.symS= L2SEXP(nameP);
	s.rhoS= (rhoP) ? L2SEXP(rhoP) : R_GlobalEnv;
	
	while (s.rhoS != R_EmptyEnv) {
		if (R_ToplevelExec(safeGetVar, (void*) &s)) {
			SEXP valS= s.valS;
			if (valS != R_UnboundValue) {
				if (TYPEOF(valS) == PROMSXP) {
					if (PRVALUE(valS) != R_UnboundValue) {
						valS= PRVALUE(valS);
					} else {
						switch (TYPEOF(PRCODE(valS))) {
						case SPECIALSXP:
						case BUILTINSXP:
						case CLOSXP:
							valS= Rf_eval(valS, R_BaseEnv);
						}
					}
					if (valS == R_UnboundValue) {
						return 0;
					}
				}
				switch (TYPEOF(valS)) {
				case CLOSXP:
				case BUILTINSXP:
				case SPECIALSXP:
					return SEXP2L(valS);
				default:
					if (valS == R_MissingArg) {
						return 0;
					}
				}
			}
			s.rhoS= ENCLOS(s.rhoS);
		}
		else {
			return 0;
		}
	}
	return 0;
}

struct safeAssign_s {
	SEXP symS, valS, rhoS;
};

static void safeAssign(void *data) {
	struct safeAssign_s *s = (struct safeAssign_s*) data;
	
	Rf_defineVar(s->symS, s->valS, s->rhoS);
}

JNIEXPORT jboolean JNICALL Java_org_rosuda_JRI_Rengine_rniAssign(
		JNIEnv *env, jobject this,
		jstring name, jlong valP, jlong rhoP) {
	struct safeAssign_s s;
	
	s.symS= jri_installString(env, name);
	if (!s.symS || s.symS == R_NilValue) {
		return JNI_FALSE;
	}
	s.valS= (valP) ? L2SEXP(valP) : R_NilValue;
	s.rhoS= (rhoP) ? L2SEXP(rhoP) : R_GlobalEnv;
	
	/* we have to use R_ToplevelExec because defineVar may fail on locked bindings */
	return (R_ToplevelExec(safeAssign, (void*) &s)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_org_rosuda_JRI_Rengine_rniAssignVarBySym(
		JNIEnv *env, jobject this,
		jlong nameP, jlong valP, jlong rhoP) {
	struct safeAssign_s s;
	
	s.symS= L2SEXP(nameP);
	s.valS= (valP) ? L2SEXP(valP) : R_NilValue;
	s.rhoS= (rhoP) ? L2SEXP(rhoP) : R_GlobalEnv;
	
	/* we have to use R_ToplevelExec because defineVar may fail on locked bindings */
	return (R_ToplevelExec(safeAssign, (void*) &s)) ? JNI_TRUE : JNI_FALSE;
}


JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniGetPromise(
		JNIEnv *env, jobject this, jlong p, jint flags)
{
	SEXP s = L2SEXP(p);
	SEXP sVal = PRVALUE(s);
	int er = 0;
	if (sVal == R_UnboundValue) {
		if (!(flags & 0x7)) {
			return 0;
		}
		switch (TYPEOF(PRCODE(s))) {
		case NILSXP:
		case LISTSXP:
		case INTSXP:
		case REALSXP:
		case STRSXP:
		case CPLXSXP:
		case RAWSXP:
		case S4SXP:
		case SPECIALSXP:
		case BUILTINSXP:
		case ENVSXP:
		case CLOSXP:
		case VECSXP:
		case EXTPTRSXP:
		case WEAKREFSXP:
			sVal = Rf_eval(s, R_BaseEnv);
			break;
		default:
			if ((flags & 0x7) == 1) {
				return 0;
			}
			sVal = R_tryEval(s, R_BaseEnv, &er);
			break;
		}
	}
	return (!er && sVal != R_UnboundValue) ? SEXP2L(sVal) : 0;
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniSpecialObject
(JNIEnv *env, jobject this, jint which)
{
  switch (which) {
  case 0: return SEXP2L(R_NilValue);
  case 1: return SEXP2L(R_GlobalEnv);
  case 2: return SEXP2L(R_EmptyEnv);
  case 3: return SEXP2L(R_BaseEnv);
  case 4: return SEXP2L(R_UnboundValue);
  case 5: return SEXP2L(R_MissingArg);
  case 6: return SEXP2L(R_NaString);
  case 7: return SEXP2L(R_BlankString);
  case 13: return SEXP2L(R_BaseNamespace);
  }
  return 0;
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniGetOptionBySym(
		JNIEnv *env, jobject this, jlong symP) {
	SEXP valS= Rf_GetOption1(L2SEXP(symP));
	return (valS != R_NilValue) ? SEXP2L(valS) : 0;
}



/*--- Debug ---*/

JNIEXPORT jint JNICALL Java_org_rosuda_JRI_Rengine_rniGetDebug(
		JNIEnv *env, jobject this, jlong p)
{	
	return RDEBUG((p) ? L2SEXP(p) : R_GlobalEnv);
}

JNIEXPORT jboolean JNICALL Java_org_rosuda_JRI_Rengine_rniSetDebug(
		JNIEnv *env, jobject this, jlong p, jint v)
{	SEXP s = (p) ? L2SEXP(p) : R_GlobalEnv;
	if (RDEBUG(s) != v) {
		SET_RDEBUG(s, v);
		return JNI_TRUE;
	}
	return JNI_FALSE;
}


//---

JNIEXPORT jobject JNICALL Java_org_rosuda_JRI_Rengine_rniXrefToJava
(JNIEnv *env, jobject this, jlong exp)
{
	SEXP xp = L2SEXP(exp);
	if (TYPEOF(xp) != EXTPTRSXP) return 0;
	return (jobject) EXTPTR_PTR(xp);
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniJavaToXref
(JNIEnv *env, jobject this, jobject o)
{
  /* this is pretty much from Rglue.c of rJava */
  jobject go = (*env)->NewGlobalRef(env, o);
  return SEXP2L(R_MakeExternalPtr(go, R_NilValue, R_NilValue));
}


JNIEXPORT jint JNICALL Java_org_rosuda_JRI_Rengine_rniExpType(
		JNIEnv *env, jobject this, jlong p)
{
	return p ? TYPEOF(L2SEXP(p)) : 0;
}

JNIEXPORT jstring JNICALL Java_org_rosuda_JRI_Rengine_rniGetClassAttrString(
		JNIEnv *env, jobject this, jlong p)
{
	return jri_putString(env, Rf_getAttrib(L2SEXP(p), R_ClassSymbol), 0);
}

JNIEXPORT jboolean JNICALL Java_org_rosuda_JRI_Rengine_rniIsS4(
		JNIEnv *env, jobject this, jlong p)
{
	return (IS_S4_OBJECT(L2SEXP(p)) == TRUE) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL Java_org_rosuda_JRI_Rengine_rniGetLength(
		JNIEnv *env, jobject this, jlong p)
{
	return (p) ? (jint) Rf_length(L2SEXP(p)) : 0;
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniGetVectorLength(
		JNIEnv *env, jobject this, jlong p)
{
#if R_VERSION < R_Version(3,0,0)
	return (p) ? (jlong) LENGTH(L2SEXP(p)) : 0;
#else
	return (p) ? (jlong) XLENGTH(L2SEXP(p)) : 0;
#endif
}

JNIEXPORT jarray JNICALL Java_org_rosuda_JRI_Rengine_rniGetArrayDim(
		JNIEnv *env, jobject this, jlong p)
{
	SEXP sDim = Rf_getAttrib(L2SEXP(p), R_DimSymbol);
	if (TYPEOF(sDim) == INTSXP && LENGTH(sDim) > 0) {
		return jri_putIntArray(env, sDim);
	}
	return 0;
}


JNIEXPORT jstring JNICALL Java_org_rosuda_JRI_Rengine_rniGetString
  (JNIEnv *env, jobject this, jlong exp)
{
      return jri_putString(env, L2SEXP(exp), 0);
}

JNIEXPORT jobjectArray JNICALL Java_org_rosuda_JRI_Rengine_rniGetStringArray
  (JNIEnv *env, jobject this, jlong exp)
{
      return jri_putStringArray(env, L2SEXP(exp));
}

JNIEXPORT jintArray JNICALL Java_org_rosuda_JRI_Rengine_rniGetIntArray
  (JNIEnv *env, jobject this, jlong exp)
{
      return jri_putIntArray(env, L2SEXP(exp));
}

JNIEXPORT jbyteArray JNICALL Java_org_rosuda_JRI_Rengine_rniGetRawArray
  (JNIEnv *env, jobject this, jlong exp)
{
      return jri_putByteArray(env, L2SEXP(exp));
}

JNIEXPORT jboolean JNICALL Java_org_rosuda_JRI_Rengine_rniIsTrue(
		JNIEnv *env, jobject this, jlong p)
{
	SEXP s = L2SEXP(p);
	if (TYPEOF(s) != LGLSXP || LENGTH(s) != 1) {
		return JNI_FALSE;
	}
	// LOGICAL(s)[0] != 0 && LOGICAL(s)[0] != 2
	return (LOGICAL(s)[0] == TRUE) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jintArray JNICALL Java_org_rosuda_JRI_Rengine_rniGetBoolArrayI
  (JNIEnv *env, jobject this, jlong exp)
{
      return jri_putBoolArrayI(env, L2SEXP(exp));
}

JNIEXPORT jintArray JNICALL Java_org_rosuda_JRI_Rengine_rniGetDoubleArray
  (JNIEnv *env, jobject this, jlong exp)
{
      return jri_putDoubleArray(env, L2SEXP(exp));
}

JNIEXPORT jlongArray JNICALL Java_org_rosuda_JRI_Rengine_rniGetVector(
		JNIEnv *env, jobject this, jlong p)
{
	SEXP s = L2SEXP(p);
	if (TYPEOF(s) != VECSXP && TYPEOF(s) != EXPRSXP) {
		return 0;
	}
	return jri_putSEXPLArray(env, s);
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniGetVectorElt(
		JNIEnv *env, jobject this, jlong p, jint index)
{	
	SEXP s = L2SEXP(p);
	if ((TYPEOF(s) != VECSXP && TYPEOF(s) != EXPRSXP)
			|| index < 0 || index >= LENGTH(s)) {
		return 0;
	}
	return SEXP2L(VECTOR_ELT(s, index));
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniGetCloBodyExpr(
		JNIEnv *env, jobject this, jlong clo)
{	
	SEXP s = L2SEXP(clo);
	if (TYPEOF(s) != CLOSXP) {
		return 0;
	}
	return SEXP2L(BODY_EXPR(s));
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniGetCloEnv(
		JNIEnv *env, jobject this, jlong clo)
{	
	SEXP s = L2SEXP(clo);
	if (TYPEOF(s) != CLOSXP) {
		return 0;
	}
	return SEXP2L(CLOENV(s));
}

JNIEXPORT jboolean JNICALL Java_org_rosuda_JRI_Rengine_rniSetCloBody(
		JNIEnv *env, jobject this, jlong clo, jlong p)
{	
	SEXP s = L2SEXP(clo);
	if (TYPEOF(s) != CLOSXP) {
		return JNI_FALSE;
	}
	SET_BODY(s, L2SEXP(p));
	return JNI_TRUE;
}


JNIEXPORT void JNICALL Java_org_rosuda_JRI_Rengine_rniIdle
  (JNIEnv *env, jobject this)
{
#ifndef Win32
    if (!R_interrupts_pending) {
        R_runHandlers(R_InputHandlers, R_checkActivity(0, 1));
    }
#endif
}

JNIEXPORT void JNICALL Java_org_rosuda_JRI_Rengine_rniRunMainLoop
  (JNIEnv *env, jobject this)
{
      run_Rmainloop();
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniPutString
(JNIEnv *env, jobject this, jstring s)
{
    return SEXP2L(jri_getString(env, s));
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniPutStringByStr(
		JNIEnv *env, jobject this, jlong strP, jint idx) {
	SEXP resultS;
	PROTECT(resultS= allocVector(STRSXP, 1));
	SET_STRING_ELT(resultS, 0, STRING_ELT(L2SEXP(strP), idx));
	UNPROTECT(1);
	return SEXP2L(resultS);
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniPutStringArray
(JNIEnv *env, jobject this, jobjectArray a)
{
    return SEXP2L(jri_getStringArray(env, a));
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniPutIntArray
(JNIEnv *env, jobject this, jintArray a)
{
    return SEXP2L(jri_getIntArray(env, a));
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniPutRawArray
(JNIEnv *env, jobject this, jbyteArray a)
{
    return SEXP2L(jri_getByteArray(env, a));
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniPutBoolArrayI
(JNIEnv *env, jobject this, jintArray a)
{
    return SEXP2L(jri_getBoolArrayI(env, a));
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniPutBoolArray
(JNIEnv *env, jobject this, jbooleanArray a)
{
    return SEXP2L(jri_getBoolArray(env, a));
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniPutDoubleArray
(JNIEnv *env, jobject this, jdoubleArray a)
{
    return SEXP2L(jri_getDoubleArray(env, a));
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniPutVector
(JNIEnv *env, jobject this, jlongArray a)
{
    return SEXP2L(jri_getSEXPLArray(env, a));
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniDuplicate(
		JNIEnv *env, jobject this, jlong p)
{
	return SEXP2L(Rf_duplicate(L2SEXP(p)));
}


JNIEXPORT jobjectArray JNICALL Java_org_rosuda_JRI_Rengine_rniGetAttrNames
(JNIEnv *env, jobject this, jlong exp)
{
    SEXP o = L2SEXP(exp);
    SEXP att = ATTRIB(o), ah = att;
    unsigned int ac = 0;
    jobjectArray sa;
    if (att == R_NilValue) return 0;
    /* count the number of attributes */
    while (ah != R_NilValue) {
	ac++;
	ah = CDR(ah);
    }
    /* allocate Java array */
    sa = (*env)->NewObjectArray(env, ac, (*env)->FindClass(env, "java/lang/String"), 0);
    if (!sa) return 0;
    ac = 0;
    ah = att;
    /* iterate again and set create the strings */
    while (ah != R_NilValue) {
	SEXP t = TAG(ah);
	if (t != R_NilValue) {
	    jobject s = (*env)->NewStringUTF(env, CHAR_UTF8(PRINTNAME(t)));
	    (*env)->SetObjectArrayElement(env, sa, ac, s);
	}
	ac++;
	ah = CDR(ah);
    }
    return sa;
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniGetAttr(
		JNIEnv *env, jobject this, jlong p, jstring name)
{
    SEXP an = jri_installString(env, name);
    if (!an || an==R_NilValue || p==0 || L2SEXP(p)==R_NilValue) return 0;
    {
        SEXP a = getAttrib(L2SEXP(p), an);
        return (a==R_NilValue)?0:SEXP2L(a);
    }
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniGetAttrBySym(
		JNIEnv *env, jobject this, jlong p, jlong name)
{
	SEXP attrS = Rf_getAttrib(L2SEXP(p), L2SEXP(name));
	return (attrS != R_NilValue) ? SEXP2L(attrS) : 0;
}

JNIEXPORT jstring JNICALL Java_org_rosuda_JRI_Rengine_rniGetAttrStringBySym(
		JNIEnv *env, jobject this, jlong p, jlong name) {
	return jri_putString(env, Rf_getAttrib(L2SEXP(p), L2SEXP(name)), 0);
}

JNIEXPORT void JNICALL Java_org_rosuda_JRI_Rengine_rniSetAttr(
		JNIEnv *env, jobject this, jlong p, jstring aName, jlong attr)
{
    SEXP an = jri_installString(env, aName);
    if (!an || an==R_NilValue || p==0 || L2SEXP(p)==R_NilValue) return;

    setAttrib(L2SEXP(p), an, (attr==0) ? R_NilValue : L2SEXP(attr));
	
	/* BTW: we don't need to adjust the object bit for "class", setAttrib does that already */

    /* this is not official API, but whoever uses this should know what he's doing
       it's ok for directly constructing attr lists, and that's what it should be used for
       SET_ATTRIB(L2SEXP(exp), (attr==0)?R_NilValue:L2SEXP(attr)); */
    
}

JNIEXPORT void JNICALL Java_org_rosuda_JRI_Rengine_rniSetAttrBySym(
		JNIEnv *env, jobject this, jlong p, jlong name, jlong attr)
{
	setAttrib(L2SEXP(p), L2SEXP(name), (attr == 0) ? R_NilValue : L2SEXP(attr));
}	

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniInstallSymbol
(JNIEnv *env, jobject this, jstring s)
{
    return SEXP2L(jri_installString(env, s));
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniInstallSymbolByStr(
		JNIEnv *env, jobject this, jlong strP, jint idx) {
	return SEXP2L(install(CHAR(STRING_ELT(L2SEXP(strP), idx))));
}

JNIEXPORT jstring JNICALL Java_org_rosuda_JRI_Rengine_rniGetSymbolName
(JNIEnv *env, jobject this, jlong exp)
{
	return jri_putSymbolName(env, L2SEXP(exp));
}

JNIEXPORT jboolean JNICALL Java_org_rosuda_JRI_Rengine_rniInherits
(JNIEnv *env, jobject this, jlong exp, jstring s)
{
	jboolean res = 0;
	const char *c;
	c=(*env)->GetStringUTFChars(env, s, 0);
	if (c) {
		if (inherits(L2SEXP(exp), (char*)c)) res = 1;
		(*env)->ReleaseStringUTFChars(env, s, c);
	}
	return res;
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniCons
(JNIEnv *env, jobject this, jlong head, jlong tail, jlong tag, jboolean lang)
{
  SEXP l;
  if (lang)
    l = LCONS((head==0)?R_NilValue:L2SEXP(head), (tail==0)?R_NilValue:L2SEXP(tail));
  else
    l = CONS((head==0)?R_NilValue:L2SEXP(head), (tail==0)?R_NilValue:L2SEXP(tail));
  
  if (tag) SET_TAG(l, L2SEXP(tag));
  return SEXP2L(l);
}

JNIEXPORT void JNICALL Java_org_rosuda_JRI_Rengine_rniSetCAR(
		JNIEnv *env, jobject this, jlong lang, jlong v)
{
	SETCAR(L2SEXP(lang), L2SEXP(v));
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniCAR
(JNIEnv *env, jobject this, jlong exp)
{
    if (exp) {
        SEXP r = CAR(L2SEXP(exp));
        return (r==R_NilValue)?0:SEXP2L(r);
    }
    return 0;
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniCDR
(JNIEnv *env, jobject this, jlong exp)
{
    if (exp) {
        SEXP r = CDR(L2SEXP(exp));
        return (r && r != R_NilValue && TYPEOF(r) == LISTSXP) ? SEXP2L(r) : 0;
    }
    return 0;
}

JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniTAG
(JNIEnv *env, jobject this, jlong exp)
{
    if (exp) {
        SEXP r = TAG(L2SEXP(exp));
        return (r==R_NilValue)?0:SEXP2L(r);
    }
    return 0;
}

/* creates a list from SEXPs provided in long[] */
JNIEXPORT jlong JNICALL Java_org_rosuda_JRI_Rengine_rniPutList
(JNIEnv *env, jobject this, jlongArray o)
{
    SEXP t=R_NilValue;
    int l,i=0;
    jlong *ap;
    
    if (!o) return 0;
    l=(int)(*env)->GetArrayLength(env, o);
    if (l<1) return SEXP2L(CONS(R_NilValue, R_NilValue));
    ap=(jlong*)(*env)->GetLongArrayElements(env, o, 0);
    if (!ap) return 0;
    
    while(i<l) {
        t=CONS((ap[i]==0)?R_NilValue:L2SEXP(ap[i]), t);
        i++;
    }
    (*env)->ReleaseLongArrayElements(env, o, ap, 0);    
    
    return SEXP2L(t);
}

/* retrieves a list (shallow copy) and returns the SEXPs in long[] */
JNIEXPORT jlongArray JNICALL Java_org_rosuda_JRI_Rengine_rniGetList
(JNIEnv *env, jobject this, jlong exp)
{
    SEXP e=L2SEXP(exp);
    
    if (exp==0 || e==R_NilValue) return 0;

    {
        unsigned len=0;
        SEXP t=e;
        
        while (t!=R_NilValue) { t=CDR(t); len++; };
        
        {
            jlongArray da=(*env)->NewLongArray(env,len);
            jlong *dae;
        
            if (!da) return 0;
        
            if (len>0) {
                int i=0;
                dae=(*env)->GetLongArrayElements(env, da, 0);
                if (!dae) {
                    (*env)->DeleteLocalRef(env,da);
                    jri_error("rniGetList: newLongArray.GetLongArrayElements failed");
                    return 0;
                }

                t=e;
                while (t!=R_NilValue && i<len) {
                    dae[i]=(CAR(t)==R_NilValue)?0:SEXP2L(CAR(t));
                    i++; t=CDR(t);
                }
                
                (*env)->ReleaseLongArrayElements(env, da, dae, 0);
            }
            
            return da;
        }
    }
    
}

/* by default those are disabled as it's a problem on Win32 ... */
#ifdef JRI_ENV_CALLS

JNIEXPORT void JNICALL Java_org_rosuda_JRI_Rengine_rniSetEnv
(JNIEnv *env, jclass this, jstring key, jstring val) {
    const char *cKey, *cVal;
    if (!key || !val) return;
    cKey=(*env)->GetStringUTFChars(env, key, 0);
    cVal=(*env)->GetStringUTFChars(env, val, 0);
    if (!cKey || !cVal) {
        jri_error("rniSetEnv: can't retrieve key/value content");
        return;
    }
#ifdef Win32
    SetEnvironmentVariable(cKey, cVal);
#else
    setenv(cKey, cVal, 1);
#endif
    (*env)->ReleaseStringUTFChars(env, key, cKey);
    (*env)->ReleaseStringUTFChars(env, val, cVal);
}

JNIEXPORT jstring JNICALL Java_org_rosuda_JRI_Rengine_rniGetEnv
(JNIEnv *env, jclass this, jstring key) {
    const char *cKey, *cVal;
    if (!key) return;
    cKey=(*env)->GetStringUTFChars(env, key, 0);
    if (!cKey) {
        jri_error("rniSetEnv: can't retrieve key/value content");
        return;
    }
    cVal=getenv(cKey);
    (*env)->ReleaseStringUTFChars(env, key, cKey);
    if (!cVal) return 0;
    return (*env)->NewStringUTF(env, cVal);
}

#endif

JNIEXPORT jint JNICALL Java_org_rosuda_JRI_Rengine_rniSetupRJava
(JNIEnv *env, jobject this, jint _in, jint _out) {
  RJava_setup(_in, _out);
  return 0;
}

JNIEXPORT jint JNICALL Java_org_rosuda_JRI_Rengine_rniRJavaLock
(JNIEnv *env, jobject this) {
  return RJava_request_lock();
}

JNIEXPORT jint JNICALL Java_org_rosuda_JRI_Rengine_rniRJavaUnlock
(JNIEnv *env, jobject this) {
  return RJava_clear_lock();
}

JNIEXPORT void JNICALL Java_org_rosuda_JRI_Rengine_rniPrint
(JNIEnv *env, jobject this, jstring s, jint oType) {
  if (s) {
    const char *c = (*env)->GetStringUTFChars(env, s, 0);
    if (c) {
      if (oType)
	REprintf("%s", c);
      else
	Rprintf("%s", c);
    }
    (*env)->ReleaseStringUTFChars(env, s, c);
  }
}

JNIEXPORT jint JNICALL Java_org_rosuda_JRI_Rengine_rniStop
(JNIEnv *env, jobject this, jint flag) {
#ifdef Win32
    UserBreak=1;
#else
    /* there are three choices now:
       0 = cooperative (requires external interrupt of ReadConsole!)
       1 = SIGINT for compatibility with old rniStop()
       2 = R's onintr but that one works *only* if used on the R thread (which renders is essentially useless unless used in some synchronous interrupt handler). */
    if (flag == 0) R_interrupts_pending = 1;
    else if (flag == 1)  kill(getpid(), SIGINT);
    else Rf_onintr();
#endif
    return 0;
}

JNIEXPORT jint JNICALL Java_org_rosuda_JRI_Rengine_rniSetProcessJEvents(
		JNIEnv *env, jobject this, jint flag) {
	Re_ProcessJEventsRequested = (int) flag;
	return 0;
}


JNIEXPORT jint JNICALL Java_org_rosuda_JRI_Rengine_rniGDClose(
		JNIEnv *env, jobject this, jint devId) {
	pGEDevDesc dd = GEgetDevice(devId);
	if (!dd) {
		return 12;
	}
	Rf_killDevice(devId);
	return 0;
}

JNIEXPORT jint JNICALL Java_org_rosuda_JRI_Rengine_rniGDResize(
		JNIEnv *env, jobject this, jint devId) {
	pGEDevDesc gd = GEgetDevice(devId);
	if (!gd) {
		return 12;
	}
	pDevDesc dd = gd->dev;
	if (!dd) {
		return 13;
	}
	dd->size(&(dd->left), &(dd->right), &(dd->bottom), &(dd->top), dd);
	GEplayDisplayList(gd);
	return 0;
}

