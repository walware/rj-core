## data editor

.checkDataStruct <- function(x, xClass1, xDim) {
	if (class(x)[1] != xClass1) {
		return (FALSE)
	}
	d <- dim(x)
	if (length(d) < 2L) {
		return (length(x) == xDim[1L])
	}
	else if (length(d) == 2) {
		return (d[1L] == xDim[1L] && d[2L] == xDim[2L])
	}
	else {
		return (FALSE)
	}
}

.getDataVectorValues <- function(x, idxs, rowMapping) {
	rowIdxs <- if (missing(rowMapping)) idxs[1L]:idxs[2L] else
			get(rowMapping, envir= .rj.tmp)[idxs[1L]:idxs[2L]]
	x <- x[rowIdxs, drop= FALSE]
	names(x) <- NULL
	x
}

.getDataVectorRowNames <- function(x, idxs, rowMapping) {
	x.names <- names(x)
	if (is.null(x.names)) {
		return (NULL)
	}
	rowIdxs <- if (missing(rowMapping)) idxs[1L]:idxs[2L] else
				get(rowMapping, envir= .rj.tmp)[idxs[1L]:idxs[2L]]
	x.names[rowIdxs]
}

.getDataMatrixValues <- function(x, idxs, rowMapping) {
	rowIdxs <- if (missing(rowMapping)) idxs[1L]:idxs[2L] else
				get(rowMapping, envir= .rj.tmp)[idxs[1L]:idxs[2L]]
	x <- x[rowIdxs, idxs[3L]:idxs[4L], drop= FALSE]
	rownames(x) <- NULL
	x
}

.getDataMatrixRowNames <- function(x, idxs, rowMapping) {
	x.names <- rownames(x)
	if (is.null(x.names)) {
		return (NULL)
	}
	rowIdxs <- if (missing(rowMapping)) idxs[1L]:idxs[2L] else
				get(rowMapping, envir= .rj.tmp)[idxs[1L]:idxs[2L]]
	x.names[rowIdxs]
}

.getDataArrayDimNames <- function(x, idxs) {
	x.dimnames <- dimnames(x)
	if (is.null(x.dimnames)) {
		return (NULL)
	}
	names(x.dimnames)[idxs[1L]:idxs[2L]]
}

.getDataArrayDimItemNames <- function(x, dimIdx, idxs) {
	x.dimnames <- dimnames(x)
	if (is.null(x.dimnames)) {
		return (NULL)
	}
	x.dimnames[[dimIdx]][idxs[1L]:idxs[2L]]
}

.getDataFrameValues <- function(x, idxs, rowMapping) {
	rowIdxs <- if (missing(rowMapping)) idxs[1L]:idxs[2L] else
				get(rowMapping, envir= .rj.tmp)[idxs[1L]:idxs[2L]]
	x <- x[rowIdxs, idxs[3L]:idxs[4L], drop= FALSE]
	attr(x, 'row.names') <- NULL
	x
}

.getDataFrameRowNames <- function(x, idxs, rowMapping) {
	x.names <- attr(x, 'row.names', exact= TRUE)
	if (is.null(x.names)) {
		return (NULL)
	}
	rowIdxs <- if (missing(rowMapping)) idxs[1L]:idxs[2L] else
				get(rowMapping, envir= .rj.tmp)[idxs[1L]:idxs[2L]]
	x.names[rowIdxs]
}


.getDataLevelValues <- function(x, max = 1000) {
	if (is.factor(x)) {
		values <- levels(x)
		if (any(is.na(x))) {
			values <- c(values, NA)
		}
	}
	else {
		values <- sort(unique(x), na.last= TRUE)
	}
	if (length(values) > max) {
		return (NULL)
	}
	return (values)
}

.getDataIntervalValues <- function(x) {
	values <- c(min(x, na.rm= TRUE), max(x, na.rm= TRUE), if (any(is.na(x))) 1L else 0L)
	return (values)
}

.searchDataTextValues <- function(x, type, pattern, max = 1000) {
	if (type == 0L) { # Eclipse
		values <- grep(pattern, x, ignore.case= TRUE, value= TRUE)
	}
	else if (type == 1L) {
		values <- grep(pattern, x, ignore.case= FALSE, value= TRUE)
	}
	else if (type == 2L) {
		values <- match(pattern, x)
		if (!is.na(values)) {
			values <- pattern
		}
		else {
			values <- character(0)
		}
	}
	else {
		stop("Illegal argument: type")
	}
	if (length(values) > max) {
		return (NULL)
	}
	return (values)
}


.formatInfo.maxLength <- 2L^20L
.formatInfo.sampleLength <- 2L^19L

.getFormatInfo <- function(x) {
	if (length(x) > .formatInfo.maxLength) {
		x <- sample(x, .formatInfo.sampleLength)
	}
	return (format.info(x))
}
