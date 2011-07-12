/* Copyright 2005 Cenqua Pty Ltd. All Rights Reserved. See LICENSE.TXT in the distribution. */

#include <jni.h>
#include "config.h"

#ifndef _Included_shaj_auth_h
#define _Included_shaj_auth_h

struct loginfo {
    jboolean isdebug;
    JNIEnv* jvm;
    jobject jLog;
};

typedef struct loginfo loginfo_t;

loginfo_t shaj_init_logger(JNIEnv* jvm, jobject jLog);

void shaj_log_debug(loginfo_t* logger, const char* fmt, ...);
void shaj_log_error(loginfo_t* logger, const char* fmt, ...);



#endif


