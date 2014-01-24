/* Copyright 2005 Cenqua Pty Ltd. All Rights Reserved. See LICENSE.TXT in the distribution. */
#ifndef _Included_shaj_pam_h
#define _Included_shaj_pam_h

jboolean shaj_chkpasswd_pam(const char* service, const char *user, const char *passwd, loginfo_t* logger);
jboolean shaj_init(struct loginfo* logger);

#endif
