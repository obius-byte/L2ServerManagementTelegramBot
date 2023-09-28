package info.mmo_dev;

import java.io.*;

public class Utils {

    public static String column(String text, boolean first, int length) {
        int textLen = text.length();

        if (textLen > length) {
            int endIndex = length - (first ? 3 : 4);
            text = text.substring(0, endIndex) + "...";
        }

        String format = first
                ? "| %" + length + "s |"
                : "%" + length + "s |";

        return String.format(format, text);
    }

    public static String getStackTrace(final Throwable e) {
        final StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw, true));

        return sw.toString()
                .replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;");
    }
}
