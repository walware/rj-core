## StatET workbench

#' Opens a file in an editor in StatET
#' @param filename name of file to open
#' 
#' @export
statet_openInEditor <- function(filename) {
	if (missing(filename) || !is.character(filename) || length(filename) != 1) {
		stop("Illegal argument: filename")
	}
	.rj_ui.execCommand("common/showFile", list(
					filename = .jnew("java/lang/String", filename) ),
			TRUE)
	return (invisible())
}

#' Opens a new graphic device in the graphic view of StatET.
#' 
#' @param width initial width of the graphic in pixel
#' @param height initial width of the graphic in pixel
#' @param ps initial font size in points
#' @export
statet_gd <- .rj_gd.new

#' Asks the user to choose a file using the StatET GUI.
#' 
#' @param new if the choosen file can be new (does not yet exits)
#' @export
statet_chooseFile <- function(new = FALSE) {
	if (!is.logical(new) || length(new) != 1) {
		stop("Illegal argument: new")
	}
	answer <- .rj_ui.execCommand("common/chooseFile", list(
					newResource = .jfield("java/lang/Boolean", name = as.character(new), convert = FALSE) ),
			TRUE)
	if (is.jnull(answer)) { # operation cancelled
		return (invisible())
	}
	filename <- .getAnswerValue(answer, "filename")
	return (.jstrVal(filename))
}
