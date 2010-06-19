## StatET workbench

#' Opens a file in an editor in StatET
#' @param filename name of file to open
#' 
#' @export
statet_openInEditor <- function(filename) {
	if (missing(filename) || !is.character(filename) || length(filename) != 1) {
		stop("Illegal argument: filename")
	}
	.rj_ui.execCommand("common/showFile", list(
					filename = .jnew("java/lang/String", filename) ), TRUE)
	invisible()
}

#' Opens a new graphic device in the graphic view of StatET.
#' 
#' @param width initial width of the graphic in pixel
#' @param height initial width of the graphic in pixel
#' @param ps initial font size in points
#' @export
statet_gd <- .rj_gd.new

