JavaGD <- function(name="JavaGD", width=400, height=300, ps=12) {
  # if rJava is loaded, we use it to initialize the class path,
  # because it knows how to merge class path into a running VM - we don't
  if ("rJava" %in% .packages() && nchar(.javaGD.get.class.path())>0) {
    .jinit(.javaGD.get.class.path())
    .javaGD.set.class.path("")
  }
  invisible(.C("newJavaGD",as.character(name),as.numeric(width),as.numeric(height),as.numeric(ps),PACKAGE="JavaGD"))
}

.getJavaGDObject <- function(devNr) {
    a<-.Call("javaGDobjectCall",as.integer(devNr-1),PACKAGE="JavaGD")
    if (!is.null(a)) {
    	if (exists(".jmkref")) a <- .jmkref(a)
	else stop(".jmkref is not available. Please use rJava 0.3 or higher.")
    }
}
