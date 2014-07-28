## StatET utilities

#' Estimates a value for the parameter max.level of \code{str}
#' for the given R element.
#' 
#' @param x the R element to show
#' @param max.elements maximum elements to show
#' @param max.levels maximum levels to show
#' @returnType integer
#' @return a value for max.level
#' @author Stephan Wahlbrink
#' @export
.estimateStrDepth <- function(x, max.elements = 2000L, max.levels = 10L) {
	deeplength <- function(x, level = 0L) {
		if (isS4(x)) {
			xslots <- slotNames(class(x))
			xlen <- length(xslots)
			if (level == 0L || xlen == 0L) {
				return (xlen)
			}
			else {
				s <- 0L
				for (xslot in xslots) {
					s <- s + deeplength(slot(x, xslot), level = level-1L)
				}
				return (s)
			}
		}
		if (is.list(x)) {
			xlen <- length(x)
			if (level == 0L || xlen == 0L || is.data.frame(x)) {
				return (xlen)
			}
			else {
				return (sum(sapply(x, deeplength, level = level-1L, USE.NAMES=FALSE)))
			}
		}
		return (0L)
	}
	
	level <- -1L
	s <- 0L
	while (level < max.levels && s < max.elements) {
		level <- level + 1L
		s.level <- deeplength(x, level)
		if (s.level == 0L) {
			return (NA)
		}
		s <- s + s.level
	}
	return (level)
}

#' Captures output of \code{str} for the given R element.
#' The output is limited to the specified number of R elements.
#' 
#' @param x the R element to show
#' @param limit maximum elements to show
#' @returnType character
#' @return output of \code{str}
#' @author Stephan Wahlbrink
#' @export
.statet.captureStr <- function(x, limit = 2000L) {
	savedOptions <- options(width = 10000L)
	on.exit(options(savedOptions))
	levels <- .estimateStrDepth(x, max.elements = limit)
	if (!is.na(levels) && levels <= 1L && getRversion() >= "2.11.0") {
		output <- capture.output(str(x, max.level = 1L, list.len = limit - 1L))
	}
	else {
		output <- capture.output(str(x, max.level = levels))
	}
	return (output)
}


.statet.prepareSrcfile <- function(filename, path) {
	map <- .rj.tmp$statet.SrcfileMap
	if (is.null(map)) {
		map <- list()
	}
	map[[filename]] <- NULL
	map[[filename]] <- path
	if (length(map) > 20) {
		map[[1]] <- NULL
	}
	.rj.tmp$statet.SrcfileMap <- map
	return (invisible(NULL))
}

.statet.extSrcfile <- function(srcfile) {
	map <- .rj.tmp$statet.SrcfileMap
	path <- NULL
	if (is.null(map) || is.null(srcfile$filename)) {
		return (srcfile)
	}
	idx <- which(names(map) == srcfile$filename)
	if (length(idx) != 1) {
		return (srcfile)
	}
	path <- map[[idx]]
	
	if (idx <= 10) {
		map[[idx]] <- NULL
		map[[srcfile$filename]] <- path
		.rj.tmp$statet.SrcfileMap <- map
	}
	
	srcfile$statet.Path <- path
	return (srcfile)
}

.addElementIds <- function(expr, elementIds) {
	names <- names(elementIds)
	for (i in seq_along(names)) {
		if (!is.na(names[i])) {
			path <- elementIds[[i]]
			tryCatch(attr(expr[[path]], "statet.ElementId") <- names[i],
					error = .rj.errorHandler )
		}
	}
	return (expr)
}

.statet.prepareCommand <- function(lines, filename = "<text>",
		srcfileAttributes, elementIds) {
	# create srcfile object
	srcfile <- srcfilecopy(filename, lines)
	if (!missing(srcfileAttributes)) {
		names <- names(srcfileAttributes)
		for (i in seq_along(names)) {
			if (!is.na(names[i])) {
				assign(names[i], srcfileAttributes[[i]], envir= srcfile)
			}
		}
	}
	
	# parse command
	expr <- parse(text= lines, srcfile= srcfile, encoding= "UTF-8")
	
	# attach element ids
	if (!missing(elementIds)) {
		expr <- .addElementIds(expr, elementIds)
	}
	
	# finish
	.rj.tmp$statet.CommandExpr <- expr
	invisible(expr)
}

.statet.evalCommand <- function() {
	expr <- .rj.tmp$statet.CommandExpr
	if (is.null(expr)) {
		stop("Commands not available.")
	}
	srcrefs <- attr(expr, "srcref", exact= TRUE)
	exi <- call("{", expr[[1]])
	if (1 <= length(srcrefs)) {
		attr(exi, "srcref") <- list(NULL, srcrefs[[1]])
	}
	eval(exi, parent.frame())
}

