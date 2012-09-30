## data editor

.getDataVectorValues <- function(x, rowIdxs) {
	x <- x[rowIdxs, drop= FALSE]
	names(x) <- NULL
	x
}

.getDataVectorRowNames <- function(x, rowIdxs) {
	x.names <- names(x)
	if (is.null(x.names)) {
		rowIdxs
	} else {
		x.names[rowIdxs]
	}
}

.getDataMatrixValues <- function(x, rowIdxs, colIdxs) {
	x <- x[rowIdxs, colIdxs, drop= FALSE]
	rownames(x) <- NULL
	x
}

.getDataMatrixRowNames <- function(x, rowIdxs) {
	x.names <- rownames(x)
	if (is.null(x.names)) {
		rowIdxs
	} else {
		x.names[rowIdxs]
	}
}

.getDataFrameValues <- function(x, rowIdxs, colIdxs) {
	x <- x[rowIdxs, colIdxs, drop= FALSE]
	row.names(x) <- NULL
	x
}

.getDataFrameRowNames <- function(x, rowIdxs) {
	x.names <- row.names(x)
	if (is.null(x.names)) {
		rowIdxs
	} else {
		x.names[rowIdxs]
	}
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
