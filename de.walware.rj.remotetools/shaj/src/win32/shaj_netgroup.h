/* Copyright 2005 Cenqua Pty Ltd. All Rights Reserved. See LICENSE.TXT in the distribution. */
#include <windows.h>
#include "../shaj_auth.h"

#ifndef _Included_shaj_netgroup_h
#define _Included_shaj_netgroup_h
BOOL shaj_memberOfGroup(LPCWSTR servername, LPCWSTR username, LPCWSTR groupname,
                           BOOL bIncludeGlobal, BOOL bIncludeLocal, BOOL bIncludeIndirect, loginfo_t* logger);


#endif    
