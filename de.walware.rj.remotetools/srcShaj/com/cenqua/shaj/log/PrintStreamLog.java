package com.cenqua.shaj.log;

import java.io.PrintStream;


public class PrintStreamLog implements Log {

    private final PrintStream out;
    private final boolean isDebug;

    public PrintStreamLog(final PrintStream out, final boolean isDebug) {
        this.out = out;
        this.isDebug = isDebug;
    }

    @Override
	public boolean isDebug() {
        return isDebug;
    }

    @Override
	public void error(final String msg) {
        out.println("OSAUTH-ERROR: " + msg);
    }

    @Override
	public void error(final String msg, final Throwable e) {
        out.println("OSAUTH-ERROR: " + msg);
        e.printStackTrace(out);
    }

    @Override
	public void debug(final String msg) {
        if (isDebug) {
            out.println("OSAUTH-DEBUG: " + msg);
        }
    }

}
