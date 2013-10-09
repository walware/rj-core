/* Copyright 2005 Cenqua Pty Ltd. All Rights Reserved. See LICENSE.TXT in the distribution. */
package com.cenqua.shaj.log;

import java.util.logging.Logger;

/**
 * Used by Shaj to log errors and debug information.
 *
 * <p>Setting the system propeerty "shaj.debug" will force Shaj to output error and debug logging
 * to System.err. Shaj is reasonably verbose in this mode, which is useful for debugging your programs.
 * </p>
 *
 * <p>
 * By default, Shaj will attempt delegate logging to (in order):</p>
 * <ul>
 *   <li> Jakarta Commons Logging
 *   <li> log4j (using the Logger class, not the deprecated Category class)
 *   <li> java.util.logging
 *   <li> System.err
 * </ul>
 *
 * <p>You can override this by setting your own default logger</p>
 */
public interface Log
{

    boolean isDebug();
    void error(String msg);
    void error(String msg, Throwable e);
    void debug(String msg);

    public class Factory
    {
        private static Log INSTANCE = null;

        private static Log makeInstance() {
            try {
                final boolean isdebug = System.getProperty("shaj.debug") != null;
                if (isdebug) {
                    return new PrintStreamLog(System.err, isdebug);
                }
            } catch (final Exception e) {
                // nop (security exception, etc)
            }

            final Class logClass = Log.class;

            try {
                if (findClass("java.util.logging.Logger")) {
                    return new JavaLoggerLog(Logger.getLogger(logClass.getName()));
                }
            } catch (final Exception e) {
                // nop
            }

            return new PrintStreamLog(System.err, false);
        }

        private static boolean findClass(final String name) {
            try {
                return Class.forName(name) != null;
            } catch (final Exception e) {
                return false;
            }
        }

        public static Log getInstance()
        {
            if (INSTANCE == null) {
                INSTANCE = makeInstance();
            }
            return INSTANCE;
        }

        public static void setInstance(final Log instance) {
            Factory.INSTANCE = instance;
        }

    }
    
}
