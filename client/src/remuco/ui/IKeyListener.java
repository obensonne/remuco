package remuco.ui;

import javax.microedition.lcdui.Canvas;

/**
 * Interface for classens listening for key events raised by a {@link Canvas}
 * based screen.
 * 
 * @author Oben Sonne
 * 
 */
public interface IKeyListener {

	public void keyPressed(int key);

	public void keyReleased(int key);

}
