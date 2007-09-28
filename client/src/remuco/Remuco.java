package remuco;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import remuco.ui.UI;
import remuco.util.FormLogger;
import remuco.util.Log;

/**
 * MIDlet of the Remuco client.
 * <p>
 * <h1>Some general notes about the client source</h1>
 * A lot of classes are declared as <code>final</code>. This has no software
 * design specific but performance reason. There a lot of discussion in the net
 * about using final classes (and so methods) for performance reaons. To draw a
 * conclusion of these discussions it is acutally bad style to use final classes
 * to improve performance since it may confuse other developers and since the
 * performance benefit is probably very small (due to nowadays very
 * sophisticated JIT compilers). However, in my uderstanding the last point is
 * valid for Java code running in JREs but it is unclear if it applys to a
 * J2ME-RE. So final classes may still improve performance. This is an
 * assumption. Once it is proved that this assumption is wrong, I will undo all
 * the performance related final declaration of classes.
 * <h1>Emulator Code</h1>
 * Some code is only used to run the client inside the WTK emulator with a
 * client integrated player which behaves like a remote player. This is useful
 * for testing the basic functionality of the client and to check themes. All
 * corresponding code is either tagged with <code>@emulator</code> in its JavaDoc or is located inside an if-statement block
 *           using the condition {@link #EMULATION}.
 * 
 * @author Christian Buennig
 * 
 */
public final class Remuco extends MIDlet implements CommandListener {

	/**
	 * @emulator If <code>true</code>, the client runs inside the WTK
	 *           emulator. This field should get set automatically to the
	 *           correct value by the Ant build script.
	 */
	public static final boolean EMULATION = false;

	private final Alert alertLoadConfig;

	private final Alert alertSaveConfig;

	private final boolean configSuccessfullyLoaded;

	private final Display display;

	private Displayable displayableAfterLog;

	private final Form logForm;

	private boolean startAppFirstTime = true;

	private final UI ui;

	public Remuco() {

		// set up logging

		logForm = new Form("Log");
		logForm.addCommand(UI.CMD_BACK);
		logForm.setCommandListener(this);
		if (EMULATION) {
			Log.ln("RUNING IN EMULATION MODE ..");
			logForm.append("Emulation -> logging goes to standard out!");
		} else {
			Log.setOut(new FormLogger(logForm));
		}

		// load the configuration

		configSuccessfullyLoaded = Config.load();

		Config.setApplicationProperties(this);

		// set up some displayables

		alertLoadConfig = new Alert("Error");
		alertLoadConfig.setString("Erros while loading configuration."
				+ " Please inspect the log for details!");
		alertLoadConfig.setType(AlertType.ERROR);
		alertLoadConfig.setTimeout(Alert.FOREVER);
		alertLoadConfig.setCommandListener(this);

		alertSaveConfig = new Alert("Error");
		alertSaveConfig.setString("Erros while saving configuration."
				+ " Please inspect the log for details!");
		alertSaveConfig.setType(AlertType.ERROR);
		alertSaveConfig.setTimeout(Alert.FOREVER);
		alertSaveConfig.setCommandListener(this);

		// get the display

		display = Display.getDisplay(this);

		// set up the start screen

		ui = new UI(this, display);

	}

	public void commandAction(Command c, Displayable d) {

		if (c == UI.CMD_SHOW_LOG) {

			displayableAfterLog = d;

			display.setCurrent(logForm);

		} else if (c == UI.CMD_BACK && d == logForm) {

			// display the displayable shown before the log

			if (displayableAfterLog != null)
				display.setCurrent(displayableAfterLog);
			else
				Log.ln("A BUG !! Do not know what to show after Log !?");

		} else if (c == UI.CMD_EXIT) {

			if (!Config.save())
				display.setCurrent(alertSaveConfig);
			else
				notifyDestroyed();

		} else if (c == Alert.DISMISS_COMMAND && d == alertLoadConfig) {

			// continue start up

			ui.go();

		} else if (c == Alert.DISMISS_COMMAND && d == alertSaveConfig) {

			// continue shut down

			notifyDestroyed();

		} else {

			Log.ln("[ROOT] unexpected command: " + c.getLabel());

		}

	}

	protected void destroyApp(boolean unconditional)
			throws MIDletStateChangeException {

		Config.save();

	}

	protected void pauseApp() {

		Log.debug("pauseApp");

	}

	protected void startApp() throws MIDletStateChangeException {

		if (startAppFirstTime) {

			startAppFirstTime = false;

			if (!configSuccessfullyLoaded) {
				display.setCurrent(alertLoadConfig);
				return;
			}

			ui.go();

			Log.debug("startApp (first time)");

		} else {

			Log.debug("startApp (again)");

		}

	}

}
