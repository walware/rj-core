/* Copyright 2005 Cenqua Pty Ltd. All Rights Reserved. See LICENSE.TXT in the distribution. */
#include "shaj_netgroup.h"
#include <lm.h>
#include <string.h>
/*
 * inspired by the code at http://win32.mvps.org/security/fksec
 * (which is under the public domain)
 */

void log_net_debug(loginfo_t* logger, const char* msg, NET_API_STATUS rc) {
  char* msgcode = "unknown";
  if (rc == ERROR_ACCESS_DENIED) {
    msgcode = "ERROR_ACCESS_DENIED";
  } else if (rc == ERROR_MORE_DATA) {
    msgcode = "ERROR_MORE_DATA";
  } else if (rc == NERR_InvalidComputer) {
    msgcode = "NERR_InvalidComputer";
  } else if (rc == NERR_UserNotFound) {
    msgcode = "NERR_UserNotFound";
  }

  shaj_log_debug(logger, "%s, %d:%s", msg, (int)rc, msgcode);
}

BOOL shaj_memberOfGroup(LPCWSTR servername, LPCWSTR username, LPCWSTR groupname,
                           BOOL bIncludeGlobal, BOOL bIncludeLocal, BOOL bIncludeIndirect, loginfo_t* logger)
{
  DWORD cRead, cTotal ;
  NET_API_STATUS rc = NERR_Success ;
  BOOL result = FALSE;

  if (bIncludeGlobal) {
      GROUP_USERS_INFO_0 * buf = 0 ;
      rc = NetUserGetGroups(servername, username, 0, (BYTE **)&buf, MAX_PREFERRED_LENGTH, &cRead, &cTotal ) ;
      if (NERR_Success == rc) {
        
        LPCWSTR s;
        while (cRead--) {
          s = buf[cRead].grui0_name ;
          if (0 == wcscmp(groupname, s)) {
            result = TRUE;
            break;
          }
        }
        NetApiBufferFree(buf);
        buf = 0;
      }
      else {
        log_net_debug(logger, "problem calling NetUserGetGroups()", rc);
        return FALSE;
      }
  }

  if (!result && bIncludeLocal) {
    LOCALGROUP_USERS_INFO_0 * buf = 0 ;
    rc = NetUserGetLocalGroups(servername, username, 0, bIncludeIndirect ? LG_INCLUDE_INDIRECT : 0,
                               (BYTE **)&buf, MAX_PREFERRED_LENGTH, &cRead, &cTotal ) ;
    if (NERR_Success == rc) {
      LPCWSTR s ;
      while (cRead--) {
        s = buf[cRead].lgrui0_name ;
          if (0 == wcscmp(groupname, s)) {
            result = TRUE;
            break;
          }
      }
      NetApiBufferFree(buf) ;
      buf = 0 ;
    }
    else {
        log_net_debug(logger, "problem calling NetUserGetLocalGroups()", rc);
        return FALSE;
    }
  }
  return result;
}

//int wmain(int argc, wchar_t *argv[]){
//  shaj_memeberOfGroup(NULL, L"matt", L"Users", TRUE, TRUE, TRUE);
//
//  // 
//}
