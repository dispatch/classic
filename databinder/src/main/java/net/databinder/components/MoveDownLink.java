/*
 * Databinder: a simple bridge from Wicket to Hibernate
 * Copyright (C) 2006  Nathan Hamblen nathan@technically.us

 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
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
		super.onBeforeRender();
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