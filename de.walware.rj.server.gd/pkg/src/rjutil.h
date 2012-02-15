/* 
 * Copyright (C) 2011-2012 Stephan Wahlbrink and others.
 * 
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

// Utility methods for R package libs using JNI with R-Java classloader (from JRI or compatible)


#include <Rdefines.h>
#include <jni.h>

#ifndef RJUTIL_H
#define RJUTIL_H

#define CHAR_UTF8(s) (Rf_getCharCE(s) == CE_UTF8) ? CHAR(s) : Rf_reEnc(CHAR(s), Rf_getCharCE(s), CE_UTF8, 1)

#define RJ_ERROR_SWALLOW 0x1
#define RJ_ERROR_RWARNING 0x2
#define RJ_ERROR_RERROR 0x3
#define RJ_GLOBAL_REF 0x10

#endif


void addJClassPath(JNIEnv *env, const char *path);

jstring newJString(JNIEnv *env, const char *s, int flags);

jmethodID getJMethod(JNIEnv *env, jclass class, const char *name, const char *sig, int flags);
jclass getJClass(JNIEnv *env, const char *name, int flags);

void handleJError(JNIEnv *env, int flags, const char *message, ...);

void handleJNewArrayError(JNIEnv *env, const char *operation);
void handleJGetArrayError(JNIEnv *env, jobject jArray, const char *operation);
void handleJNewStringError(JNIEnv *env, const char *operation);

