## RJ graphic device

#' Initializes the RJ graphic device.
#' 
#' @param as.default set it as default graphic device
#' @export
.rj_gd.init <- function(as.default = TRUE) {
	.rj.initRJava()
	.rj.checkRJavaInit()
	Sys.setenv("JAVAGD_CLASS_NAME"="de/walware/rj/gd/JavaGD")
	library("JavaGD")
	if (as.default) {
		options(device=".rj_gd.new")
	}
	return (invisible(TRUE))
}

#' Opens a new RJ graphic device.
#' 
#' @param width initial width of the graphic in pixel
#' @param height initial width of the graphic in pixel
#' @param ps initial font size in points
#' @export
.rj_gd.new <- function(width = 400, height = 300, ps = 12) {
	.rj_gd.init(FALSE) # TODO add flag to check if initialized
	dev <- JavaGD(width = width, height = height, ps = ps)
	id <- as.integer(dev.cur())
	if (is.null(dev) || id < 2L
			|| Sys.getenv("JAVAGD_CLASS_NAME") != "de/walware/rj/gd/JavaGD") {
		return (invisible(dev))
	}
	.jcall(.rj.originals$rjInstance, "V", "initLastGraphic", (id - 1L), .jnull(class="java/lang/String"))
	return (invisible(dev))
}

