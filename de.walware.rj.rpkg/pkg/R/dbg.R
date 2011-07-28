## RJ dbg

.breakpoint <- function() {
	if (tracingState(FALSE)) {
		on.exit(tracingState(TRUE))
	}
	
	answer <- .Call("Re_ExecJCommand", "dbg:checkBreakpoint", sys.call(), 0L)
	
	if (!is.null(answer)) {
		envir <- sys.frame(-1)
		eval(expr= answer, envir= envir)
	}
}
