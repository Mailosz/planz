package pl.mo.planz;

public class StringUtils {
    public static String escapeQuotes(String s) {
        if (s == null) return null;
        return s.replace("\"", "\\\"");
    }

    public static String escapeHTML(String s) {
        if (s == null) return null;
        return s.replace("<", "&lt;");
    }

    public static boolean isNullOrEmpty(String s) {
        if (s == null) return true;
        if (s.equals("")) {
            return true;
        }

        return false;
    }
}
