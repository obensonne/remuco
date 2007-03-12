package remuco.ui.canvas;

import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class ScreenElement {

	private Image img;

	private int x, y, width, height;

	public ScreenElement(int x, int y, Image i) {
		this.x = x;
		this.y = y;
		this.width = i.getWidth();
		this.height = i.getHeight();
		this.img = i;
	}

	public void draw(Graphics g) {
		g.drawImage(img, x, y, Graphics.TOP | Graphics.LEFT);
	}

	public int getHeigth() {
		return height;
	}

	public Image getImage() {
		return img;
	}

	public int getNextX() {
		return x + width;
	}

	public int getNextY() {
		return y + height;
	}

	public int getWidth() {
		return width;
	}

	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public void setImage(Image i) {
		img = i;
		width = i.getWidth();
		height = i.getHeight();
	}

}
