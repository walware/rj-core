/* Copyright 2005 Cenqua Pty Ltd. All Rights Reserved. See LICENSE.TXT in the distribution. */
#include "shaj_group.h"
#include <grp.h>
#include <string.h>

jboolean shaj_memberOfGroup(const char* username, const char* groupname, loginfo_t* logger)
{
  struct group *grp;
  jboolean found = JNI_FALSE;

  shaj_log_debug(logger, "checking group for user=%s group=%s", username, groupname);

  setgrent();
  while ((grp = getgrent()) != NULL) {
    if (grp->gr_name == NULL) {
      continue;
    }
    
    if (0 == strcmp(groupname, grp->gr_name)) {
      // is username in this group?
      char** members;
      for (members = grp->gr_mem; *members; members++) {
        char* member = *members;
        if (0 == strcmp(username, member)) {
          found = JNI_TRUE;
        }
      }
    }
    if (found) {
      break;
    }
  }
  endgrent();
  
  return found;
}

