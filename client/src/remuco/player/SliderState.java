package remuco.player;

public final class SliderState {

	private int length;

	private int position;

	public SliderState() {
		length = -1;
		position = 0;
	}

	public int getLength() {
		return length;
	}

	public int getPosition() {
		return position;
	}

	public void setLength(int length) {
		this.length = length;
	}

	public void setPosition(int progress) {
		this.position = progress;
	}

}
