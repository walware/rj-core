/* Copyright 2005 Cenqua Pty Ltd. All Rights Reserved. See LICENSE.TXT in the distribution. */
#include <stdlib.h>
#include <stdarg.h>
#include "shaj_auth.h"
#include "config.h"

loginfo_t shaj_init_logger(JNIEnv* jvm, jobject jLog)
{
    loginfo_t logger;
    logger.isdebug = JNI_FALSE;
    logger.jvm = jvm;
    logger.jLog = jLog;

    if (jLog != NULL) {
        jclass cls = (*jvm)->GetObjectClass(jvm, jLog);
        jmethodID mid = (*jvm)->GetMethodID(jvm, cls, "isDebug", "()Z");
        if (mid != NULL) {
            logger.isdebug = (*jvm)->CallBooleanMethod(jvm, jLog, mid);
        }
    }

    return logger;
    
}

#define MAXBUF 1000

static void dolog(JNIEnv* jvm, jobject jLog, char* methodname, const char* fmt, va_list ap) {

    jstring jstr;
    jclass cls;
    jmethodID mid;
    char* buf = calloc(MAXBUF, sizeof(char));
    
    if (buf == NULL) {
        return;
    }

    vsnprintf(buf, MAXBUF, fmt, ap);

    jstr = (*jvm)->NewStringUTF(jvm, buf);
    
    cls = (*jvm)->GetObjectClass(jvm, jLog);
    mid = (*jvm)->GetMethodID(jvm, cls, methodname, "(Ljava/lang/String;)V");
    if (mid != NULL) {
        (*jvm)->CallVoidMethod(jvm, jLog, mid, jstr);
    }

    if (jstr != NULL) {
        (*jvm)->DeleteLocalRef(jvm, jstr);
    }

    free(buf);
}

void shaj_log_debug(loginfo_t* logger, const char* fmt, ...) {
    va_list ap;
    
    if (logger == NULL) {
        return;
    }
    
    if (!(logger->isdebug)) {
        return;
    }
    if ((logger->jvm == NULL) || (logger->jLog == NULL)) {
        return;
    }

    va_start(ap, fmt);
    dolog(logger->jvm, logger->jLog, "debug", fmt, ap);
    va_end(ap);
}
void shaj_log_error(loginfo_t* logger, const char* fmt, ...) {
    va_list ap;
    
    if (logger == NULL) {
        return;
    }
    
    if ((logger->jvm == NULL) || (logger->jLog == NULL)) {
        return;
    }

    va_start(ap, fmt);
    dolog(logger->jvm, logger->jLog, "error", fmt, ap);
    va_end(ap);
}
