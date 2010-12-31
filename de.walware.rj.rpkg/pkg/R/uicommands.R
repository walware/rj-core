## RJ UI

#' Sends an UI command to the GUI which executes the command
#' if it is supported
#' 
#' @param commandId the id of the UI command
#' @param args an optional list with arguments (meaning depends on UI command)
#' @param wait if R must wait until the command is executed
#'     (necessary if you need a return value)
#' @returnType char
.rj_ui.execCommand <- function(commandId, args, wait = TRUE) {
	if (missing(commandId)) {
		stop("Missing param 'commandId'")
	}
	.rj.checkInit()
	if (missing(args)) {
		jargs <- .jnull("java/util/Map")
	}
	else if (is.list(args)) {
		argNames <- names(args)
		if (!is.character(argNames) || any(is.na(argNames))) {
			stop("Arguments must be named")
		}
		jargs <- .jnew("java/util/HashMap", length(args))
		jargs <- .jcast(jargs, new.class = "java/util/Map")
		for (i in seq(along = args)) {
			key <- .jnew("java/lang/String", argNames[i])
			.jcall(jargs, "Ljava/lang/Object;", "put", .jcast(key), .jcast(args[[i]]))
		}
	}
	else {
		stop("Illegal argument: args")
	}
	.jcall(.rj.jInstance, "Ljava/util/Map;", "execUICommand", commandId, jargs, TRUE)
}

.getAnswerValue <- function(answer, key) {
	key <- .jnew("java/lang/String", key)
	.jcall(answer, "Ljava/lang/Object;", "get", .jcast(key))
}

.rj_ui.loadHistory <- function(filename = ".Rhistory") {
	if (!is.character(filename) || length(filename) != 1) {
		stop("Illegal argument: filename")
	}
	.rj_ui.execCommand("loadHistory", list(
					filename = .jnew("java/lang/String", filename)), TRUE)
	return (invisible())
}
.rj_ui.saveHistory <- function(filename = ".Rhistory") {
	if (!is.character(filename) || length(filename) != 1) {
		stop("Illegal argument: filename")
	}
	.rj_ui.execCommand("saveHistory", list(
					filename = .jnew("java/lang/String", filename) ), TRUE)
	return (invisible())
}
.rj_ui.addtoHistory <- function(line) {
	if (missing(line) || !is.character(line) || length(line) != 1) {
		stop("Illegal argument: line")
	}
	.rj_ui.execCommand("addtoHistory", list(
					line = .jnew("java/lang/String", line) ), FALSE)
	return (invisible())
}

