/* Copyright 2005 Cenqua Pty Ltd. All Rights Reserved. See LICENSE.TXT in the distribution. */
#include <jni.h>
#include <stdio.h>
#include "../com_cenqua_shaj_PAMAuthenticator.h"
#include "../shaj_auth.h"
#include "shaj_pam.h"
#include "shaj_group.h"


/*
 * this prevents us init-ing twice (for example, a class being
 * collected then loaded again)
 */
static jboolean HAVE_LOADED = JNI_FALSE;
static jboolean HAVE_LOADED_RESULT = JNI_FALSE;

JNIEXPORT jboolean JNICALL Java_com_cenqua_shaj_Shaj_initlibrary
  (JNIEnv * jvm, jclass jThisClass, jobject jLog)
{
    loginfo_t loginfo;

    if (HAVE_LOADED == JNI_TRUE) {
        return HAVE_LOADED_RESULT;
    }
    
    loginfo = shaj_init_logger(jvm, jLog);
    
    HAVE_LOADED_RESULT = shaj_init(&loginfo);
    HAVE_LOADED = JNI_TRUE;

    return HAVE_LOADED_RESULT;
}

JNIEXPORT jboolean JNICALL Java_com_cenqua_shaj_PAMAuthenticator_checkPasswordImpl
  (JNIEnv *jvm, jclass jThisClass, jstring aService, jstring aUsername, jstring aPassword, jobject jLog)
{
    const char* c_service;
    const char* c_username;
    const char* c_password;
    jboolean result;
    loginfo_t loginfo;
    
    loginfo = shaj_init_logger(jvm, jLog);

    //TODO do a check for username=="" or null
    //TODO what is the correct "default" servicename if null?
    
    c_service = (*jvm)->GetStringUTFChars(jvm, aService, NULL);
    c_username = (*jvm)->GetStringUTFChars(jvm, aUsername, NULL);
    c_password = (*jvm)->GetStringUTFChars(jvm, aPassword, NULL);

    result = shaj_chkpasswd_pam(c_service, c_username, c_password, &loginfo);
    
    (*jvm)->ReleaseStringUTFChars(jvm, aService, c_service);
    (*jvm)->ReleaseStringUTFChars(jvm, aUsername, c_username);
    (*jvm)->ReleaseStringUTFChars(jvm, aPassword, c_password);
    
    return result;
}


JNIEXPORT jboolean JNICALL Java_com_cenqua_shaj_PAMAuthenticator_checkGroupMembershipImpl
  (JNIEnv *jvm, jclass jThisClass, jstring aUsername, jstring aGroup, jobject jLog)
{
    const char* c_username;
    const char* c_group;
    jboolean result;
    loginfo_t loginfo;
    
    loginfo = shaj_init_logger(jvm, jLog);

    c_username = (*jvm)->GetStringUTFChars(jvm, aUsername, NULL);
    c_group = (*jvm)->GetStringUTFChars(jvm, aGroup, NULL);

    result = shaj_memberOfGroup(c_username, c_group, &loginfo);
    
    (*jvm)->ReleaseStringUTFChars(jvm, aUsername, c_username);
    (*jvm)->ReleaseStringUTFChars(jvm, aGroup, c_group);
    
    return result;
}


JNIEXPORT jboolean JNICALL Java_com_cenqua_shaj_PAMAuthenticator_isSupportedImpl
  (JNIEnv * jvm, jclass jThisClass, jobject jLog)
{
  return JNI_TRUE;
}


JNIEXPORT jboolean JNICALL Java_com_cenqua_shaj_Win32Authenticator_isSupportedImpl
  (JNIEnv * jvm, jclass jThisClass, jobject jLog)
{
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_cenqua_shaj_Win32Authenticator_checkPasswordImpl
  (JNIEnv * jvm, jclass jThisClass, jstring aDomain, jstring aUserName, jstring aPassword, jobject aLog)
{
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_cenqua_shaj_Win32Authenticator_checkGroupMembershipImpl
  (JNIEnv * jvm, jclass jThisClass, jstring aDomain, jstring aUserName, jstring aPassword, jobject aLog)
{
  return JNI_FALSE;
}
