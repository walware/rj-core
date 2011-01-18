AC_INIT(rj, 0.5, stephan.wahlbrink@walware.de)


AC_MSG_CHECKING(R config)
: ${R_HOME=`R RHOME`}
if test -z "${R_HOME}"; then
  AC_MSG_ERROR(cannot determine R_HOME)
fi
AC_MSG_RESULT([found
    R_HOME   : ${R_HOME}])
AC_SUBST(R_HOME)
export R_HOME
RBIN="${R_HOME}/bin/R"


CC=`"${RBIN}" CMD config CC`;
CFLAGS=`"${RBIN}" CMD config CFLAGS`

AC_PROG_CC


AC_MSG_CHECKING(Java config of R)
: ${JAVA_HOME=`"${RBIN}" CMD config JAVA_HOME|sed 's/ERROR:.*//'`}
: ${JAVA=`"${RBIN}" CMD config JAVA|sed 's/ERROR:.*//'`}
: ${JAVAC=`"${RBIN}" CMD config JAVAC|sed 's/ERROR:.*//'`}
: ${JAVAH=`"${RBIN}" CMD config JAVAH|sed 's/ERROR:.*//'`}
: ${JAVA_CPPFLAGS=`"${RBIN}" CMD config JAVA_CPPFLAGS|sed 's/ERROR:.*//'`}
: ${JAVA_LIBS=`"${RBIN}" CMD config JAVA_LIBS|sed 's/ERROR:.*//'`}
: ${JAR=`"${RBIN}" CMD config JAR|sed 's/ERROR:.*//'`}

if test -z "${JAVA_HOME}"; then
  AC_MSG_ERROR([cannot determine JAVA_HOME
R is not configured with Java support. Please run
    R CMD javareconf
as root to add Java support to R.

If you don't have root privileges, run
    R CMD javareconf -e
to set all Java-related variables and then install the package.])
fi

if test -z "${JAVA_HOME}" \
    || test -z "${JAVA}" \
    || test -z "${JAVAC}" \
    || test -z "${JAVAH}" \
    || test -z "${JAVA_CPPFLAGS}" \
    || test -z "${JAVA_LIBS}" \
    || test -z "${JAR}"; then
  AC_MSG_ERROR([cannot determine complete java config
    JAVA_HOME: ${JAVA_HOME}
    JAVA     : ${JAVA}
    JAVAC    : ${JAVAC}
    JAVAH    : ${JAVAH}
    JAVA_CPPFLAGS: ${JAVA_CPPFLAGS}
    JAVA_LIBS: ${JAVA_LIBS}
    JAR      : ${JAR}
R is not configured with full Java support. Please make sure 
an JDK is installed and run
    R CMD javareconf
as root to add Java support to R.

If you don't have root privileges, run
    R CMD javareconf -e
to set all Java-related variables and then install the package.])
fi

AC_MSG_RESULT([ok
    JAVA_HOME: ${JAVA_HOME}
    JAVA     : ${JAVA}
    JAVAC    : ${JAVAC}
    JAVAH    : ${JAVAH}
    JAVA_CPPFLAGS: ${JAVA_CPPFLAGS}
    JAVA_LIBS: ${JAVA_LIBS}
    JAR      : ${JAR}
])
AC_SUBST(JAVA_HOME)
AC_SUBST(JAVA)
AC_SUBST(JAVAC)
AC_SUBST(JAVAH)
AC_SUBST(JAVA_CPPFLAGS)
AC_SUBST(JAVA_LIBS)
AC_SUBST(JAR)
export JAVA_HOME JAVA JAVAC JAVAH JAVA_CPPFLAGS JAVA_LIBS JAVA_LD_LIBRARY_PATH JAR


# Configure and build JRI
CONFIGURED=1
export CONFIGURED
AC_CONFIG_SUBDIRS(jri)

AC_CONFIG_FILES([src/Makevars])
AC_OUTPUT
