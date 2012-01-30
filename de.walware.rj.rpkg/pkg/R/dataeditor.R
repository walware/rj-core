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

