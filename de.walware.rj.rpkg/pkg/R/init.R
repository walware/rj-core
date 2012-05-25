## RJ init

#' Initializes the package
.onLoad <- function(libname, pkgname) {
	utilsEnv <- getNamespace("utils")
	assign("help", envir = .rj.originals, value = get("help", envir = utilsEnv))
	assign("help.start", envir = .rj.originals, value = get("help.start", envir = utilsEnv))
	assign("print.help_files_with_topic", envir = .rj.originals, value = get("print.help_files_with_topic", envir = utilsEnv))
	
	baseEnv <- .BaseNamespaceEnv
	assign("file.choose", envir = .rj.originals, value = get("file.choose", envir = baseEnv))
	assign("srcfile", envir = .rj.originals, value = get("srcfile", envir = baseEnv))
	
	return (invisible(TRUE))
}

.rj.originals <- new.env()

.patchPackage <- function(name, value, envir, ns = TRUE) {
	if (exists(name, envir)) {
		unlockBinding(name, envir)
		on.exit(lockBinding(name, envir), add= TRUE)
		assign(name, value, envir)
	}
	if (ns && getRversion() < "2.15.0") {
		envName <- environmentName(envir)
		if (envName == "base") {
			ns <- "base"
		}
		else if (!is.null(envName) && substring(envName, 1L, 8L) == "package:") {
			ns <- asNamespace(substring(envName, 9L))
		}
		else {
			ns <- NULL
		}
		if (!is.null(ns)) {
			assignInNamespace(name, value, ns= ns)
		}
	}
	return (invisible(TRUE))
}


#' Environment for temporary R objects
.rj.tmp <- new.env()

#' Returns the next available id (a element name with the specified prefix)
#' 
#' @param prefix prefix of the element name, usually a key for a element type
#' @param envir optional environment, default is \code{.rj.tmp}
#' @return the id
#' @returnType char
.rj.nextId <- function(prefix, envir = .rj.tmp) {
	i <- 1L; 
	repeat {
		name <- paste(prefix, i, sep = ""); 
		if (!exists(name, envir = envir, inherits = FALSE)) {
			assign(name, NULL, envir = envir); 
			return(name); 
		}
		i <- i + 1L; 
	}
}


.rj.tmp$Debug <- FALSE

.rj.errorHandler <- function(e) {
	if (.rj.tmp$Debug) {
		print(e)
	}
}

resolveVisible <- function(result) {
	if (result$visible) {
		return (result$value)
	}
	else {
		return (invisible(result$value))
	}
}
