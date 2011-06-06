\name{rj.gd}
\alias{.rj.GD}
\title{
  Create a new RJ graphics device
}
\description{
  \code{.rj.GD} initializes a new graphics device. If supported, the new device is
  shown in a new window/page in the client application.
}
\usage{
.rj.GD(name="rj.gd", width=400, height=300, ps=12)
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
.rj.GD()
}
}
\keyword{device}
