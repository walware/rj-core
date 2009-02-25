/* Copyright 2005 Cenqua Pty Ltd. All Rights Reserved. See LICENSE.TXT in the distribution. */
#ifndef _Included_config_h
#define _Included_config_h

#ifdef OS_IS_LINUX

#define USE_DLLOADED_PAM
#include <security/pam_appl.h>

#endif

#ifdef OS_IS_OSX

#include <pam/pam_appl.h>

#endif

#ifdef OS_IS_SOLARIS

#include <security/pam_appl.h>

#endif

#ifdef OS_IS_WINDOWS

#define vsnprintf _vsnprintf

#endif
    
#endif
