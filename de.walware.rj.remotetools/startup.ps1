## (Windows PowerShell Script)
# Shell script to start R server (for StatET)
##
# Usually you have to change only the CONFIG sections. You should set at least:
#     R_HOME
#     JAVA_HOME
# Depending on the system configuration, it can be required to set:
#     S_HOSTADDRESS
# 
# The authentication is set to 'none' by default.
##
# Usage of this script:
#     startup.sh <address> [options]
#     startup.sh <name> [options]
# It starts the R server asynchronously in background by default.
#     <address>                        the complete RMI-address
#     <name>                           the session name only (the hostname
#                                      should be set in this file)
# Options:
#     -wd=<working directory>          initial R working directory
#     -debug                           enables debug output and
#                                      runs R in foreground
#
# Note: This script does not start an RMI registry! You have to launch it
# as system daemon or manually (see Java documentation), e.g. by:
#     %JAVA_HOME%/bin/rmiregistry.exe
##
# Author: Stephan Wahlbrink
###############################################################################

###############################################################################
# SCRIPT - INIT / READING PARAMETERS ##########################################
###############################################################################
param([String] $ADDRESS)

if ([String]::IsNullOrEmpty("$ADDRESS"))
{
	echo "Missing address or name for R server"
	exit -1
}

$S_NAME=$ADDRESS -replace '.*\/([^/]+)$', '$1'
$S_HOSTADDRESS=""
$S_REGISTRYPORT=""
if ( $ADDRESS -ne $S_NAME )
{
	$S_HOSTADDRESS=$ADDRESS -replace '[^/]*\/\/([^:/]*).*', '$1'
	$S_REGISTRYPORT=$ADDRESS -replace '[^/]*\/\/[^:/]*:?([0-9]*)\/.*', '$1'
}

$WD=$HOME
$SCRIPT=$MyInvocation.MyCommand.Path

foreach ($1 in $Args) {
	switch -wildcard ("$1") {
	"-wd=*" {
		$WD=$1.Substring(4)
		}
#	"-host=*" {
#		$S_HOSTADDRESS=$1.Substring(6)
#		}
	"-debug*" {
		$DEBUG=1
		echo "Debug mode enabled"
		}
	"-dev*" {
		$DEV=1
		echo "Development mode enabled"
		}
	default {
		echo "Unknown parameter: $1"
		}
	}
}
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
#     R_HOME="/usr/lib/R"
#     R_ARCH="/x64"
#     R_DOC_DIR="/usr/share/doc/R"
#     R_SHARE_DIR="/usr/share/R"
#     R_INCLUDE_DIR="/usr/include/R"
#     R_LIBS_SITE="/usr/local/lib/R/site-library"
#     JAVA_HOME="/usr/lib/jvm/java-6-sun"

$R_HOME=""
$R_ARCH="/x64"
$JAVA_HOME=""

###############################################################################
# Set the home and work directory of this R server.
# The home must contain the jar files of this server, if this file is in this
# directory, you don't have to change the value.
# The working directory is for files like log files
##
# Example:
#     RJS_HOME=`dirname "$SCRIPT"`
#     RJS_WORK="~/.RJServer"

$RJS_HOME=Split-Path -Path $SCRIPT -Parent 
$RJS_WORK="$HOME/.RJServer"

###############################################################################
# Set explicitly the hostname and port you want to use to access the RMI 
# registry/R server.
# It is recommended to use the IP address instead of a name.
# By default, the hostname is extracted form the specified RMI address 
# (script parameter). Nevertheless there are some reasons to set it explicitly
# here, e.g.:
#  - For SSH tunnel connections '127.0.0.1' (localhost) is sufficient;
#    recommend, if no other network connections are used; and required if the 
#    public IP address is blocked by the firewall.
#  - To make sure that the correct IP address is set even the hostname is 
#    used in the RMI address.
##
# Usage:
#     S_HOSTADDRESS=<ip or hostname>
#     S_REGISTRYPORT=<port of rmi-registry>
# Example:
#     S_HOSTADDRESS=192.168.1.80

#$S_HOSTADDRESS=""


###############################################################################
# Add additional java options here
##
# Example:
#     JAVA_OPTS="-server -Dde.walware.rj.rmi.disableSocketFactory=true"

$JAVA_OPTS=@("-server")
$JAVA_OPTS_LIB=@()


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

$AUTH="none"

