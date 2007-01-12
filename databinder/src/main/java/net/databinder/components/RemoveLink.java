package net.databinder.components;

import wicket.markup.html.link.Link;
import wicket.markup.html.list.ListItem;
import wicket.markup.html.list.ListView;


/**
 * Similar to the Link created by ListView.removeLink(), but can be overridden
 * for saving changes to persistent storage.
 */
public class RemoveLink extends Link {
	private ListItem item;
	/**
	 * @param id Wicket id of removal link
	 * @param item associated list item
	 */
	public RemoveLink(String id, ListItem item) {
		super(id);
		this.item = item;
	}
	/** @return parent of ListItem casted as ListView. */
	protected ListView getListView() {
		return (ListView) item.getParent();
	}
	/** 
	 * Removes linked item from its list. Override to save changes after calling
	 * this super implementation.
	 */ 
	public void onClick()
	{
		getListView().modelChanging();
		getListView().getList().remove(item.getModelObject());
		getListView().modelChanged();
	}
}