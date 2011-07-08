#!/bin/sh
##
# Shell script to start R server (for StatET)
##
# Usually you have to change only the CONFIG sections. You should set at least:
#     R_HOME
#     JAVA_HOME
#     HOSTADDRESS
# 
# The authentication is set to 'fx' (method using SSH) by default.
##
# Usage of this script:
#     startup.sh <address or name> [options]
# It starts the R server asynchronously in background by default.
# Options:
#     -wd=<working directory>          initial R working directory
#     -debug                           enables debug output and
#                                      runs R in foreground
#
# Note: This script does not start an RMI registry! You have to launch it
# as system daemon or manually (see Java documentation), e.g. by:
#     $JAVA_HOME/bin/rmiregistry &
##
# Author: Stephan Wahlbrink
###############################################################################

###############################################################################
# SCRIPT - INIT / READING PARAMETERS ##########################################
###############################################################################
if [ -z "$1" ]
then
	echo Missing name for R server
	exit -1
fi
ADDRESS=$1
NAME=`basename $ADDRESS`
shift
WD=~
SCRIPT=`readlink -f "$0"`

until [ -z "$1" ]  # Until all parameters used up
do
	case "$1" in
	-wd=*)
		WD=${1##-wd=}
		;;
#	-host=*)
#		HOSTADDRESS=${1##-host=}
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
###############################################################################

###############################################################################
# CONFIG - SYSTEM SETTINGS ####################################################
###############################################################################
# Configure the startup in the following sections

###############################################################################
# Required system specific settings
# 
# Set the path to home of R and Java.
# If parts of the R installation (e.g. the documentation) are located in not
# sub-directories of R home, it is often required to set additional variables
# specifying the several installation locations (see example).
# 
# Example:
#     R_HOME=/usr/lib/R
#     R_DOC_DIR=/usr/share/doc/R
#     R_SHARE_DIR=/usr/share/R
#     R_INCLUDE_DIR=/usr/include/R
#     R_LIBS_SITE=/usr/local/lib/site-library
#     JAVA_HOME=/usr/lib/jvm/java-6-sun
R_HOME=
JAVA_HOME=

###############################################################################
# Set the home and work directory of this R server.
# The home must contain the jar files of this server, if this file is in this
# directory, you don't have to change the value.
# The working directory is for files like log files
##
# Example:
#     RJS_HOME=`dirname "$SCRIPT"`
#     RJS_WORK=~/.RJServer
RJS_HOME=`dirname "$SCRIPT"`
RJS_WORK=~/.RJServer

###############################################################################
# Set the hostname you want to use to access the server
# It is recommended to use the ip address instead of a name
##
# Usage:
#     HOSTADDRESS=<ip or hostname>
# Example:
#     HOSTADDRESS=192.168.1.80
HOSTADDRESS=

###############################################################################
# Add additional java options here
##
# Example:
#     JAVA_OPTS="-server -Drjava.path=<path-to-rjava>"
JAVA_OPTS="-server"
JAVA_OPTS_LIB=


###############################################################################
# CONFIG - AUTHENTICATION METHODS #############################################
###############################################################################
# You must specify the method (exactly one) to use for authentication when 
# clients wants to connect to the R.
# There are several methods available. The methods provided by default
# are described in the following subsections. To change the method uncomment
# the lines of method you want to use and comment the previous AUTH definition.
##
# General usage:
#     RJ parm:     -auth=<method-id>[:<config>]
#                  -auth=<classname>[:<config>]
# 
# <config> option depends on the selected method

###############################################################################
# Authentication method: disabled / 'none'
# 
# Disables authentication. Anybody can connect to R. Use it only if you are in 
# a secure environment! All users can connect to R get full user rights!
##
# General usage:
#     RJ param:    -auth=none
# 
# Script Usage:
#     AUTH=none

#AUTH=none

###############################################################################
# Authentication method: password file / 'name-pass'
# 
# Authentication using loginname-password combination.
##
# General usage:
#     RJ Param:    -auth=name-pass:file=<passwordfile>
# 
# Script usage:
#     AUTH=name-pass:file
#     AUTH_PW_FILE=~/.RJServer/logins
# 
# <passwordfile> is the path to a property file with
#     pairs of loginname and password, for example:
#         myname=mypassword
#     Make sure that only authorized users can read this file!

# AUTH=name-pass:file
# AUTH_PW_FILE=~/.RJServer/logins

###############################################################################
# Authentication method: local user account / 'local-shaj'
# 
# Authentication using your local loginname-password combination (PAM).
# The 'Shaj' library is required for this method. It is provided in sources and
# binary form for several platforms. If no binary match your environment, you
# have to build it for your system. You find the files inside the folder
#     shaj-native
# of this remotetools package.
##
# General Usage:
#     RJ Param:    -auth=local-shaj
#     Java Prop:   java.library.path=<folder of library>
# 
# Script Usage:
#     AUTH=local-shaj
#     AUTH_SHAJ_LD_DIR=<folder of library>
# Example:
#     AUTH=local-shaj
#     AUTH_SHAJ_LD_DIR="$RJS_HOME/shaj-native/linux-x64"

# AUTH=local-shaj
# AUTH_SHAJ_LD_DIR="$RJS_HOME/shaj-native/<platform>"

###############################################################################
# Authentication method: ssh / 'fx' (exchange over file)
# 
# Authentication for automatic startup (via ssh by StatET).
##
# General Usage:
#     RJ Param:    -auth=fx:file=<keyfile>
# 
# Script Usage:
#     AUTH=fx:file
#     AUTH_FX_FILE=<keyfile>
#     AUTH_FX_USER=$USER
#     AUTH_FX_MASK=600
# 
# <keyfile> a (empty). The fs permission to edit the files represents the
#     right to login into R, so:
#     The file must be writable for authorized user only!
#     If the script setup is used, the files is created automatically and
#     the permission is set according to AUTH_FX_USER and AUTH_FX_MASK

AUTH=fx:file
AUTH_FX_FILE="$RJS_WORK/session-$NAME.lock"
AUTH_FX_USER=$USER
AUTH_FX_MASK=600


###############################################################################
# SCRIPT - STARTUP SERVER #####################################################
###############################################################################
# Usually you don't have to edit the lines below

#$JAVA_HOME/bin/rmiregistry &

mkdir -p "$RJS_WORK"

## Finish auth configuration
if [ "$FORCE_AUTH" ]
then
	AUTH="$FORCE_AUTH"
fi

if [ "$AUTH" = "name-pass:file" ]
then
	AUTH="name-pass:file=$AUTH_PW_FILE"
fi

if [ "$AUTH" = "local-shaj" ]
then
	AUTH=local-shaj
	JAVA_OPTS_LIB="$JAVA_OPTS_LIB:$AUTH_SHAJ_LD_DIR"
	JAVA_CP="$JAVA_CP:$RJS_HOME/shaj.jar"
fi

if [ "$AUTH" = "fx:file" ]
then
	AUTH="fx:file=$AUTH_FX_FILE"
	AUTH_FX_FOLDER=`dirname "$AUTH_FX_FILE"`
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

## Java config
if [ $DEV ]
then
	JAVA_CP="$RJS_HOME/../de.walware.rj.server/bin:$RJS_HOME/../de.walware.rj.data/bin:$RJS_HOME/bin:$RJS_HOME/binShaj"
	RMI_BASE="file://$RJS_HOME/../de.walware.rj.server/bin"
	RJAVA_CP=
else
	JAVA_CP="$RJS_HOME/rj.server.jar:$RJS_HOME/rj.data.jar:$JAVA_CP"
	RMI_BASE="file://$RJS_HOME/rj.server.jar"
	RJAVA_CP=
fi

JAVA_OPTS="$JAVA_OPTS -Djava.rmi.server.hostname=$HOSTADDRESS -Djava.security.policy=$RJS_HOME/security.policy -Djava.rmi.server.codebase=$RMI_BASE"
if [ "$JAVA_OPTS_LIB" ]
then
	JAVA_OPTS="$JAVA_OPTS -Djava.library.path=$JAVA_OPTS_LIB"
fi
if [ "$RJAVA_CP" ]
then
	JAVA_OPTS="$JAVA_OPTS -Drjava.class.path=$RJAVA_CP"
fi

## Other environment
PATH=$R_HOME/bin:$PATH
LD_LIBRARY_PATH=$R_HOME/lib

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

START_EXEC="$JAVA_HOME/bin/java -cp $JAVA_CP $JAVA_OPTS de.walware.rj.server.RMIServerControl start $ADDRESS $OPTS"
#echo $START_EXEC

if [ $DEBUG ]
then
	echo HOSTADDRESS = $HOSTADDRESS
	echo PATH = $PATH
	echo LD_LIBRARY_PATH = $LD_LIBRARY_PATH
	echo R_HOME = $R_HOME
	echo JAVA_HOME = $JAVA_HOME
	echo CLASSPATH = $JAVA_CP
	echo JAVA_OPTIONS = $JAVA_OPTS
	echo AUTH = $AUTH
	
	# Start server directly
	$START_EXEC
	START_EXIT=$?
	START_PID=$!
	exit $START_EXIT
else
	# First check if running or dead server is already bound
	CLEAN_EXEC="$JAVA_HOME/bin/java -cp $JAVA_CP $JAVA_OPTS de.walware.rj.server.RMIServerControl clean $ADDRESS"
	$CLEAN_EXEC
	CLEAN_EXIT=$?
	if [ $CLEAN_EXIT -ne 0 ]
	then
		echo "Check and cleanup of old server failed (CODE=$CLEAN_EXIT), cancelling startup."
		exit $CLEAN_EXIT
	fi
	
	# Start server detached
	nohup $START_EXEC > "$RJS_WORK/session-$NAME.out" 2>&1 < /dev/null &
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
