package remuco.player;

/**
 * Descriptor for a list related action.
 * 
 * @author Oben Sonne
 * 
 */
public class ListAction extends AbstractAction {

	public ListAction(int id, String label, String desc) {
		super(id, label, desc);
	}

	public boolean isItemAction() {
		return false;
	}

	public boolean isListAction() {
		return true;
	}


}
