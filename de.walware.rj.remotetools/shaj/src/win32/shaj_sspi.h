/* Copyright 2005 Cenqua Pty Ltd. All Rights Reserved. See LICENSE.TXT in the distribution. */
#include <windows.h>
#include "../shaj_auth.h"

#ifndef _Included_shaj_sspi_h
#define _Included_shaj_sspi_h


BOOL WINAPI SSPLogonUser(LPTSTR szDomain, LPTSTR szUser, LPTSTR szPassword, loginfo_t* logger);
jboolean shaj_init(loginfo_t* logger);

#endif    