###############################################################################
# Authentication method: password file / 'name-pass'
# 
# Authentication using loginname-password combination.
##
# General usage:
#     RJ Param:    -auth=name-pass:file=<passwordfile>
# 
# Script usage:
#     AUTH="name-pass:file"
#     AUTH_PW_FILE="~/.RJServer/logins"
# 
# <passwordfile> is the path to a property file with
#     pairs of loginname and password, for example:
#         myname=mypassword
#     Make sure that only authorized users can read this file!

#$AUTH="name-pass:file"
#$AUTH_PW_FILE="$HOME/.RJServer/logins"

###############################################################################
# Authentication method: local user account / 'local-shaj'
# 
# Authentication using your local loginname-password combination (PAM).
# The 'Shaj' library is required for this method. It is provided in sources and
# binary form for several platforms. If no binary match your environment, you
# have to build it for your system. You find the files inside the folder
#     shaj
# of this remotetools package.
##
# General Usage:
#     RJ Param:    -auth=local-shaj
#     Java Prop:   java.library.path=<folder of library>
# 
# Script Usage:
#     AUTH="local-shaj"
#     AUTH_SHAJ_LD_DIR="<folder of library>"
# Example:
#     AUTH=local-shaj
#     AUTH_SHAJ_LD_DIR="$RJS_HOME/shaj/linux-x64"

#$AUTH="local-shaj"
#$AUTH_SHAJ_LD_DIR="$RJS_HOME/shaj/<platform>"

###############################################################################
# Authentication method: ssh / 'fx' (exchange over file)
# 
# Authentication for automatic startup (via ssh by StatET).
##
# General Usage:
#     RJ Param:    -auth=fx:file=<keyfile>
# 
# Script Usage:
#     AUTH="fx:file"
#     AUTH_FX_FILE="<keyfile>"
#     AUTH_FX_USER=$USER
#     AUTH_FX_MASK="600"
# 
# <keyfile> a (empty). The fs permission to edit the files represents the
#     right to login into R, so:
#     The file must be writable for authorized user only!
#     If the script setup is used, the files is created automatically and
#     the permission is set according to AUTH_FX_USER and AUTH_FX_MASK

#$AUTH="fx:file"
$AUTH_FX_FILE="$RJS_WORK/session-$S_NAME.lock"
$AUTH_FX_USER=[System.Security.Principal.WindowsIdentity]::GetCurrent().Name
$AUTH_FX_MASK=new-object System.Security.AccessControl.FileSystemAccessRule $AUTH_FX_USER, 'FullControl', 'Allow'


###############################################################################
# SCRIPT - STARTUP SERVER #####################################################
###############################################################################
# Usually you don't have to edit the lines below
$PATH_SEP=";"

#$JAVA_HOME/bin/rmiregistry &

if ( !( Test-Path "$RJS_WORK" ) ) {
	mkdir -p "$RJS_WORK"
}

## Final RMI address
if ( $S_HOSTADDRESS )
{
	$S_ADDRESS="//$S_HOSTADDRESS"
	if ( $S_REGISTRYPORT ) {
		$S_ADDRESS="$S_ADDRESS:$S_REGISTRYPORT"
	}
	$S_ADDRESS="$S_ADDRESS/$S_NAME"
} else {
	$S_ADDRESS="//$S_NAME"
}

## Finish auth configuration
if ( $FORCE_AUTH )
{
	$AUTH="$FORCE_AUTH"
}

if ( $AUTH -eq "name-pass:file" )
{
	$AUTH="name-pass:file=$AUTH_PW_FILE"
}

if ( $AUTH -eq "local-shaj" )
{
	$AUTH="local-shaj"
	$JAVA_OPTS_LIB="$JAVA_OPTS_LIB$PATH_SEP$AUTH_SHAJ_LD_DIR"
	$JAVA_CP="$JAVA_CP$PATH_SEP$RJS_HOME/shaj/auth.jar"
}

if ( "$AUTH" -eq "fx:file" )
{
	$AUTH="fx:file=$AUTH_FX_FILE"
	$AUTH_FX_FOLDER=Split-Path -Path $AUTH_FX_FILE -Parent
	mkdir -p "$AUTH_FX_FOLDER"
	echo "00000000" > "$AUTH_FX_FILE"
	
	$ACL=Get-Acl "$AUTH_FX_FILE"
	$ACL.SetAccessRule($AUTH_FX_MASK)
	#$SystemAccessRule = new-object System.Security.AccessControl.FileSystemAccessRule 'SYSTEM', 'FullControl', 'Allow'
	#$ACL.SetAccessRule($SystemAccessRule)
	$ACL.SetAccessRuleProtection($True, $False)
	Set-Acl "$AUTH_FX_FILE" $ACL
}

$OPTS=@("-auth=$AUTH")
if ( $DEBUG )
{
	$OPTS+="-verbose"
}

