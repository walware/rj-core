echo off
rem ##
rem # Windows batch script to start R server (for StatET)
rem ##
rem # Usually you have to change only the CONFIG sections. You should set at least:
rem #     R_HOME
rem #     JAVA_HOME
rem # Depending on the system configuration, it can be required to set:
rem #     S_HOSTADDRESS
rem # 
rem # The authentication is set to 'fx' (method using SSH) by default.
rem ##
rem # Usage of this script:
rem #     startup.cmd <address> [options]
rem #     startup.cmd <name> [options]
rem # It starts the R server asynchronously in background by default.
rem #     <address>                        the complete RMI-address
rem #     <name>                           the session name only (the hostname
rem #                                      should be set in this file)
rem # Options:
rem #     -wd=<working directory>          initial R working directory
rem #     -debug                           enables debug output and
rem #                                      runs R in foreground
rem #
rem # Note: This script does not start an RMI registry! You have to launch it
rem # as system daemon or manually (see Java documentation), e.g. by:
rem #     $JAVA_HOME/bin/rmiregistry.exe
rem ##
rem # Author: Stephan Wahlbrink
rem ###############################################################################

rem ###############################################################################
rem # SCRIPT - INIT / READING PARAMETERS ##########################################
rem ###############################################################################
if [ -z "$1" ]
then
	echo Missing address or name for R server
	exit -1
fi
set ADDRESS=$1
set S_NAME=`basename $ADDRESS`
set S_HOSTADDRESS=
set S_REGISTRYPORT=
if [ "$ADDRESS" != "$S_NAME" ]
then
	set S_HOSTADDRESS=`expr "$ADDRESS" : "[^/]*\/\/\([^:/]*\).*"`
	set S_REGISTRYPORT=`expr "$ADDRESS" : "[^/]*\/\/[^:/]*:\([0-9]\+\)\/.*"`
fi
shift
set WD=~
set SCRIPT=`readlink -f "$0"`

until [ -z "$1" ]  # Until all parameters used up
do
	case "$1" in
	-wd=*)
		WD=${1##-wd=}
		;;
#	-host=*)
#		S_HOSTADDRESS=${1##-host=}
#		;;
	-debug*)
		DEBUG=1
		echo Debug mode enabled
		;;
	-dev*)
		DEV=1
		echo Development mode enabled
		;;
	*)
		echo Unknown parameter: $1
		;;
	esac
	shift
done
rem ###############################################################################

rem ###############################################################################
rem # CONFIG - SYSTEM SETTINGS ####################################################
rem ###############################################################################
rem # Configure the startup in the following sections

rem ###############################################################################
rem # Required system specific settings
rem # 
rem # Set the path to home of R and Java.
rem # If parts of the R installation (e.g. the documentation) are located in not
rem # sub-directories of R home, it is often required to set additional variables
rem # specifying the several installation locations (see example).
rem # 
rem # Example:
rem #     R_HOME=/usr/lib/R
rem #     R_DOC_DIR=/usr/share/doc/R
rem #     R_SHARE_DIR=/usr/share/R
rem #     R_INCLUDE_DIR=/usr/include/R
rem #     R_LIBS_SITE=/usr/local/lib/R/site-library
rem #     JAVA_HOME=/usr/lib/jvm/java-6-sun

set R_HOME=
set JAVA_HOME=

rem #rem ##############################################################################
rem # Set the home and work directory of this R server.
rem # The home must contain the jar files of this server, if this file is in this
rem # directory, you don't have to change the value.
rem # The working directory is for files like log files
rem ##
rem # Example:
rem #     RJS_HOME=`dirname "$SCRIPT"`
rem #     RJS_WORK=~/.RJServer

set RJS_HOME=`dirname "$SCRIPT"`
set RJS_WORK=~/.RJServer

rem ###############################################################################
rem # Set explicitly the hostname and port you want to use to access the RMI 
rem # registry/R server.
rem # It is recommended to use the IP address instead of a name.
rem # By default, the hostname is extracted form the specified RMI address 
rem # (script parameter). Nevertheless there are some reasons to set it explicitly
rem # here, e.g.:
rem #  - For SSH tunnel connections '127.0.0.1' (localhost) is sufficient;
rem #    recommend, if no other network connections are used; and required if the 
rem #    public IP address is blocked by the firewall.
rem #  - To make sure that the correct IP address is set even the hostname is 
rem #    used in the RMI address.
rem ##
rem # Usage:
rem #     S_HOSTADDRESS=<ip or hostname>
rem #     S_REGISTRYPORT=<port of rmi-registry>
rem # Example:
rem #     S_HOSTADDRESS=192.168.1.80

rem set S_HOSTADDRESS=


