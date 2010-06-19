## StatET help

.getHelpFile <- function(file) {
	path <- dirname(file)
	dirpath <- dirname(path)
	if(!file.exists(dirpath))
		stop(gettextf("invalid '%s' argument", "file"), domain = NA)
	pkgname <- basename(dirpath)
	RdDB <- file.path(path, pkgname)
	if(!file.exists(paste(RdDB, "rdx", sep=".")))
		stop(gettextf("package %s exists but was not installed under R >= 2.10.0 so help cannot be accessed", sQuote(pkgname)), domain = NA)
	tools:::fetchRdDB(RdDB, basename(file))
}

#' Creates rhelp url, mostly compatible to \code{help}
#' 
#' @param topic 
#' @param package 
#' @param ... for compatibility
#' @returnType character
#' @return URL in rhelp schema
.getRHelpUrl <- function (topic, package, ..., paths) {
	if (!missing(package)) {
		if (is.name(y <- substitute(package))) {
			package <- as.character(y)
		}
	}
	if (inherits(paths, "packageInfo")) {
		return (paste("rhelp:///page", package, "", sep = "/"))
	}
	if (inherits(paths, "help_files_with_topic")) {
		if (missing(topic)) {
			topic <- "help"
			package <- "utils"
		}
		else {
			topic <- attr(paths, "topic")
		}
		if (missing(package)) {
			return (paste("rhelp:///topic", topic, sep = "/"))
		}
		else {
			return (paste("rhelp:///page", package, topic, sep = "/"))
		}
	}
	stop("Unexpected help information.")
}

#' Creates HTML help page for the given help page(s).
#' 
#' @param x vector of help pages (as returns by original \code{help}
#' @return HTML page for a single page, otherwise \code{NULL}
#' @returnType character
.getLiveHelp <- function(x) {
	paths <- as.character(x)
	if(!length(paths)) {
		writeLines(c(gettextf("No documentation for '%s' in specified packages and libraries:",
								topic),
						gettextf("you could try '??%s'",
								topic)))
		return (invisible(NULL))
	}
	
	if (attr(x, "tried_all_packages")) {
		paths <- unique(dirname(dirname(paths)))
		msg <- gettextf("Help for topic '%s' is not in any loaded package but can be found in the following packages:",
				topic)
		writeLines(c(strwrap(msg), "",
						paste(" ", formatDL(c(gettext("Package"), basename(paths)),
										c(gettext("Library"), dirname(paths)),
										indent = 22))))
		return (invisible(NULL))
	}
	
	file <- NULL
	if (length(paths) > 1L) {
		p <- paths
		msg <- gettextf("Help on topic '%s' was found in the following packages:",
				topic)
		paths <- dirname(dirname(paths))
		txt <- formatDL(c("Package", basename(paths)),
				c("Library", dirname(paths)),
				indent = 22L)
		writeLines(c(strwrap(msg), "", paste(" ", txt), ""))
		if (interactive()) {
			fp <- file.path(paths, "Meta", "Rd.rds")
			tp <- basename(p)
			titles <- tp
			if(type == "html" || type == "latex")
				tp <- tools::file_path_sans_ext(tp)
			for (i in seq_along(fp)) {
				tmp <- try(.readRDS(fp[i]))
				titles[i] <- if(inherits(tmp, "try-error"))
							"unknown title" else
							tmp[tools::file_path_sans_ext(tmp$File) == tp[i], "Title"]
			}
			txt <- paste(titles, " {", basename(paths), "}", sep="")
			## the default on menu() is currtently graphics = FALSE
			res <- menu(txt, title = gettext("Choose one"),
					graphics = getOption("menu.graphics"))
			if (res > 0) {
				file <- p[res]
			} else {
				file <- NULL
			}
		} else {
			file <- paths[1L]
			writeLines(gettext("\nUsing the first match ..."))
		}
	} else {
		file <- paths
	}
	
	if (is.null(file)) {
		return (invisible(NULL))
	}
	
	file <- sub("/html/([^/]*)\\.html$", "/help/\\1", file)
	if (getRversion() < "2.11.0") {
		rd <- .getHelpFile(file)
	}
	else {
		rd <- utils:::.getHelpFile(file)
	}
	
	.renderRd(rd, basename(dirname(dirname(file))))
}

.showHelp <- function(url) {
	if (missing(url) || !is.character(url) || length(url) != 1) {
		stop("Illegal argument: url")
	}
	.rj_ui.execCommand("r/showHelp", list(
					url = .jnew("java/lang/String", url) ), FALSE)
}

#' Shows R help in StatET. This is a console command for R help in StatET
#' mainly compatible to the original \code{help}.
#' 
#' @seealso help
#' @export
statet_help <- function(..., help_type = "html", live = FALSE) {
	if (help_type != "html") {
		return (.rj.originals$help(..., help_type = "html"))
	}
	if (getRversion() < "2.10.0") {
		paths <- .rj.originals$help(..., chmhelp = FALSE, htmlhelp = TRUE)
		help.statet <- paths
	}
	else {
		paths <- .rj.originals$help(..., help_type = "html")
		if (length(paths) == 0) {
			return (paths); #visible
		}
		if (live) {
			help.statet <- .getLiveHelp(paths)
			if (!is.null(help.statet)) {
				help.statet <- paste(help.statet, collapse = "\n")
				help.statet <- paste("html:///", help.statet, sep = "")
			}
		}
		else {
			help.statet <- .getRHelpUrl(..., paths = paths);
		}
	}
	if (is.character(help.statet) && !is.na(help.statet)) {
		.showHelp(help.statet)
	}
	invisible(paths)
}

#' Shows the R help start page in StatET. This is a console command for R help in StatET
#' mainly compatible to the original \code{help.start}.
#' 
#' At moment no argument functionality is supported.
#' 
#' @param ... for compatibility
#' @seealso help.start
#' @export
statet_help.start <- function(...) {
	.showHelp("rhelp:///")
	invisible()
}


#' Reassigns R help functions with versions for R help in StatET.
#' 
#' @export
.statet.reassign_help <- function(){
	utilsEnv <- as.environment("package:utils")
	rjEnv <- as.environment("package:rj")
	
	unlockBinding("help", utilsEnv)
	assignInNamespace("help", statet_help, ns = "utils", envir = utilsEnv)
	assign("help", statet_help, utilsEnv)
	lockBinding("help", utilsEnv)
	
	unlockBinding("?", utilsEnv)
	assignInNamespace("?", statet_help, ns = "utils", envir = utilsEnv)
	assign("?", statet_help, utilsEnv)
	lockBinding("?", utilsEnv)
	
	unlockBinding("help.start", utilsEnv)
	assignInNamespace("help.start", statet_help.start, ns = "utils", envir = utilsEnv)
	assign("help.start", statet_help.start, utilsEnv)
	lockBinding("help.start", utilsEnv)
}

