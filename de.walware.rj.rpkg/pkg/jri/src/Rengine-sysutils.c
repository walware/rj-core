#include <stdio.h>

#include "org_rosuda_JRI_Rengine.h"
#include <errno.h>

#ifdef Win32
#include <windows.h>
#else
#include <unistd.h>
#endif


#ifdef Win32
#define STREAM_HANDLE HANDLE
#define STD_OUT_ID STD_OUTPUT_HANDLE
#define STD_ERR_ID STD_ERROR_HANDLE

static STREAM_HANDLE bakStdHandle(DWORD id) {
	return GetStdHandle(id);
}
static int setStdHandle(DWORD id, STREAM_HANDLE handle) {
	BOOL result= SetStdHandle(id, handle);
	return (result != 0) ? 0 : -1;
}
static int closeHandle(STREAM_HANDLE handle) {
	BOOL result= CloseHandle(handle);
	return (result != 0) ? 0 : -1;
}
static jint readHandle(STREAM_HANDLE handle, void *buffer, jint count) {
	DWORD n= 0;
	BOOL result;
	result= ReadFile(handle, buffer, count, &n, NULL);
	return (result != 0) ? n : -1;
}

#else
#define STREAM_HANDLE int
#define INVALID_HANDLE_VALUE -1
#define STD_OUT_ID STDOUT_FILENO
#define STD_ERR_ID STDERR_FILENO

static STREAM_HANDLE bakStdHandle(int id) {
	int result;
	do {
		result= dup(id);
	} while (result == -1 && errno == EINTR);
	return result;
}
static int setStdHandle(int id, STREAM_HANDLE handle) {
	int result;
	do {
		result= dup2(handle, id);
	} while (result == -1 && errno == EINTR);
	return (result != -1) ? 0 : -1;
}
static int closeHandle(STREAM_HANDLE handle) {
	int result;
	do {
		result= close(handle);
	} while (result == -1 && errno == EINTR);
	return result;
}
static jint readHandle(STREAM_HANDLE handle, void *buffer, jint count) {
	ssize_t n= 0;
	do {
		n= read(handle, buffer, count);
	} while (n == -1 && errno == EINTR);
	return n;
}
#endif

STREAM_HANDLE bakOut_wrHandle= INVALID_HANDLE_VALUE;
STREAM_HANDLE bakErr_wrHandle= INVALID_HANDLE_VALUE;
STREAM_HANDLE sysOut_rdHandle= INVALID_HANDLE_VALUE;
STREAM_HANDLE sysOut_wrHandle= INVALID_HANDLE_VALUE;
void* sysOut_Buffer;
jint sysOut_BufferSize;


JNIEXPORT jstring JNICALL Java_org_rosuda_JRI_Rengine_rniGetSysOutEnc(
		JNIEnv *env, jclass clazz) {
#ifdef Win32
	int cp= GetOEMCP();
	char name[15];
	sprintf(name, "Cp%d", cp);
	return (*env)->NewStringUTF(env, name);
#else
	return NULL;
#endif
}

static void closeSysOut() {
	STREAM_HANDLE old_rdHandle= sysOut_rdHandle;
	STREAM_HANDLE old_wrHandle= sysOut_wrHandle;
	
	sysOut_rdHandle= INVALID_HANDLE_VALUE;
	sysOut_wrHandle= INVALID_HANDLE_VALUE;
	
	if (bakOut_wrHandle != INVALID_HANDLE_VALUE) {
		setStdHandle(STD_OUT_ID, bakOut_wrHandle);
	}
	if (bakErr_wrHandle != INVALID_HANDLE_VALUE) {
		setStdHandle(STD_ERR_ID, bakErr_wrHandle);
	}
	
	if (old_rdHandle != INVALID_HANDLE_VALUE) {
		closeHandle(old_rdHandle);
	}
	if (old_wrHandle != INVALID_HANDLE_VALUE) {
		closeHandle(old_wrHandle);
	}
}

