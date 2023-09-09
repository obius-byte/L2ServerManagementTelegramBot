package info.mmo_dev;

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
}
