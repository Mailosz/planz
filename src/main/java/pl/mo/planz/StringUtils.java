package pl.mo.planz;

public class StringUtils {
    public static String escapeQuotes(String s) {
        if (s == null) return null;
        return s.replace("\"", "\\\"");
    }
}
