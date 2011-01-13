## StatET workbench

#' Opens a file in an editor in StatET
#' @param filename name of file to open
#' 
#' @export
statet.openInEditor <- function(filename) {
	if (missing(filename) || !is.character(filename) || length(filename) != 1) {
		stop("Illegal argument: filename")
	}
	.rj_ui.execCommand("common/showFile", list(
					filename= filename ), wait= TRUE)
	return (invisible())
}

#' Asks the user to choose a file using the StatET GUI.
#' 
#' @param new if the choosen file can be new (does not yet exits)
#' @export
statet.chooseFile <- function(new = FALSE) {
	if (!is.logical(new) || length(new) != 1) {
		stop("Illegal argument: new")
	}
	answer <- .rj_ui.execCommand("common/chooseFile", list(
					newResource= new ), wait= TRUE)
	if (is.null(answer)) { # operation cancelled
		return (invisible())
	}
	return (answer$filename)
}

#' Shows the Cmd History view in StatET
#' 
#' @export
statet.showHistory <- function() {
	.rj_ui.execCommand("common/showHistory", wait= FALSE)
	return (invisible())
}
