package net.databinder.components;

import java.util.Collections;
import java.util.List;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;


/**
 * Similar to the Link created by ListView.moveDownLink(), but can be overridden
 * for saving changes to persistent storage.
 */
public class MoveDownLink extends Link {
	private ListItem item;
	
	/**
	 * @param id Wicket id of move link
	 * @param item associated list item
	 */
	public MoveDownLink(String id, ListItem item) {
		super(id);
		this.item = item;
	}

	/** @return parent of ListItem casted as ListView. */
	protected ListView getListView() {
		return (ListView) item.getParent();
	}

	/** Disable link as appropriate.  */
	protected void onBeforeRender()
	{
		setAutoEnable(false);
		List list = getListView().getList();
		if (list.indexOf(item.getModelObject()) == (list.size() - 1))
			setEnabled(false);
	}

	/** 
	 * Moves linked item within its list. Override to save changes after calling
	 * this super implementation.
	 */ 
	public void onClick()
	{
		List list = getListView().getList();
		final int index = list.indexOf(item.getModelObject());
		if (index != -1)
		{
			getListView().modelChanging();
			Collections.swap(list, index, index + 1);
			getListView().modelChanged();
		}
	}
}