## Java config
$JAVA_EXE="$JAVA_HOME/bin/java.exe"
if ( $DEV )
{
	$JAVA_CP="$RJS_HOME/../de.walware.rj.server/bin$PATH_SEP$RJS_HOME/../de.walware.rj.data/bin$PATH_SEP$RJS_HOME/bin$PATH_SEP$RJS_HOME/binShaj"
	$RMI_BASE="file://$RJS_HOME/../de.walware.rj.server/bin"
	$RJAVA_CP=""
} else {
	$JAVA_CP="$RJS_HOME/de.walware.rj.server.jar$PATH_SEP$RJS_HOME/de.walware.rj.data.jar$PATH_SEP$JAVA_CP"
	$RMI_BASE="file://$RJS_HOME/de.walware.rj.server.jar"
	$RJAVA_CP=""
}

$JAVA_OPTS+="-Djava.security.policy=$RJS_HOME/security.policy"
$JAVA_OPTS+="-Djava.rmi.server.codebase=$RMI_BASE"
if ( $S_HOSTADDRESS )
{
	$JAVA_OPTS+="-Djava.rmi.server.hostname=$S_HOSTADDRESS"
}
if ( $JAVA_OPTS_LIB )
{
	$JAVA_OPTS+="-Djava.library.path=$JAVA_OPTS_LIB"
}
if ( $RJAVA_CP )
{
	$JAVA_OPTS+="-Drjava.class.path=$RJAVA_CP"
}

## Other environment
$PATH="$R_HOME/bin$R_ARCH$PATH_SEP$Env:PATH"
$LD_LIBRARY_PATH="$R_HOME/lib"

$env:PATH = $PATH
$env:JAVA_HOME = $JAVA_HOME
$env:R_HOME = $R_HOME
$env:R_ARCH = $R_ARCH
$env:R_LIBS = $R_LIBS
$env:R_LIBS_USER = $R_LIBS_USER
$env:R_LIBS_SITE = $R_LIBS_SITE
$env:R_DOC_DIR = $R_DOC_DIR
$env:R_SHARE_DIR = $R_SHARE_DIR
$env:R_INCLUDE_DIR = $R_INCLUDE_DIR
$env:LC_ALL = $LC_ALL

cd $WD

$START_ARGS=@('-cp',$JAVA_CP)+$JAVA_OPTS+@('de.walware.rj.server.RMIServerControl','start',$S_ADDRESS)+$OPTS
#echo "$JAVA_EXE $START_ARGS"

if ( $DEBUG )
{
	echo "S_HOSTADDRESS = $S_HOSTADDRESS"
	echo "S_REGISTRYPORT = $S_REGISTRY_PORT"
	echo "PATH = $PATH"
	echo "LD_LIBRARY_PATH = $LD_LIBRARY_PATH"
	echo "R_HOME = $R_HOME"
	echo "R_ARCH = $R_ARCH"
	echo "JAVA_HOME = $JAVA_HOME"
	echo "JAVA_EXE = $JAVA_EXE"
	echo "CLASSPATH = $JAVA_CP"
	echo "JAVA_OPTIONS = $JAVA_OPTS"
	echo "AUTH = $AUTH"
	
	# Start server directly
	& $JAVA_EXE $START_ARGS
	$START_EXIT=$LastExitCode
	exit $START_EXIT
} else {
	# First check if running or dead server is already bound
	$CLEAN_ARGS=@('-cp',$JAVA_CP)+$JAVA_OPTS+@('de.walware.rj.server.RMIServerControl','clean',$S_ADDRESS)
	& $JAVA_EXE $CLEAN_ARGS
	$CLEAN_EXIT=$LastExitcode
	if ( $CLEAN_EXIT -ne 0 )
	{
		echo "Check and cleanup of old server failed (CODE=$CLEAN_EXIT), cancelling startup."
		exit $CLEAN_EXIT
	}
	
	# Start server detached
	$JAVA_EXE="$JAVA_HOME/bin/javaw.exe"
	$START_PROCESS=Start-Process $JAVA_EXE -NoNewWindow -PassThru`
			-RedirectStandardOutput "$RJS_WORK/session-$S_NAME.out" `
			-RedirectStandardError "$RJS_WORK/session-$S_NAME.err" `
			$START_ARGS
	#$START_EXIT=
	$START_PID=$START_PROCESS.Id
	if ( ! $START_PROCESS.HasExited )
	{
		echo "Started server in background (PID=$START_PID)."
		exit 0
	} else {
		echo "Startup failed"
		exit -1
	}
}