rem #rem ##############################################################################
# Add additional java options here
rem ##
rem # Example:
rem #     JAVA_OPTS="-server -Dde.walware.rj.rmi.disableSocketFactory=true"

set JAVA_OPTS="-server"
set JAVA_OPTS_LIB=


rem ###############################################################################
rem # CONFIG - AUTHENTICATION METHODS #############################################
rem ###############################################################################
rem # You must specify the method (exactly one) to use for authentication when 
rem # clients wants to connect to the R.
rem # There are several methods available. The methods provided by default
rem # are described in the following subsections. To change the method uncomment
rem # the lines of method you want to use and comment the previous AUTH definition.
rem ##
rem # General usage:
rem #     RJ parm:     -auth=<method-id>[:<config>]
rem #                  -auth=<classname>[:<config>]
rem # 
rem # <config> option depends on the selected method

rem ###############################################################################
rem # Authentication method: disabled / 'none'
rem # 
rem # Disables authentication. Anybody can connect to R. Use it only if you are in 
rem # a secure environment! All users can connect to R get full user rights!
rem ##
rem # General usage:
rem #     RJ param:    -auth=none
rem # 
rem # Script Usage:
rem #     AUTH=none

rem set AUTH=none

rem ###############################################################################
rem # Authentication method: password file / 'name-pass'
rem # 
rem # Authentication using loginname-password combination.
rem ##
rem # General usage:
rem #     RJ Param:    -auth=name-pass:file=<passwordfile>
rem # 
rem # Script usage:
rem #     AUTH=name-pass:file
rem #     AUTH_PW_FILE=~/.RJServer/logins
rem # 
rem # <passwordfile> is the path to a property file with
rem #     pairs of loginname and password, for example:
rem #         myname=mypassword
rem #     Make sure that only authorized users can read this file!

rem set AUTH=name-pass:file
rem set AUTH_PW_FILE=~/.RJServer/logins

rem ###############################################################################
rem # Authentication method: local user account / 'local-shaj'
rem # 
rem # Authentication using your local loginname-password combination (PAM).
rem # The 'Shaj' library is required for this method. It is provided in sources and
rem # binary form for several platforms. If no binary match your environment, you
rem # have to build it for your system. You find the files inside the folder
rem #     shaj
rem # of this remotetools package.
rem ##
rem # General Usage:
rem #     RJ Param:    -auth=local-shaj
rem #     Java Prop:   java.library.path=<folder of library>
rem # 
rem # Script Usage:
rem #     AUTH=local-shaj
rem #     AUTH_SHAJ_LD_DIR=<folder of library>
rem # Example:
rem #     AUTH=local-shaj
rem #     AUTH_SHAJ_LD_DIR="$RJS_HOME/shaj/linux-x64"

rem set AUTH=local-shaj
rem set AUTH_SHAJ_LD_DIR="$RJS_HOME/shaj/<platform>"

rem ###############################################################################
rem # Authentication method: ssh / 'fx' (exchange over file)
rem # 
rem # Authentication for automatic startup (via ssh by StatET).
rem ##
rem # General Usage:
rem #     RJ Param:    -auth=fx:file=<keyfile>
rem # 
rem # Script Usage:
rem #     AUTH=fx:file
rem #     AUTH_FX_FILE=<keyfile>
rem #     AUTH_FX_USER=$USER
rem #     AUTH_FX_MASK=600
rem # 
rem # <keyfile> a (empty). The fs permission to edit the files represents the
rem #     right to login into R, so:
rem #     The file must be writable for authorized user only!
rem #     If the script setup is used, the files is created automatically and
rem #     the permission is set according to AUTH_FX_USER and AUTH_FX_MASK

set AUTH=fx:file
set AUTH_FX_FILE="$RJS_WORK/session-$S_NAME.lock"
set AUTH_FX_USER=$USER
set AUTH_FX_MASK=600


rem ###############################################################################
rem # SCRIPT - STARTUP SERVER #####################################################
rem ###############################################################################
rem # Usually you don't have to edit the lines below

rem set $JAVA_HOME/bin/rmiregistry &

mkdir -p "$RJS_WORK"

rem ## Final RMI address
if [ -n "$S_HOSTADDRESS" ]
then
	set S_ADDRESS="//$S_HOSTADDRESS"
	if [ -n "$S_REGISTRYPORT" ]
	then
		set S_ADDRESS="$S_ADDRESS:$S_REGISTRYPORT"
	fi
	set S_ADDRESS="$S_ADDRESS/$S_NAME"
else
	set S_ADDRESS="//$S_NAME"
fi

rem ## Finish auth configuration
if [ "$FORCE_AUTH" ]
then
	set AUTH="$FORCE_AUTH"
fi

if [ "$AUTH" = "name-pass:file" ]
then
	set AUTH="name-pass:file=$AUTH_PW_FILE"
