## RJ gr(aphics)

.gr.convertUser2Dev <- function(xy, devId) {
	currentDevId <- dev.cur()
	if (devId != currentDevId) {
		on.exit(expr= dev.set(which= currentDevId), add= TRUE)
		dev.set(which= devId)
	}
	xy[1] <- grconvertX(xy[1], from= "user", to= "dev")
	xy[2] <- grconvertY(xy[2], from= "user", to= "dev")
	return (xy)
}

.gr.convertDev2User <- function(xy, devId) {
	currentDevId <- dev.cur()
	if (devId != currentDevId) {
		on.exit(expr= dev.set(which= currentDevId), add= TRUE)
		dev.set(which= devId)
	}
	xy[1] <- grconvertX(xy[1], from= "dev", to= "user")
	xy[2] <- grconvertY(xy[2], from= "dev", to= "user")
	return (xy)
}

