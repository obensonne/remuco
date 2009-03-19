package remuco.player;

public abstract class AbstractAction {

	public final String desc;

	public final int id;

	public final String label;

	private String disabledReason;

	private boolean enabled = true;

	public AbstractAction(int id, String label, String desc) {

		this.id = id;
		this.label = label;
		this.desc = desc;
	}

	public void disbale(String reason) {
		enabled = false;
		disabledReason = reason;
	}

	public void enable() {
		enabled = true;
	}

	public String getDisabledReason() {
		return disabledReason;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public abstract boolean isItemAction();

	public abstract boolean isListAction();

}
