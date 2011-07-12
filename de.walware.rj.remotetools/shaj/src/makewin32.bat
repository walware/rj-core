@echo off

call "d:/Program Files/Microsoft Visual Studio .NET/Common7/Tools/vsvars32.bat"

del *.obj *.dll *.exp *.lib

set SRCS=shaj_common.c win32/shaj_sspi.c win32/shaj_jni_impl.c win32/shaj_netgroup.c

cl /c -DOS_IS_WINDOWS -I%JAVA_HOME%\include -I%JAVA_HOME%\include\win32  %SRCS%

LINK /DLL /OUT:shaj.dll *.obj advapi32.lib netapi32.lib
