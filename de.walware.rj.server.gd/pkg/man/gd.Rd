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
.rj.GD(name = "rj.gd", width = 7, height = 7, size.unit = "in",
    xpinch = NA, ypinch = NA, canvas = "white",
    pointsize = 12, gamma = 1.0)
}
\arguments{
  \item{name}{Name of the device (unused)}
  \item{width, height}{Initial size (number) of the graphic window, for horizontal and vertical
    orientation.
    The unit of the values are inch, if not otherwise specified by the argument \code{size.unit}.
    The default size is 7 x 7 inch.}
  \item{size.unit}{The unit (known character) of the size values in \code{width} and \code{height}.
    Supported units are inch (\code{"in"}) and pixels (\code{"px"}).  The default unit is inch.}
  \item{xpinch, ypinch}{The resolution of the device (number), pixel per inch (ppi), for
    horizontal and vertical orientation.
    By default the values are specified by the client application.}
  \item{canvas}{The color (R color) of the canvas which is visible when the background color is 
    transparent.
    The default color is \code{"white"}.}
  \item{pointsize}{The default pointsize (number) of plotted text in big points (1/72 inch).
    Values are rounded to the nearest integer. The default text size is 12 pt.}
  \item{gamma}{Gamma correction factor as number (not the gamma value itself).
    Colors in R are defined for the color space sRGB.  If your monitor does not conform to sRGB, 
    you might be able to improve things by tweaking this parameter to apply additional gamma 
    correction to the RGB channels.  The default is no additional gamma correction (\code{1.0}).}
}
\value{
  Currently the return value is NULL.
}
\examples{
\dontrun{
.rj.GD()
}
}
\keyword{device}
