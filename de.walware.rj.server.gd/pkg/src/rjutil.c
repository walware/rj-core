/* 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, a copy is available at
 * http://www.r-project.org/Licenses/
 */

#include <R_ext/Error.h>

#include "rjutil.h"


static int rjinit = 0;

static jclass jcClass = 0;
static jclass jcRJClassLoader = 0;
static jmethodID jmClassForName = 0;
static jobject joRJClassLoader = 0;
static jmethodID jmRJClassLoaderAddClassPath = 0;


static void rj_init(JNIEnv *env) {
	jclass jc;
	jmethodID jm;
	jobject jo;
	
	const char *errorMessage = "Init Java classloader support failed (%s).";
	
	jc = (*env)->FindClass(env, "java/lang/Class");
	if (!jc) {
		rjinit = -1;
		handleJError(env, RJ_ERROR_RERROR, errorMessage, "finding class 'java.lang.Class'");
	}
	jcClass = (*env)->NewGlobalRef(env, jc);
	if (!jcClass) {
		rjinit = -1;
		handleJError(env, RJ_ERROR_RERROR, errorMessage, "creating ref for 'jcClass'");
	}
	(*env)->DeleteLocalRef(env, jc);
	
	jm = (*env)->GetStaticMethodID(env, jcClass, "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;");
	if (!jm) {
		rjinit = -1;
		handleJError(env, RJ_ERROR_RERROR, errorMessage, "getting method 'java.lang.Class#forName(String, boolean, ClassLoader)'");
	}
	jmClassForName = jm;
	
	jc = (*env)->FindClass(env, "RJavaClassLoader");
	if (!jc) {
		rjinit = -2;
		handleJError(env, RJ_ERROR_RERROR, errorMessage, "finding class 'RJavaClassLoader'");
	}
	jcRJClassLoader = (*env)->NewGlobalRef(env, jc);
	if (!jcRJClassLoader) {
		rjinit = -1;
		handleJError(env, RJ_ERROR_RERROR, errorMessage, "creating ref for 'jcRJClassLoader'");
	}
	(*env)->DeleteLocalRef(env, jc);
	
	jm = (*env)->GetStaticMethodID(env, jcRJClassLoader, "getPrimaryLoader", "()LRJavaClassLoader;");
	if (!jm) {
		rjinit = -1;
		handleJError(env, RJ_ERROR_RERROR, errorMessage, "getting static method 'RJavaClassLoader#getPrimaryLoader()'");
	}
	jo = (*env)->CallStaticObjectMethod(env, jcRJClassLoader, jm);
	if (!jo) {
		rjinit = -1;
		handleJError(env, RJ_ERROR_RERROR, errorMessage, "calling 'RJavaClassLoader#getPrimaryLoader()'");
	}
	joRJClassLoader = (*env)->NewGlobalRef(env, jo);
	if (!joRJClassLoader) {
		rjinit = -1;
		handleJError(env, RJ_ERROR_RERROR, errorMessage, "creating ref for 'joRJavaClassLoader'");
	}
	(*env)->DeleteLocalRef(env, jo);
	
	jm = (*env)->GetMethodID(env, jcRJClassLoader, "addClassPath", "(Ljava/lang/String;)V");
	if (!jm) {
		rjinit = -1;
		handleJError(env, RJ_ERROR_RERROR, errorMessage, "getting method 'RJavaClassLoader#addClassPath(String)'");
	}
	jmRJClassLoaderAddClassPath = jm;
	
	rjinit = 1;
}


void addJClassPath(JNIEnv *env, const char *path) {
	if (rjinit != 1) {
		rj_init(env);
	}
	
	jstring jPath = newJString(env, path, RJ_ERROR_RERROR);
	(*env)->CallObjectMethod(env, joRJClassLoader, jmRJClassLoaderAddClassPath, jPath);
	(*env)->DeleteLocalRef(env, jPath);
}

jstring newJString(JNIEnv *env, const char *s, int flags) {
	jstring js = (*env)->NewStringUTF(env, s);
	if (!js) {
		handleJError(env, flags, "Creating new Java string '%s' failed.", s);
		return 0;
	}
	
	if ((flags & RJ_GLOBAL_REF) == RJ_GLOBAL_REF) {
		jstring global = (*env)->NewGlobalRef(env, js);
		(*env)->DeleteLocalRef(env, js);
		if (!global) {
			handleJError(env, flags, "Creating ref for Java string '%s' failed.", s);
			return 0;
		}
		return global;
	}
	else {
		return js;
	}
}

jclass getJClass(JNIEnv *env, const char *name, int flags) {
	jclass jc;
	
	if (rjinit != 1) {
		rj_init(env);
	}
	
	{	jstring js = newJString(env, name, flags);
		if (!js) {
			return 0;
		}
		jc = (*env)->CallStaticObjectMethod(env, jcClass, jmClassForName, js, JNI_TRUE,
				joRJClassLoader );
		(*env)->DeleteLocalRef(env, js);
	}
	if (!jc) {
		handleJError(env, flags, "Cannot find Java class '%s'.", name);
		return 0;
	}
	
	if ((flags & RJ_GLOBAL_REF) == RJ_GLOBAL_REF) {
		jclass global = (*env)->NewGlobalRef(env, jc);
		(*env)->DeleteLocalRef(env, jc);
		if (!global) {
			handleJError(env, flags, "Creating ref for Java class '%s' failed.", name);
			return 0;
		}
		return global;
	}
	else {
		return jc;
	}
}

jmethodID getJMethod(JNIEnv *env, jclass class, const char *name, const char *sig, int flags) {
	jmethodID jm;
	
	if (rjinit != 1) {
		rj_init(env);
	}
	
	jm = (*env)->GetMethodID(env, class, name, sig);
	if (!jm) {
		handleJError(env, flags, "Cannot get Java method '%s'.", name);
		return 0;
	}
	
	return jm;
}

void handleJError(JNIEnv *env, int flags, const char *message, ...) {
	if ((*env)->ExceptionCheck(env) == JNI_TRUE) {
#ifndef JGD_DEBUG
		(*env)->ExceptionDescribe(env);
#endif
		if (!(flags & 0x7)) {
			return;
		}
		
		(*env)->ExceptionClear(env);
	}
	
	if ((flags & 0x2)) {
		va_list ap;
		char msg[1024];
		msg[1023] = 0;
		
		va_start(ap, message);
		vsnprintf(msg, 1023, message, ap);
		va_end(ap);
		
		if ((flags & 0xf) == RJ_ERROR_RWARNING) {
			Rf_warning("[RJ-GD/R-Java] %s", msg);
		} else {
			Rf_error("[RJ-GD/R-Java] %s", msg);
		}
	}
}
