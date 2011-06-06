
.rj.getGDSize <- function(devNr = dev.cur()) {
	par <- rep(0,6)
	l <- .C("javaGDgetSize", as.integer(devNr-1), as.double(par), PACKAGE= "rj.gd")
	par <- l[[2]]
	if (par[6]==0) list() else list(
				x= par[1],
				y= par[2],
				width= (par[3]-par[1]),
				height= (par[4]-par[2]),
				dpiX= par[5],
				dpiY= par[6])
}

.rj.copyGD <- function(devNr = dev.cur(),
		device = pdf, width.diff = 0, height.diff = 0, ...) {
	s <- .rj.getGDSize(devNr)
	pd <- dev.cur()
	dev.set(devNr)
	dev.copy(device, width= par()$din[1]/0.72+width.diff, height= par()$din[2]/0.72+height.diff, ... )
	dev.off()
	dev.set(pd)
	invisible(devNr)
}

.rj.getGDVersion <- function() {
	initLib()
	
	v <- .C("javaGDversion", as.integer(rep(0,4)), PACKAGE= "rj.gd")[[1]]
	list(major= v[1]%/%65536, minor= (v[1]%/%256)%%256, patch= (v[1]%%256), numeric= v[1])
}

.rj.setGDDisplayParameters <- function(dpiX = 100, dpiY = 100, aspect = 1) {
	invisible(.C("javaGDsetDisplayParam", as.double(c(dpiX, dpiY, aspect)), PACKAGE= "rj.gd"))
}
