#
# What a joy this makefile is. But it works just fine thank you very much.
#

JDK_HOME=$(JAVA_HOME)
OS=LINUX
#OS=SOLARIS
#CC=cc
#JDK_HOME=/Library/Java/Home
#OS=OSX

OBJECTS=\
  shaj_common.o \
  pam/shaj_auth.o \
  pam/shaj_group.o \
  pam/shaj_jni_impl.o

CPPFLAGS=-I${JDK_HOME}/include -Wall
OUR_SOLIBS= 
OUR_SOOPTS= 

ifeq ($(OS),LINUX)
CPPFLAGS+= -I${JDK_HOME}/include/linux -DOS_IS_LINUX
OUR_SOFLAGS+= -shared -o libshaj.so
OUR_SOLIBS+= -lpam -ldl
endif

ifeq ($(OS),OSX)
CPPFLAGS+= -DOS_IS_OSX
OUR_SOFLAGS+= -dynamiclib -o libshaj.jnilib
OUR_SOLIBS+= -lpam -framework JavaVM
JDK_HOME=/Library/Java/Home
endif

ifeq ($(OS),SOLARIS)
CPPFLAGS+= -I${JDK_HOME}/include/solaris -DOS_IS_SOLARIS 
OUR_SOFLAGS+= -G -o libshaj.so
OUR_SOLIBS+= -lpam 
endif

all: libshaj.so

clean:
	rm -f libshaj.so libshaj.jnilib *.o **/*.o

libshaj.so: $(OBJECTS)
	$(CC) $(OUR_SOFLAGS) $(OBJECTS) $(OUR_SOLIBS)
