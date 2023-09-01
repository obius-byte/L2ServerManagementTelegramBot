package info.mmo_dev;

import java.util.Collections;

public class Utils {

    public static String column(String text, boolean first, int length) {
        int textLen = text.length();

        if ( first )
            return text + String.join("", Collections.nCopies(length - textLen, " ")) + " |";

        return String.join("", Collections.nCopies(( length / 2 ) - textLen, " ")) + text + " |";
    }

    public static String column(String text) {
        return column(text, false, 26);
    }
}
