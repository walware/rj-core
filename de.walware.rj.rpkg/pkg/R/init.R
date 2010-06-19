## RJ init

#' Initializes the package
.onLoad <- function(libname, pkgname) {
	if (.jinit() < 0) {
		stop(".jinit failed.")
	}
	if (!.jpackage(pkgname, lib.loc = libname)) {
		stop(".jpackage failed.")
	}
	
	.rj.jInstance <<- .jcall("de/walware/rj/server/RJ", "Lde/walware/rj/server/RJ;", "get")
	
	utilsEnv <- as.environment("package:utils")
	assign("help", envir = .rj.originals, value = get("help", envir = utilsEnv))
	assign("?", envir = .rj.originals, value = get("?", envir = utilsEnv))
	assign("help.start", envir = .rj.originals, value = get("help.start", envir = utilsEnv))
	
	invisible(TRUE)
}

.rj.jInstance <- NULL

.rj.originals <- new.env()

#' Checks if the package (binding to Java part) is successfully initialized
#' @param jpackage if additional java package is required
.rj.checkInit <- function(jpackage = FALSE) {
	if (is.null(.rj.jInstance)) {
		stop("RJ not (successfully) initialized")
	}
	invisible()
}

