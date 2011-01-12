## RJ UI

#' Sends an UI command to the GUI which executes the command
#' if it is supported
#' 
#' @param commandId the id of the UI command
#' @param args an optional list with arguments (meaning depends on UI command)
#' @param wait if R must wait until the command is executed
#'     (necessary if you need a return value)
#' @returnType char
.rj_ui.execCommand <- function(commandId, args = list(), wait = TRUE) {
	if (missing(commandId)) {
		stop("Missing param: commandId")
	}
	if (!is.list(args)) {
		stop("Illegal argument: args must be a list")
	}
	if (length(args) > 0) {
		args.names <- names(args)
		if (is.null(args.names) || any(is.na(args.names))) {
			stop("Illegal argument: args must be named")
		}
	}
	
	options <- 0L
	if (wait) {
		options <- options + 1L
	}
	.Call("Re_ExecJCommand", paste("ui", commandId, sep=":"), args, options)
}


.rj_ui.loadHistory <- function(filename = ".Rhistory") {
	if (!is.character(filename) || length(filename) != 1) {
		stop("Illegal argument: filename")
	}
	.rj_ui.execCommand("loadHistory", list(
					filename = filename ), TRUE)
	return (invisible())
}
.rj_ui.saveHistory <- function(filename = ".Rhistory") {
	if (!is.character(filename) || length(filename) != 1) {
		stop("Illegal argument: filename")
	}
	.rj_ui.execCommand("saveHistory", list(
					filename = filename ), TRUE)
	return (invisible())
}
.rj_ui.addtoHistory <- function(line) {
	if (missing(line) || !is.character(line) || length(line) != 1) {
		stop("Illegal argument: line")
	}
	.rj_ui.execCommand("addtoHistory", list(
					line = line ), FALSE)
	return (invisible())
}

