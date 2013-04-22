## RJ graphic device

rj.GD <- function(name = "rj.gd", width = 7, height = 7, size.unit = "in",
		xpinch = NA, ypinch = NA, canvas = "white",
		pointsize = 12, gamma = 1.0) {
	initLib()
	
	if (!size.unit %in% c("in", "px")) {
		error(paste("Illegal argument: unsupported unit", size.unit))
	}
	size.unit <- switch(size.unit, "px" = 1L, 0L)
	
	invisible(.Call("newJavaGD", name,
					width, height, size.unit,
					xpinch, ypinch, canvas,
					pointsize, gamma,
					PACKAGE= "rj.gd" ))
}

.rj.getGDJavaObject <- function(devNr) {
	a <- .Call("javaGDobjectCall", devNr - 1L, PACKAGE= "rj.gd")
	if (!is.null(a)) {
		if (exists(".jmkref")) a <- .jmkref(a)
		else stop(".jmkref is not available. Please use rJava 0.3 or higher.")
	}
}
