package remuco.util;

import javax.microedition.lcdui.Form;


/**
 * A {@link remuco.util.ILogPrinter} implementation which prints out log
 * messages to STDOUT and a Form. This is useful for inspecting what happens
 * on a mobile device where STDOUT cannot be inspected.
 * 
 * @author Christian Buennig
 * 
 */
public class FormLogPrinter implements ILogPrinter {

    private Form f;

    public FormLogPrinter(Form f) {
        this.f = f;
    }

    private static int MAX_LOG_ELEMENTS = 70;

    public void print(String s) {
        checkFormSize();
        System.out.print(s);
        f.append(s);
    }

    public void println(String s) {
        checkFormSize();
        System.out.println(s);
        f.append(s + "\n");
    }

    public void println() {
        checkFormSize();
        System.out.println();
        f.append("\n");
    }

    private void checkFormSize() {
        if (f.size() >= MAX_LOG_ELEMENTS) {
            for (int i = 0; i < 10; i++) {
                f.delete(0);
            }
        }
    }

}
