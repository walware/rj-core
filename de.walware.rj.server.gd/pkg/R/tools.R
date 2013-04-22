
.rj.copyGD <- function(devNr = dev.cur(),
		device = pdf, width.diff = 0, height.diff = 0, ...) {
	pd <- dev.cur()
	dev.set(devNr)
	dev.copy(device, width= par()$din[1]/0.72+width.diff, height= par()$din[2]/0.72+height.diff, ... )
	dev.off()
	dev.set(pd)
	invisible(devNr)
}

.rj.getGDVersion <- function() {
	initLib()
	
	v <- .Call("javaGDversion", PACKAGE= "rj.gd")
	list(major= v[1]%/%65536, minor= (v[1]%/%256)%%256, patch= (v[1]%%256), numeric= v[1])
}

.rj.setGDDisplayParameters <- function(dpiX = 100, dpiY = 100, aspect = 1) {
	invisible(.Call("javaGDsetDisplayParam", as.double(c(dpiX, dpiY, aspect)), PACKAGE= "rj.gd"))
}
