package remuco.player;

/**
 * Descriptor for an item related action.
 * 
 * @author Oben Sonne
 * 
 */
public class ItemAction extends AbstractAction {

	public final boolean multiple;

	public ItemAction(int id, String label, boolean multiple, String desc) {

		super(id, label, desc);

		this.multiple = multiple;
	}

	public boolean isItemAction() {
		return true;
	}

	public boolean isListAction() {
		return false;
	}

}