fi

if [ "$AUTH" = "local-shaj" ]
then
	set AUTH=local-shaj
	set JAVA_OPTS_LIB="$JAVA_OPTS_LIB:$AUTH_SHAJ_LD_DIR"
	set JAVA_CP="$JAVA_CP:$RJS_HOME/shaj/auth.jar"
fi

if [ "$AUTH" = "fx:file" ]
then
	set AUTH="fx:file=$AUTH_FX_FILE"
	set AUTH_FX_FOLDER=`dirname "$AUTH_FX_FILE"`
	mkdir -p "$AUTH_FX_FOLDER"
	echo "00000000" > "$AUTH_FX_FILE"
	if [ $AUTH_FX_USER ]
	then
		chown $AUTH_FX_USER "$AUTH_FX_FILE"
	fi
	if [ $AUTH_FX_MASK ]
	then
		chmod $AUTH_FX_MASK "$AUTH_FX_FILE"
	fi
fi

OPTS="-auth=$AUTH"
if [ $DEBUG ]
then
	OPTS="$OPTS -verbose"
fi

rem ## Java config
if [ $DEV ]
then
	set JAVA_CP="$RJS_HOME/../de.walware.rj.server/bin:$RJS_HOME/../de.walware.rj.data/bin:$RJS_HOME/bin:$RJS_HOME/binShaj"
	set RMI_BASE="file://$RJS_HOME/../de.walware.rj.server/bin"
	set RJAVA_CP=
else
	set JAVA_CP="$RJS_HOME/de.walware.rj.server.jar:$RJS_HOME/de.walware.rj.data.jar:$JAVA_CP"
	set RMI_BASE="file://$RJS_HOME/de.walware.rj.server.jar"
	set RJAVA_CP=
fi

set JAVA_OPTS="$JAVA_OPTS -Djava.security.policy=$RJS_HOME/security.policy -Djava.rmi.server.codebase=$RMI_BASE"
if [ -n "$S_HOSTADDRESS" ]
then
	set JAVA_OPTS="$JAVA_OPTS -Djava.rmi.server.hostname=$S_HOSTADDRESS"
fi
if [ -n "$JAVA_OPTS_LIB" ]
then
	set JAVA_OPTS="$JAVA_OPTS -Djava.library.path=$JAVA_OPTS_LIB"
fi
if [ -n "$RJAVA_CP" ]
then
	set JAVA_OPTS="$JAVA_OPTS -Drjava.class.path=$RJAVA_CP"
fi

rem ## Other environment
set PATH=$R_HOME/bin:$PATH
set LD_LIBRARY_PATH=$R_HOME/lib

export PATH
export LD_LIBRARY_PATH
export JAVA_HOME
export R_HOME
export R_LIBS
export R_LIBS_USER
export R_LIBS_SITE
export R_DOC_DIR
export R_SHARE_DIR
export R_INCLUDE_DIR
export LC_ALL

cd "$WD"

set START_EXEC="$JAVA_HOME/bin/java -cp $JAVA_CP $JAVA_OPTS de.walware.rj.server.RMIServerControl start $S_ADDRESS $OPTS"
rem echo $START_EXEC

if [ $DEBUG ]
then
	echo S_HOSTADDRESS = $S_HOSTADDRESS
	echo S_REGISTRYPORT = $S_REGISTRYPORT
	echo PATH = $PATH
	echo LD_LIBRARY_PATH = $LD_LIBRARY_PATH
	echo R_HOME = $R_HOME
	echo JAVA_HOME = $JAVA_HOME
	echo CLASSPATH = $JAVA_CP
	echo JAVA_OPTIONS = $JAVA_OPTS
	echo AUTH = $AUTH
	
	rem # Start server directly
	$START_EXEC
	START_EXIT=$?
	START_PID=$!
	exit $START_EXIT
else
	rem # First check if running or dead server is already bound
	CLEAN_EXEC="$JAVA_HOME/bin/java -cp $JAVA_CP $JAVA_OPTS de.walware.rj.server.RMIServerControl clean $S_ADDRESS"
	$CLEAN_EXEC
	CLEAN_EXIT=$?
	if [ $CLEAN_EXIT -ne 0 ]
	then
		echo "Check and cleanup of old server failed (CODE=$CLEAN_EXIT), cancelling startup."
		exit $CLEAN_EXIT
	fi
	
	rem # Start server detached
	nohup $START_EXEC > "$RJS_WORK/session-$S_NAME.out" 2>&1 < /dev/null &
	START_EXIT=$?
	START_PID=$!
	if [ $START_EXIT -eq 0 ]
	then
		echo "Started server in background (PID=$START_PID)."
		exit 0
	else
		echo "Startup failed"
		exit $START_EXIT
	fi
fi
