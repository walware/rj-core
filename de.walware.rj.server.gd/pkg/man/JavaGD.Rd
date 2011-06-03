\name{JavaGD}
\alias{JavaGD}
\alias{javaGD}
\title{
  Create a new Java graphics device
}
\description{
  \code{JavaGD} initializes a new Java grapgics device and creates a new window.
}
\usage{
JavaGD(name="JavaGD", width=400, height=300, ps=12)
}
\arguments{
  \item{name}{Name of the device}
  \item{width}{Initial width of the window (usually in pixels, but the
    actual interpretation depends on the Java implementation)}
  \item{height}{Initial height of the window}
  \item{ps}{Initial point size}
}
\value{
  Currently the return value is implementation dependent, mostly just
  the return code as returned by the low-level C function
}
\examples{
\dontrun{
JavaGD()
}
}
\keyword{device}
