package com.cenqua.shaj.log;

import java.io.PrintStream;

public class PrintStreamLog implements Log {

    private final PrintStream out;
    private final boolean isDebug;

    public PrintStreamLog(PrintStream out, boolean isDebug) {
        this.out = out;
        this.isDebug = isDebug;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public void error(String msg) {
        out.println("OSAUTH-ERROR: " + msg);
    }

    public void error(String msg, Throwable e) {
        out.println("OSAUTH-ERROR: " + msg);
        e.printStackTrace(out);
    }

    public void debug(String msg) {
        if (isDebug) {
            out.println("OSAUTH-DEBUG: " + msg);
        }
    }

}