.statet.prepareSource <- function(info) {
	assign("statet.NextSourceInfo", info, envir= .rj.tmp)
}

.statet.extSource <- function(expr) {
	info <- .rj.tmp$statet.NextSourceInfo
	if (is.null(info)) {
		return (expr)
	}
	on.exit(rm("statet.NextSourceInfo", envir= .rj.tmp))
	if (is.null(expr)) {
		return (expr)
	}
	srcfile <- attr(expr, "srcfile", exact= TRUE)
	if (is.null(srcfile)
			|| is.null(srcfile$statet.Path) || is.null(srcfile$timestamp)
			|| srcfile$statet.Path != info$path
			|| (unclass(srcfile$timestamp) != info$timestamp
				&& abs(unclass(srcfile$timestamp) - info$timestamp) != 3600 )
			|| length(expr) != info$exprsLength ) {
		return (expr)
	}
	expr <- .addElementIds(expr, info$elementIds)
	return (expr)
}


#' Initializes the debug tools
.statet.initDebug <- function() {
	if (options("keep.source") != TRUE) {
		options("keep.source" = TRUE)
	}
	baseEnv <- as.environment("package:base")
	
	# ext base::srcfile
	.injectSrcfile <- function(fname, envir) {
		ffun <- get(fname, envir= envir)
		fbody <- body(ffun)
		l <- length(fbody)
		if (length(fbody[[l]]) == 2 && fbody[[l]][[1]] == "return") {
			c1 <- quote(rj:::.statet.extSrcfile(x))
			c1[[2]] <- fbody[[l]][[2]]
			fbody[[l]][[2]] <- c1
			body(ffun) <- fbody
			return (.patchPackage(fname, ffun, envir= envir))
		}
		cat("Could not install rj extension for '", fname, "'.\n", sep= "")
		return (FALSE)
	}
	.injectSource <- function(fname, envir) {
		ffun <- get(fname, envir= envir)
		fbody <- body(ffun)
		l <- length(fbody)
		for (i in 1L:l) {
			if (length(fbody[[i]]) == 3 && fbody[[i]][[1]] == "<-" && fbody[[i]][[2]] == "exprs") {
				c1 <- quote(rj:::.statet.extSource(x))
				c1[[2]] <- fbody[[i]][[3]]
				fbody[[i]][[3]] <- c1
				body(ffun) <- fbody
				return (.patchPackage(fname, ffun, envir= envir))
			}
		}
		# For 2.14.0
		for (i in 1L:l) {
			if (length(fbody[[i]]) == 4 && length(fbody[[i]][[4]]) == 3
					&& fbody[[i]][[4]][[1]] == "<-" && fbody[[i]][[4]][[2]] == "exprs") {
				c1 <- quote(rj:::.statet.extSource(x))
				c1[[2]] <- fbody[[i]][[4]][[3]]
				fbody[[i]][[4]][[3]] <- c1
				body(ffun) <- fbody
				return (.patchPackage(fname, ffun, envir= envir))
			}
		}
		cat("Could not install rj extension for '", fname, "'.\n", sep= "")
		return (FALSE)
	}
	.injectSource("source", baseEnv)
	.injectSrcfile("srcfile", baseEnv)
	.injectSrcfile("srcfilecopy", baseEnv)
	return (invisible())
}

#### R env / R lib path (R pkg manager)

.renv.checkLibs <- function() {
	libs <- .libPaths()
	result <- file.info(libs)$mtime
	names(result) <- libs
	
	result
}

.renv.getAvailPkgs <- function(repo) {
	fields= c('Package', 'Version', 'Priority', 'License',
			'Depends', 'Imports', 'LinkingTo', 'Suggests', 'Enhances')
	result <- available.packages(contriburl= contrib.url(repo), fields= fields,
			filter= c('R_version', 'OS_type', 'subarch') )
	result[, fields, drop= FALSE]
}

.renv.getInstPkgs <- function(lib) {
	names <- list.files(lib)
	fields <- c('Package', 'Version', 'Title', 'Built')
	result <- matrix(NA_character_, nrow= length(names), ncol= length(fields))
	num <- 0L
	for (name in names) {
		pkgpath <- file.path(lib, name)
		if (file.access(pkgpath, 5L)) {
			next
		}
		if (file.exists(file <- file.path(pkgpath, 'Meta', 'package.rds'))) {
			md <- try(readRDS(file))
			if (inherits(md, 'try-error')) {
				next
			}
			descr <- md$DESCRIPTION[fields]
			if (is.null(descr)) {
				next
			}
			enc <- md$DESCRIPTION['Encoding']
			if (!is.na(enc)) {
				txt <- try(iconv(descr[3L], from= enc, to= "UTF-8"))
				if (!inherits(txt, "try-error")) {
					descr[3L] <- txt
				}
			}
			descr[1L] <- name
			result[num <- num + 1, ] <- descr
		}
	}
	result[seq.int(from= 1L, length.out= num), , drop= FALSE]
}

