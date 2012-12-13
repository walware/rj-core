## StatET workbench

#' Opens a file in an editor in StatET
#' @param filename name of file to open
#' 
#' @export
openInEditor <- function(name, fileName, elementName, filename) {
	if (!missing(filename)) {
		fileName <- filename
	}
	if (missing(name) && missing(fileName) && missing(elementName)) {
		stop("Missing argument: name")
	}
	if (missing(fileName) && missing(elementName)) {
		# auto
		fileName <- NULL
		elementName <- NULL
		if (is.character(name) && length(name) == 1) {
			fileName <- name
		}
		else {
			elementName <- substitute(name)
		}
	}
	else if (!missing(elementName)) {
		fileName <- NULL
		elementName # test, if it refers to an existing element
		elementName <- substitute(elementName)
	}
	else if (!missing(fileName)) {
		elementName <- NULL
	}
	
	if (!is.null(fileName)) {
		if (!is.character(fileName) || length(fileName) != 1) {
			stop("Illegal argument: fileName")
		}
		.rj_ui.execCommand("common/showFile", list(
						filename= fileName ), wait= TRUE)
	}
	else {
		elementName <- deparse(elementName, backtick= TRUE, control= c(),
				width.cutoff= 500, nlines= 2L)
		if (length(elementName) != 1) {
			stop("Illegal argument: elementName")
		}
		.rj_ui.execCommand("showElement", list(
						elementName= elementName ), wait= TRUE)
	}
	return (invisible())
}

statet.openInEditor <- openInEditor

#' Opens the package manager in StatET
#' 
#' @export
openPackageManager <- function() {
	.rj_ui.execCommand("r/openPackageManager", wait= FALSE)
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