#ifndef Win32
static jint resetJOutput(JNIEnv *env, jarray consoleHandlers) {
	jclass jc;
	jmethodID jm;
	jobject joOut, joErr;
	
	jc= (*env)->FindClass(env, "java/io/FileDescriptor");
	if (jc == NULL) {
		return -1;
	}
	jm= (*env)->GetMethodID(env, jc, "<init>", "(I)V");
	if (jm == NULL) {
		return -1;
	}
	joOut= (*env)->NewObject(env, jc, jm, (jint) bakOut_wrHandle);
	if (joOut == NULL) {
		return -1;
	}
	joErr= (*env)->NewObject(env, jc, jm, (jint) bakErr_wrHandle);
	if (joOut == NULL) {
		return -1;
	}
	
	jc= (*env)->FindClass(env, "java/io/FileOutputStream");
	if (jc == NULL) {
		return -1;
	}
	jm= (*env)->GetMethodID(env, jc, "<init>", "(Ljava/io/FileDescriptor;)V");
	if (jm == NULL) {
		return -1;
	}
	joOut= (*env)->NewObject(env, jc, jm, joOut);
	if (joOut == NULL) {
		return -1;
	}
	joErr= (*env)->NewObject(env, jc, jm, joErr);
	if (joOut == NULL) {
		return -1;
	}
	
	jc= (*env)->FindClass(env, "java/io/PrintStream");
	if (jc == NULL) {
		return -1;
	}
	jm= (*env)->GetMethodID(env, jc, "<init>", "(Ljava/io/OutputStream;Z)V");
	if (jm == NULL) {
		return -1;
	}
	joOut= (*env)->NewObject(env, jc, jm, joOut, JNI_TRUE);
	if (joOut == NULL) {
		return -1;
	}
	joErr= (*env)->NewObject(env, jc, jm, joErr, JNI_TRUE);
	if (joOut == NULL) {
		return -1;
	}
	
	jc= (*env)->FindClass(env, "java/lang/System");
	if (jc == NULL) {
		return -1;
	}
	jm= (*env)->GetStaticMethodID(env, jc, "setOut", "(Ljava/io/PrintStream;)V");
	if (jm == NULL) {
		return -1;
	}
	(*env)->CallStaticVoidMethod(env, jc, jm, joOut);
	if ((*env)->ExceptionCheck(env)) {
		return -1;
	}
	jm= (*env)->GetStaticMethodID(env, jc, "setErr", "(Ljava/io/PrintStream;)V");
	if (jm == NULL) {
		return -1;
	}
	(*env)->CallStaticVoidMethod(env, jc, jm, joErr);
	if ((*env)->ExceptionCheck(env)) {
		return -1;
	}
	
	jc= (*env)->FindClass(env, "java/util/logging/StreamHandler");
	if (jc == NULL) {
		return -1;
	}
	jm= (*env)->GetMethodID(env, jc, "setOutputStream", "(Ljava/io/OutputStream;)V");
	if (jm == NULL) {
		return -1;
	}
	jint n= (*env)->GetArrayLength(env, consoleHandlers);
	for (jint i= 0; i < n; i++) {
		jobject handler = (*env)->GetObjectArrayElement(env, consoleHandlers, i);
		if ((*env)->ExceptionCheck(env)) {
			return -1;
		}
		(*env)->CallVoidMethod(env, handler, jm, joErr);
		if ((*env)->ExceptionCheck(env)) {
			return -1;
		}
	}
	return 0;
}
#endif

JNIEXPORT jint JNICALL Java_org_rosuda_JRI_Rengine_rniInitSysPipes(
		JNIEnv *env, jclass clazz, jobject buffer, jarray consoleHandlers) {
	jint code= 0;
	if (sysOut_rdHandle != INVALID_HANDLE_VALUE) {
		return 161001;
	}
	
	bakOut_wrHandle= bakStdHandle(STD_OUT_ID);
	bakErr_wrHandle= bakStdHandle(STD_ERR_ID);
	
	if (bakOut_wrHandle == INVALID_HANDLE_VALUE || bakErr_wrHandle == INVALID_HANDLE_VALUE) {
		return 171001;
	}
	
#ifdef Win32
	SECURITY_ATTRIBUTES saAttr; 
	saAttr.nLength= sizeof(SECURITY_ATTRIBUTES); 
	saAttr.bInheritHandle= TRUE; 
	saAttr.lpSecurityDescriptor= NULL;
	
	if (!CreatePipe(&sysOut_rdHandle, &sysOut_wrHandle, &saAttr, 0x2000)) {
		sysOut_rdHandle= INVALID_HANDLE_VALUE;
		sysOut_wrHandle= INVALID_HANDLE_VALUE;
		return 201000;
	}
	if (!SetHandleInformation(sysOut_rdHandle, HANDLE_FLAG_INHERIT, 0)) {
		closeSysOut();
		return 203000;
	}
	if (!SetHandleInformation(sysOut_wrHandle, HANDLE_FLAG_INHERIT | HANDLE_FLAG_PROTECT_FROM_CLOSE,
			HANDLE_FLAG_INHERIT | HANDLE_FLAG_PROTECT_FROM_CLOSE )) {
		closeSysOut();
		return 204000;
	}
	
	SetStdHandle(STD_INPUT_HANDLE, INVALID_HANDLE_VALUE);
#else
	if (resetJOutput(env, consoleHandlers)) {
		return 181000;
	}
	{	STREAM_HANDLE sysOut_Handles[2];
		if (pipe(sysOut_Handles)) {
			return 201000 + errno;
		}
		sysOut_rdHandle= sysOut_Handles[0];
		sysOut_wrHandle= sysOut_Handles[1];
	}
	// flag FD_CLOEXEC= 0 by default
#endif
	
	if ((code= setStdHandle(STD_OUT_ID, sysOut_wrHandle))) {
		closeSysOut();
		return 211000;
	}
	if ((code= setStdHandle(STD_ERR_ID, sysOut_wrHandle))) {
		closeSysOut();
		return 212000;
	}
	
	sysOut_Buffer= (*env)->GetDirectBufferAddress(env, buffer);
	if (sysOut_Buffer == NULL) {
		closeSysOut();
		return 131;
	}
	sysOut_BufferSize= (*env)->GetDirectBufferCapacity(env, buffer);
	
	return 0;
}

JNIEXPORT jint JNICALL Java_org_rosuda_JRI_Rengine_rniFlushSysOut(
		JNIEnv *env, jclass clazz) {
#ifdef Win32
	FlushFileBuffers(sysOut_wrHandle);
#endif
	
	return 0;
}

JNIEXPORT jint JNICALL Java_org_rosuda_JRI_Rengine_rniReadSysOut(
		JNIEnv *env, jclass clazz, jint pos) {
	if (pos >= sysOut_BufferSize) {
		return 0;
	}
	return readHandle(sysOut_rdHandle, sysOut_Buffer + pos, sysOut_BufferSize - pos);
}
