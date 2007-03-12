package remuco.ui.canvas;

public class Point {

	public int x, y;

	public Point() {
		x = 0;
		y = 0;
	}

	public Point(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public Point(Point p) {
		x = p.x;
		y = p.y;
	}

	public void set(Point p) {
		x = p.x;
		y = p.y;
	}
}