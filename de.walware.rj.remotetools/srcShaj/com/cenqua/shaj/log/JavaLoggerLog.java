package com.cenqua.shaj.log;

import java.util.logging.Level;
import java.util.logging.Logger;

public class JavaLoggerLog implements Log {

    private final Logger logger;

    public JavaLoggerLog(Logger logger) {
        this.logger = logger;
    }

    public boolean isDebug() {
        return logger.isLoggable(Level.FINE);
    }

    public void error(String msg) {
        logger.severe(msg);
    }

    public void error(String msg, Throwable e) {
        logger.log(Level.SEVERE, msg, e);
    }

    public void debug(String msg) {
        logger.fine(msg);
    }
}
