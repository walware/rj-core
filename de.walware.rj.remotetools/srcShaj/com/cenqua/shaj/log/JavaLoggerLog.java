package com.cenqua.shaj.log;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaLoggerLog implements Log {

    private final Logger logger;

    public JavaLoggerLog(final Logger logger) {
        this.logger = logger;
    }

    @Override
	public boolean isDebug() {
        return logger.isLoggable(Level.FINE);
    }

    @Override
	public void error(final String msg) {
        logger.severe(msg);
    }

    @Override
	public void error(final String msg, final Throwable e) {
        logger.log(Level.SEVERE, msg, e);
    }

    @Override
	public void debug(final String msg) {
        logger.fine(msg);
    }
}
