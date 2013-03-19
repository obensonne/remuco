package remuco.client.android;

import remuco.client.common.data.ItemList;

public interface RequestHandlerCallback {
    public void setList(ItemList l);

    public void getList();

    public void clearList();
}
