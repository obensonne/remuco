package remuco.ui;

import remuco.player.ActionParam;
import remuco.ui.screens.ItemlistScreen;

public interface IItemListController {

	public void ilcBack(ItemlistScreen ils);
	
	public void ilcRoot(ItemlistScreen ils);
	
	public void ilcAction(ItemlistScreen ils, ActionParam a);
	
	public void ilcShowNested(ItemlistScreen ils, String path[]);
}
