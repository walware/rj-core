/* Copyright 2005 Cenqua Pty Ltd. All Rights Reserved. See LICENSE.TXT in the distribution. */
/* This file is based on passwd-pam.c in xscreensaver which is:
 * written by Bill Nottingham <notting@redhat.com> (and jwz) for
 * xscreensaver, Copyright (c) 1993-2016 Jamie Zawinski <jwz@jwz.org>
 */
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include "../config.h"
#include "../shaj_auth.h"
#include "shaj_pam.h"

/*
 * if USE_DLLOADED_PAM is defined, then instead of calling the
 * PAM functions directly, we dlopen libpam.so manually and
 * call them indirectly.
 * We need to do this because when libpam.so is linked in, it
 * is not linked with RTLD_GLOBAL which means libpam.so stops
 * working.
 *
 * Otherwise we just map the mypam_* functions to pam_*
 */
#ifdef USE_DLLOADED_PAM
#include "mypam.c"
#else

#define mypam_start pam_start
#define mypam_end pam_end
#define mypam_authenticate pam_authenticate
#define mypam_setcred pam_setcred
#define mypam_strerror pam_strerror

#endif

jboolean shaj_init(loginfo_t* logger) {
  
#ifdef USE_DLLOADED_PAM
  return mypam__init(logger);
#else
  return JNI_TRUE;
#endif
  
}

static int ourpam_conversation(int nmsgs,
#ifndef OS_IS_SOLARIS
                               const 
#endif
                               struct pam_message **msg,
                               struct pam_response **resp,
                               void *authdata);

struct ourpam_authdata {
  const char *user;
  const char *password;
  loginfo_t* logger;
};


# define PAM_STRERROR(pamh, status) mypam_strerror((pamh), (status))


jboolean
shaj_chkpasswd_pam(const char* service, const char *user, const char *password, loginfo_t* logger){
  
  pam_handle_t *pamh = 0;
  int status = -1;
  struct pam_conv pc;
  struct ourpam_authdata c;
  
  c.user = user;
  c.password = password;
  c.logger = logger;
  
  pc.conv = &ourpam_conversation;
  pc.appdata_ptr = (void *) &c;
  
  
  status = mypam_start(service, c.user, &pc, &pamh);
  shaj_log_debug(logger, "pam_start (\"%s\", \"%s\", ...) ==> %d (%s)",
                    service, c.user,
                    status, PAM_STRERROR (pamh, status));
  
  if (status != PAM_SUCCESS) goto DONE;
  
  status = mypam_authenticate(pamh, 0);
  
  shaj_log_debug(logger, " pam_authenticate (...) ==> %d (%s)",
                    status, PAM_STRERROR(pamh, status));
  
  if (status == PAM_SUCCESS) {
    /* be nice to Kerberos and refresh credentials */
    int status2 = mypam_setcred(pamh, PAM_REINITIALIZE_CRED);
    shaj_log_debug(logger, " pam_setcred (...) ==> %d (%s)",
                      status2, PAM_STRERROR(pamh, status2));
    goto DONE;
  }

 DONE:
  if (pamh) {
    int status2 = mypam_end(pamh, status);
    pamh = 0;
    shaj_log_debug(logger, "pam_end (...) ==> %d (%s)",
                      status2,
                      (status2 == PAM_SUCCESS ? "Success" : "Failure"));
  }
  return (status == PAM_SUCCESS ? JNI_TRUE : JNI_FALSE);
}

static int
ourpam_conversation(int nmsgs,
#ifndef OS_IS_SOLARIS
                    const 
#endif
                    struct pam_message **msg,
                    struct pam_response **resp,
                    void *authdatap)
{
  /*
   * strings we pass to PAM in the reply objects are freed by
   * PAM as described here
   * http://www.opengroup.org/onlinepubs/8329799/chap5.htm
   */
  int replies = 0;
  struct pam_response *reply = 0;
  struct ourpam_authdata *authdata = (struct ourpam_authdata *) authdatap;
  loginfo_t* logger = authdata->logger;

  reply = (struct pam_response *) calloc(nmsgs, sizeof(*reply));
  if (!reply) return PAM_CONV_ERR;
	
  for (replies = 0; replies < nmsgs; replies++) {
    switch (msg[replies]->msg_style) {
    case PAM_PROMPT_ECHO_ON:
      reply[replies].resp_retcode = PAM_SUCCESS;
      reply[replies].resp = strdup(authdata->user);
      shaj_log_debug(logger, "  PAM ECHO_ON(\"%s\") ==> \"%s\"",
                        msg[replies]->msg,
                        reply[replies].resp);
      break;
    case PAM_PROMPT_ECHO_OFF:
      reply[replies].resp_retcode = PAM_SUCCESS;
      reply[replies].resp = strdup(authdata->password);
      shaj_log_debug(logger, "  PAM ECHO_OFF(\"%s\") ==> password",
                        /*msg[replies]->msg*/ "(masked)");
      break;
    case PAM_TEXT_INFO:
      /* ignore */
      reply[replies].resp_retcode = PAM_SUCCESS;
      reply[replies].resp = 0;
      shaj_log_debug(logger, "  PAM TEXT_INFO(\"%s\") ==> ignored",
                        msg[replies]->msg);
      break;
    case PAM_ERROR_MSG:
      /* ignore it */
      reply[replies].resp_retcode = PAM_SUCCESS;
      reply[replies].resp = 0;
      shaj_log_debug(logger, "  PAM ERROR_MSG(\"%s\") ==> ignored",
                        msg[replies]->msg);
      break;
    default:
      /* fail at this point */
      free (reply);
      shaj_log_error(logger, "  PAM unknown %d(\"%s\") ==> ignored",
                        msg[replies]->msg_style, msg[replies]->msg);
      return PAM_CONV_ERR;
    }
  }
  *resp = reply;
  return PAM_SUCCESS;
}


