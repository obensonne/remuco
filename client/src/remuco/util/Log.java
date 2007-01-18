package remuco.util;

public class Log {

    private static ILogPrinter out = new ConsoleLogger();

    private static final int PACKSEP = ".".charAt(0);

    public static void l(Object o, String msg) {
        out.print(toClassName(o) + ": " + msg);
    }

    public static void l(String msg) {
        out.print(msg);
    }

    public static void ln() {
        out.println();
    }

    public static void ln(byte[] ba) {
        for (int i = 0; i < ba.length; i++) {
            out.print(Integer.toHexString(ba[i]) + " ");
        }
        out.println();
    }

    public static void ln(Object o, String msg) {
        out.println(toClassName(o) + ": " + msg);
    }

    public static void ln(String msg) {
        out.println(msg);
    }

    public static void setOut(ILogPrinter out) {
        if (out == null)
            throw new NullPointerException("null logger not supported");
        Log.out = out;
    }

    private static String toClassName(Object o) {
        String s;
        int i, len;
        StringBuffer sb = new StringBuffer();
        if (o == null) {
            s = "NULL";
        } else if (o instanceof Class) {
            s = ((Class) o).getName();
        } else {
            s = o.getClass().getName();
        }
        i = s.lastIndexOf(PACKSEP) + 1;
        if (i > 0) {
            sb.append(s.substring(i));
        }

        len = 30;
        i = sb.length();
        while (i < len) {
            sb.append(" ");
            i++;
        }

        return sb.toString();
    }

}
