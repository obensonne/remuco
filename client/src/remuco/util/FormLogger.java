package remuco.util;

import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;

/**
 * A {@link remuco.util.ILogPrinter} implementation which prints out log
 * messages to a {@link Form}. This is useful for inspecting what happens on a
 * mobile device where STDOUT cannot be inspected.
 * 
 * @author Christian Buennig
 * 
 */
public final class FormLogger implements ILogPrinter {

	private final Form f;

	private int insertPos = 0;

	public FormLogger(Form f) {
		this.f = f;
	}

	private static int MAX_LOG_ELEMENTS = 70;

	public void print(String s) {
		checkFormSize();
		f.insert(insertPos, new StringItem(null, s));
		insertPos++;
		// f.append(s);
	}

	public void println(String s) {
		checkFormSize();
		f.insert(insertPos, new StringItem(null, s + "\n"));
		insertPos = 0;
		// f.append(s + "\n");
	}

	public void println() {
		checkFormSize();
		f.insert(insertPos, new StringItem(null, "\n"));
		insertPos = 0;
		// f.append("\n");
	}

	private void checkFormSize() {
		int size = f.size();
		if (size >= MAX_LOG_ELEMENTS) {
			for (int i = size - 1; i > size - 11; i--) {
				f.delete(i);
			}
		}
	}
}
