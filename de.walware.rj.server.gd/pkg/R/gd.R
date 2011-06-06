## RJ graphic device

rj.GD <- function(name = "rj.gd", width = 400, height = 300, ps = 12) {
	initLib()
	invisible(.C("newJavaGD", as.character(name),
					as.numeric(width), as.numeric(height), as.numeric(ps),
					PACKAGE= "rj.gd" ))
}

.rj.getGDJavaObject <- function(devNr) {
	a <- .Call("javaGDobjectCall", as.integer(devNr-1), PACKAGE= "rj.gd")
	if (!is.null(a)) {
		if (exists(".jmkref")) a <- .jmkref(a)
		else stop(".jmkref is not available. Please use rJava 0.3 or higher.")
	}
}
