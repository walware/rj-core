/* Copyright 2005 Cenqua Pty Ltd. All Rights Reserved. See LICENSE.TXT in the distribution. */
#include <jni.h>
#include <stdio.h>
#include <windows.h>
#include "../com_cenqua_shaj_Shaj.h"
#include "../com_cenqua_shaj_PAMAuthenticator.h"
#include "../com_cenqua_shaj_Win32Authenticator.h"
#include "../shaj_auth.h"
#include "shaj_sspi.h"
#include "shaj_netgroup.h"

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
    return JNI_FALSE;
}

JNIEXPORT jboolean JNICALL Java_com_cenqua_shaj_PAMAuthenticator_checkGroupMembershipImpl
  (JNIEnv *jvm, jclass jThisClass, jstring aUsername, jstring aGroup, jobject jLog)
{
  return JNI_FALSE;
}


JNIEXPORT jboolean JNICALL Java_com_cenqua_shaj_PAMAuthenticator_isSupportedImpl
  (JNIEnv * jvm, jclass jThisClass, jobject jLog)
{
  return JNI_FALSE;
}


JNIEXPORT jboolean JNICALL Java_com_cenqua_shaj_Win32Authenticator_isSupportedImpl
  (JNIEnv * jvm, jclass jThisClass, jobject jLog)
{
    return JNI_TRUE;
}


JNIEXPORT jboolean JNICALL Java_com_cenqua_shaj_Win32Authenticator_checkPasswordImpl
  (JNIEnv * jvm, jclass jThisClass, jstring aDomain, jstring aUsername, jstring aPassword, jobject jLog)
{
    const char* c_domain;
    const char* c_username;
    const char* c_password;
    loginfo_t loginfo;
    jboolean result;

    HANDLE token = NULL;
    BOOL status = 1;

    loginfo = shaj_init_logger(jvm, jLog);

    c_domain = (aDomain == NULL) ? NULL : (*jvm)->GetStringUTFChars(jvm, aDomain, NULL);
    c_username = (*jvm)->GetStringUTFChars(jvm, aUsername, NULL);
    c_password = (*jvm)->GetStringUTFChars(jvm, aPassword, NULL);

    status = SSPLogonUser((char*)c_domain, (char*)c_username, (char*)c_password, &loginfo);
    result =  status ? JNI_TRUE : JNI_FALSE;

    shaj_log_debug(&loginfo, "checking password for domain=%s user=%s => %d",
                      (c_domain==NULL)? "(NULL)" : c_domain, c_username, (int) result);
    
    if (aDomain != NULL) {
        (*jvm)->ReleaseStringUTFChars(jvm, aDomain, c_domain);
    }
    
    (*jvm)->ReleaseStringUTFChars(jvm, aUsername, c_username);
    (*jvm)->ReleaseStringUTFChars(jvm, aPassword, c_password);
    
    return result;
}

JNIEXPORT jboolean JNICALL Java_com_cenqua_shaj_Win32Authenticator_checkGroupMembershipImpl
  (JNIEnv * jvm, jclass jThisClass, jstring aDomain, jstring aUsername, jstring aGroup, jobject jLog)
{
    const jchar* wc_domain;
    const jchar* wc_username;
    const jchar* wc_group;
    loginfo_t loginfo;
    jboolean result;

    HANDLE token = NULL;
    BOOL status = 1;

    loginfo = shaj_init_logger(jvm, jLog);

    wc_domain = (aDomain == NULL) ? NULL : (*jvm)->GetStringChars(jvm, aDomain, NULL);
    wc_username = (*jvm)->GetStringChars(jvm, aUsername, NULL);
    wc_group = (*jvm)->GetStringChars(jvm, aGroup, NULL);

    status = shaj_memberOfGroup(wc_domain, wc_username, wc_group, TRUE, TRUE, TRUE, &loginfo);
    result =  status ? JNI_TRUE : JNI_FALSE;

    shaj_log_debug(&loginfo, "checking group for domain=%ws user=%ws group=%ws => %d",
                      (wc_domain==NULL)? L"(NULL)" : wc_domain, wc_username, wc_group, (int) result);
    
    if (aDomain != NULL) {
        (*jvm)->ReleaseStringChars(jvm, aDomain, wc_domain);
    }
    
    (*jvm)->ReleaseStringChars(jvm, aUsername, wc_username);
    (*jvm)->ReleaseStringChars(jvm, aGroup, wc_group);
    
    return result;
}

//TODO use this for groups check http://msdn.microsoft.com/library/default.asp?url=/library/en-us/secauthz/security/checktokenmembership.asp
// http://win32.mvps.org/security/fksec/doc/class_fksec__token.html#_details
// http://sources.redhat.com/cgi-bin/cvsweb.cgi/src/winsup/utils/mkgroup.c?rev=1.25&content-type=text/x-cvsweb-markup&cvsroot=src

// http://msdn.microsoft.com/library/en-us/netmgmt/netmgmt/netusergetgroups.asp?frame=true
// http://win32.mvps.org/security/fksec/doc/class_fksec__sid.html#a29

// unicode example see http://msdn.microsoft.com/library/default.asp?url=/library/en-us/netmgmt/netmgmt/looking_up_a_users_full_name.asp
//      http://msdn.microsoft.com/library/default.asp?url=/library/en-us/intl/unicode_1cmr.asp
