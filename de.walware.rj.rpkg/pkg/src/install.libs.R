# just a wrapper to system() that shows what we are doing ...
sys <- function(x, ...) {
  message(x)
  s <- system(x, ...)
#  if (!isTRUE(s == 0L)) message("-> FAILED")
  s
}

## the next part is about JRI
## on OS X we need to merge architectures into one fat file
if (length(grep("^darwin", R.version$os))) {
  is.fat <- function(fn) { a <- 0L; try({f <- file(fn, "rb"); a <- readBin(f, 1L, 1L); close(f)}, silent=TRUE); isTRUE(a[1] == -1095041334L || a[1] == -889275714L) } ## fat magic is 0xCAFEBABE - either endianness
  jni <- "../jri/libjri.jnilib"
  if (isTRUE(file.exists(jni))) { ## continue only if JRI was actually compiled
    dir.create(file.path(R_PACKAGE_DIR, "jri"), recursive = TRUE, showWarnings = FALSE)
    dest <- file.path(R_PACKAGE_DIR, "jri", "libjri.jnilib")
    if (is.fat(jni) || !file.exists(dest)) {
      ## if the new file is fat we assume it has all archs so we copy; if there is no dest, copy as well
      file.copy(jni, dest, overwrite = TRUE)
    } else { ## new file is single arch, so merge
      ## we try lipo -create first and fall back to -replace if it doesn't work (becasue the arch exists already)
      if (sys(paste("/usr/bin/lipo -create", shQuote(dest), jni, "-o", shQuote(dest), ">/dev/null 2>&1")) != 0) {
        if (is.fat(dest)) { # if the file is fat, replace, otherwise it means we have the same arch so copy
          arch <- gsub("/", "", R_ARCH, fixed=TRUE)
          sys(paste("/usr/bin/lipo -replace", arch, jni, shQuote(dest), "-o", shQuote(dest), ">/dev/null 2>&1"))
        } else file.copy(jni, dest, overwrite = TRUE)
      }
    }
  }
} else { ## on other platforms we simply install in jri$(R_ARCH)
  jri <- if (WINDOWS) "jri.dll" else "libjri.so"
  jni <- file.path("..", "jri", jri)
  if (isTRUE(file.exists(jni))) { ## continue only if JRI was actually compiled
    libarch <- if (nzchar(R_ARCH)) paste("jri", R_ARCH, sep='') else "jri"
    dir.create(file.path(R_PACKAGE_DIR, libarch), recursive = TRUE, showWarnings = FALSE)
    dest <- file.path(R_PACKAGE_DIR, libarch, jri)
    file.copy(jni, dest, overwrite = TRUE)
  }
}
