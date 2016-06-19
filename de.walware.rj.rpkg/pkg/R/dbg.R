## RJ dbg

.breakpoint <- function() {
	if (tracingState(FALSE)) {
		on.exit(tracingState(TRUE))
	}
	
	answer <- .Call("Re_ExecJCommand", "dbg:checkBreakpoint", sys.call(), 0L,
			PACKAGE= "(embedding)" )
	
	if (!is.null(answer)) {
		envir <- sys.frame(-1)
		eval(expr= answer, envir= envir)
	}
}

.checkError <- function() {
	if (tracingState(FALSE)) {
		on.exit(tracingState(TRUE))
	}
	if ("rj" %in% loadedNamespaces()) {
		answer <- .Call("Re_ExecJCommand", "dbg:checkEB", NULL, 0L,
				PACKAGE= "(embedding)" )
		
		if (!is.null(answer)) {
			envir <- sys.frame(-1)
			eval(expr= answer, envir= envir)
		}
	}
}
