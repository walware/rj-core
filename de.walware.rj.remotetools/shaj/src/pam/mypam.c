/* Copyright 2005 Cenqua Pty Ltd. All Rights Reserved. See LICENSE.TXT in the distribution. */
#include <dlfcn.h>

static void *libpam = NULL;

typedef int FUNC_pam_start (const char*,const char*,const struct pam_conv*,pam_handle_t**);
typedef int FUNC_pam_end (pam_handle_t*,int);
typedef int FUNC_pam_authenticate (pam_handle_t*,int);
typedef int FUNC_pam_setcred (pam_handle_t*,int);
typedef char* FUNC_pam_strerror (pam_handle_t*,int);

static FUNC_pam_start* real_pam_start;
static FUNC_pam_end* real_pam_end;
static FUNC_pam_authenticate* real_pam_authenticate;
static FUNC_pam_setcred* real_pam_setcred;
static FUNC_pam_strerror* real_pam_strerror;

static void* mydlsym(void* libhandle, const char* fname, loginfo_t* logger) {
    char* error;
    void* fn;

    fn = dlsym(libhandle, fname);
    error = dlerror();
    if (error != NULL) {
        shaj_log_error(logger, "dlsym could not find %s: %s\n", fname, error);
        return NULL;
    }
    return fn;
}

/* return 1 on success */
static jboolean mypam__init(loginfo_t* logger) {

    if (logger->isdebug) {
        shaj_log_debug(logger, "attempting to load libpam.so");
    }
    
    libpam = dlopen ("libpam.so", RTLD_NOW | RTLD_GLOBAL);
    if (libpam == NULL) {
        shaj_log_error(logger, "Could not dlopen libpam.so with RTLD_NOW|RTLD_GLOBAL: %s\n", dlerror());;
        return JNI_FALSE;
    }

    
    real_pam_start = (FUNC_pam_start*) mydlsym(libpam, "pam_start", logger);
    if (real_pam_start == NULL) {
        return JNI_FALSE;
    }

    real_pam_end = (FUNC_pam_end*) mydlsym(libpam, "pam_end", logger);
    if (real_pam_end == NULL) {
        return JNI_FALSE;
    }

    real_pam_authenticate =  (FUNC_pam_authenticate*) mydlsym(libpam, "pam_authenticate", logger);
    if (real_pam_authenticate == NULL) {
        return JNI_FALSE;
    }
    
    real_pam_strerror =  (FUNC_pam_strerror*) mydlsym(libpam, "pam_strerror", logger);
    if (real_pam_strerror == NULL) {
        return JNI_FALSE;
    }
    
    real_pam_setcred = (FUNC_pam_setcred*) mydlsym(libpam, "pam_setcred", logger);
    if (real_pam_setcred == NULL) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

static int mypam_start (
    const char               *service,
    const char                *user,
    const struct pam_conv    *pam_conv,
    pam_handle_t            **pamh
    )
{
    return (*real_pam_start)(service, user, pam_conv, pamh);
}

static int mypam_end (
    pam_handle_t    *pamh,
    int              status
)
{
    return (*real_pam_end)(pamh, status);
}

static int mypam_authenticate (
    pam_handle_t    *pamh,
    int              flags
)
{
    return (*real_pam_authenticate)(pamh, flags);
}

static const char *mypam_strerror (
    pam_handle_t    *pamh,
    int              error_number
    )
{
    return (*real_pam_strerror)(pamh, error_number);
}

static int mypam_setcred (
    pam_handle_t    *pamh,
    int              flags
    )
{
    return (*real_pam_setcred)(pamh, flags);
}
    
