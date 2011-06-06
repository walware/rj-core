
.pkg <- new.env()

.onLoad <- function(libname, pkgname) {
	library.dynam("rj.gd", pkgname, libname)
	assign("init", FALSE, env= .pkg)
	assign("cp", as.character(system.file("java", "gd.jar", package="rj.gd")), env= .pkg)
	if (!file.exists(.pkg$cp)) {
		error("gd.jar file for classpath is missing")
	}
}

initLib <- function() {
	if (.pkg$init) {
		return(invisible())
	}
	.Call("RJgd_initLib", .pkg$cp, PACKAGE="rj.gd")
	assign("init", FALSE, env= .pkg)
	return(invisible())
}
