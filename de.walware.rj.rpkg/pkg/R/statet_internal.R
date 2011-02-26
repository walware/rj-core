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
			if (level == 0L || xlen == 0L) {
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
	levels <- .estimateStrDepth(x, max.elements = limit)
	if (!is.na(levels) && levels <= 1L && getRversion() >= "2.11.0") {
		output <- capture.output(str(x, max.level = 1L, list.len = limit - 1L))
	}
	else {
		output <- capture.output(str(x, max.level = levels))
	}
	options(savedOptions)
	return (output)
}

.renderRd <- function(rd, pkg.name) {
	rdHTML <- NULL
	tmpout <- textConnection("rdHTML", open = "w", local = TRUE, encoding = "UTF-8")
	tools:::Rd2HTML(rd, out = tmpout, dynamic = TRUE, package = pkg.name)
	close(tmpout)
	return (rdHTML)
}

.statet.checkPkg <- function(id, libPath, name) {
	descr.file <- file.path(libPath, name, "DESCRIPTION")
	rdDB.base <- file.path(libPath, name, "help", name)
	if (!file.exists(descr.file)) {
		return (NULL)
	}
	if (!file.exists(paste(rdDB.base, "rdb", sep = "."))) {
		stop("Missing Rd file.")
	}
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
			if (is.character(x) && attr(x, "Rd_tag") %in% tags) {
				return (x)
			}
		}
		txt <- paste(.get(node), collapse = " ")
		if (length(txt) == 1) {
			txt <- gsub("([[:space:]]+)", " ", gsub("(^[[:space:]]+)|([[:space:]]+$)", "", txt))
			return (txt)
		}
		else {
			return ("")
		}
	}
	
	extractFields <- function(rd, rdData) {
		for (i in seq(along = rd)) {
			tag <- attr(rd[[i]], "Rd_tag")
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
	data$.id <- id
	data$.name <- name
	return (data)
}

