# Process this file with autoconf to produce a configure script.
AC_INIT(JavaGD, 0.5, Simon.Urbanek@math.uni-augsburg.de)
AC_CONFIG_SRCDIR([src/javaGD.c])
AC_CONFIG_HEADER([src/config.h])

# find R home and set CC/CFLAGS
: ${R_HOME=`R RHOME`}
if test -z "${R_HOME}"; then
  echo "could not determine R_HOME"
  exit 1
fi
RBIN="${R_HOME}/bin/R"
CC=`${RBIN} CMD config CC`;
CPP=`${RBIN} CMD config CPP`;
CFLAGS=`${RBIN} CMD config CFLAGS`
AC_SUBST(R_HOME)

# Checks for programs.
AC_PROG_CC

# Checks for libraries.

# Checks for header files.
AC_HEADER_STDC
AC_HEADER_SYS_WAIT
AC_CHECK_HEADERS([string.h sys/time.h unistd.h])

# Checks for typedefs, structures, and compiler characteristics.
AC_C_CONST
AC_HEADER_TIME

# Checks for library functions.
AC_FUNC_MALLOC
AC_TYPE_SIGNAL
AC_CHECK_FUNCS([memset mkdir rmdir select socket])

AC_MSG_CHECKING([Java support in R])
R_JAVA_HOME=`"${RBIN}" CMD config JAVA_HOME`
: ${JAVA_HOME="${R_JAVA_HOME}"}
if test -z "${JAVA_HOME}"; then
   AC_MSG_ERROR([absent
R was configured without Java support. Please run
R CMD javareconf
as root to add Java support to R.

If you don't have root privileges, run
R CMD javareconf -e
to set all Java-related variables and then install rJava.
(works with R 2.5.0 only)])
fi

#: ${JAR=`"${RBIN}" CMD config JAR|sed 's/ERROR:.*//'`}
: ${JAVA=`"${RBIN}" CMD config JAVA|sed 's/ERROR:.*//'`}
#: ${JAVAC=`"${RBIN}" CMD config JAVAC|sed 's/ERROR:.*//'`}
#: ${JAVAH=`"${RBIN}" CMD config JAVAH|sed 's/ERROR:.*//'`}
: ${JAVA_CPPFLAGS=`"${RBIN}" CMD config JAVA_CPPFLAGS|sed 's/ERROR:.*//'`}
: ${JAVA_LIBS=`"${RBIN}" CMD config JAVA_LIBS|sed 's/ERROR:.*//'`}

AC_MSG_RESULT([present:
interpreter : '${JAVA}'
cpp flags   : '${JAVA_CPPFLAGS}'
java libs   : '${JAVA_LIBS}'])

java_error='One or more Java configuration variables are not set.'
if test -z "${JAVA}"; then
   java_error='Java interpreter is missing or not registered in R'
fi

have_all_flags=no
if test -n "${JAVA}" && test -n "${JAVA_CPPFLAGS}" && test -n "${JAVA_LIBS}"; then
  have_all_flags=yes;
fi
if test "${have_all_flags}" = no; then
  AC_MSG_ERROR([${java_error}
Make sure R is configured with full Java support (including JDK). Run
R CMD javareconf
as root to add Java support to R.

If you don't have root privileges, run
R CMD javareconf -e
to set all Java-related variables and then install rJava.
])
fi

JAVA_INC="${JAVA_CPPFLAGS}"

if test `echo foo | sed -e 's:foo:bar:'` = bar; then
   JAVA_CPPFLAGS0=`echo ${JAVA_CPPFLAGS} | sed -e 's:$(JAVA_HOME):${JAVA_HOME}:g'`
   JAVA_LIBS0=`echo ${JAVA_LIBS} | sed -e 's:$(JAVA_HOME):${JAVA_HOME}:g'`
   JAVA_LD_LIBRARY_PATH0=`echo ${JAVA_LD_LIBRARY_PATH} | sed -e 's:$(JAVA_HOME):${JAVA_HOME}:g'`
else
   AC_MSG_WARN([sed is not working properly - the configuration may fail])
   JAVA_CPPFLAGS0="${JAVA_CPPFLAGS}"
   JAVA_LIBS0="${JAVA_LIBS}"
   JAVA_LD_LIBRARY_PATH0="${JAVA_LD_LIBRARY_PATH}"
fi

LIBS="${LIBS} ${JAVA_LIBS0}"
CFLAGS="${CFLAGS} ${XTRA_CF} ${JAVA_CPPFLAGS0}"
LD_LIBRARY_PATH="${LD_LIBRARY_PATH}:${JAVA_LD_LIBRARY_PATH0}"

AC_MSG_CHECKING([whether JNI programs can be compiled])
AC_LINK_IFELSE([
#include <jni.h>
int main(void) {
    jobject o;
    return 0;
}
	],[AC_MSG_RESULT(yes)],
	[AC_MSG_ERROR([Cannot compile a simple JNI program. See config.log for details.])])

AC_SUBST(JAVA_LIBS)
AC_SUBST(JAVA_INC)
AC_SUBST(JAVA_HOME)
AC_SUBST(JAVA_LD)
AC_SUBST(XTRA_CF)
AC_SUBST(XTRA_LD)
AC_SUBST(JAVA)
AC_SUBST(JAVAC)
AC_SUBST(JAVAH)

AC_CONFIG_FILES([src/Makevars])
AC_OUTPUT
