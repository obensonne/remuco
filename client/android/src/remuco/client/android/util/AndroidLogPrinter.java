package remuco.client.android.util;

import android.util.Log;
import remuco.client.common.util.ILogPrinter;

public class AndroidLogPrinter implements ILogPrinter {

	/*
	 * This log printer uses Android's logging framework which is good when
	 * debugging. When running on real devices, it might be better to print
	 * logging output to a screen visible to the user.
	 */

	@Override
	public void println(String s) {
		Log.i("Remuco", s);
	}

}
