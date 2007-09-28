package remuco.util;


public final class ConsoleLogger implements ILogPrinter {

    public void print(String s) {
        System.out.print(s);
    }

    public void println(String s) {
        System.out.println(s);
    }

    public void println() {
        System.out.println();
    }

}
