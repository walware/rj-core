## RJ init

#' Initializes the package
.onLoad <- function(libname, pkgname) {
	
	utilsEnv <- getNamespace("utils")
	assign("help", envir = .rj.originals, value = get("help", envir = utilsEnv))
	assign("help.start", envir = .rj.originals, value = get("help.start", envir = utilsEnv))
	assign("print.help_files_with_topic", envir = .rj.originals, value = get("print.help_files_with_topic", envir = utilsEnv))
	
	assign("file.choose", envir = .rj.originals, value = get("file.choose", envir = .BaseNamespaceEnv))
	
	return (invisible(TRUE))
}

.rj.originals <- new.env()


.rj.initRJava <- function() {
	require(rJava)
	if (.jinit() < 0) {
		stop(".jinit failed.")
	}
	
	assign("rjInstance", .jcall("de/walware/rj/server/RJ", "Lde/walware/rj/server/RJ;", "get"), .rj.originals)
}

#' Checks if the package (binding to Java part) is successfully initialized
#' @param jpackage if additional java package is required
.rj.checkRJavaInit <- function() {
	if (is.null(.rj.originals$rjInstance)) {
		stop("RJ not (successfully) initialized")
	}
	return (invisible())
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