.renv.getInstPkgDetail <- function(lib, name) {
	fields <- c('Priority', 'License',
			'Depends', 'Imports', 'LinkingTo', 'Suggests', 'Enhances' )
	file <- file.path(lib, name, 'Meta', 'package.rds')
	md <- readRDS(file)
	md$DESCRIPTION[fields]
}

.renv.isValidLibLoc <- function(path) {
	# path <- normalizePath(path)
	current <- path
	repeat {
		if (file.access(current, 0L) == 0L) { # exists
			result <-  file.access(current, 3L) # writable
			names(result) <- path
			return (result)
		}
		parent <- dirname(current)
		if (nchar(parent) <= 1L || parent == current) {
			return (-1L)
		}
		current <- parent
	}
}

#### R help

.rhelp.loadPkgDescr <- function(lib, name) {
	fields <- c('Version', 'Title', 'Description', 'Author', 'Maintainer', 'URL', 'Built')
	file <- file.path(lib, name, 'Meta', 'package.rds')
	md <- readRDS(file)
	descr <- md$DESCRIPTION[fields]
	if (is.null(descr)) {
		return (NULL)
	}
	enc <- md$DESCRIPTION['Encoding']
	if (!is.na(enc)) {
		txt <- try(iconv(descr[2L:5L], from= enc, to= "UTF-8"))
		if (!inherits(txt, "try-error")) {
			descr[2L:5L] <- txt
		}
	}
	return (descr)
}

.renderRd <- function(rd, pkg.name) {
	rdHTML <- NULL
	tmpout <- textConnection("rdHTML", open = "w", local = TRUE, encoding = "UTF-8")
	tools:::Rd2HTML(rd, out = tmpout, dynamic = TRUE, package = pkg.name)
	close(tmpout)
	Encoding(rdHTML) <- "UTF-8"
	return (rdHTML)
}

.rhelp.loadPkgRd <- function(lib, name) {
	rdDB.base <- file.path(lib, name, "help", name)
	rdDB.file <- paste0(rdDB.base, ".rdb")
	if (!file.exists(rdDB.file)) {
		stop("Missing Rd file.")
	}
	
	if (getRversion() >= "3.0.0") {
		flushRdDB <- function() invisible(.Internal(lazyLoadDBflush(rdDB.file)))
	}
	else {
		flushRdDB <- function() invisible()
	}
	
	flushRdDB()
	
	rdDB <- tools:::fetchRdDB(rdDB.base)
	
	getTEXT <- function(node) {
		tags <- c("TEXT", "VERB")
		.get <- function(x) {
			if (is.list(x)) {
				l <- length(x)
				if (l == 0) {
					return (NULL)
				}
				txts <- NULL
				for (i in 1:l) {
					txt <- .get(x[[i]])
					if (!is.null(txt)) {
						txts <- c(txts, txt)
					}
				}
				return (txts)
			}
			if (is.character(x) && attr(x, "Rd_tag", exact= TRUE) %in% tags) {
				return (x)
			}
		}
		txt <- paste(.get(node), collapse = " ")
		if (length(txt) == 1) {
			txt <- gsub("([[:space:]]+)", " ",
					gsub("(^[[:space:]]+)|([[:space:]]+$)", "", txt) )
			return (txt)
		}
		else {
			return ("")
		}
	}
	
	extractFields <- function(rd, rdData) {
		for (i in seq(along = rd)) {
			tag <- attr(rd[[i]], "Rd_tag", exact= TRUE)
			if (is.null(tag)) {
				next
			}
			if (tag == "\\alias") {
				rdData$topics <- c(rdData$topics, getTEXT(rd[[i]]))
				next
			}
			if (tag == "\\title") {
				rdData$title <- getTEXT(rd[[i]])
				next
			}
			if (tag == "\\keyword") {
				rdData$keywords <- c(rdData$keywords, getTEXT(rd[[i]]))
				next
			}
			if (tag == "\\concept") {
				rdData$concepts <- c(rdData$concepts, getTEXT(rd[[i]]))
				next
			}
		}
		
		return (rdData)
	}
	
	createRdData <- function(rd) {
		try( {
			rdData <- list(title = NA_character_,
					topics = character(length = 0),
					keywords = character(length = 0),
					concepts = character(length = 0) )
			rdData <- extractFields(rd, rdData)
			rdData$HTML <- .renderRd(rd, name)
			class(rdData) <- "RdData"
			rdData
		}, silent = TRUE)
	}
	
	data <- lapply(X = rdDB, FUN = createRdData)
	
	flushRdDB()
	
	return (data)
}
