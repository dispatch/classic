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

import wicket.markup.html.link.Link;
import wicket.markup.html.list.ListItem;
import wicket.markup.html.list.ListView;
import wicket.model.BoundCompoundPropertyModel;
import wicket.model.IModel;

/**
 * Simple ListVew subclass that wraps its item models in a BoundCompoundPropertyModel.
 * Useful for lists where the item components will be mapped through property expressions.
 * Also contains Link subclasses that can be used to move or remove list elements (which
 * could be nested in a standard ListView).
 * @author Nathan Hamblen
 */

public abstract class PropertyListView extends ListView {

	/**
	 * Construct with a model.
	 */
	public PropertyListView(String id, IModel model) {
		super(id, model);
	}
	
	/**
	 * Construct without model, assume bound externally.
	 * @param id
	 */
	public PropertyListView(String id) {
		super(id);
	}
	
	/**
	 * Construct with a "small," unmodeled List. The list can not be detached and will
	 * reside in the session, but is convenient for lists of a limited size.
	 */
	public PropertyListView(String id, List l) {
		super(id, l);
	}
	
	@Override
	protected IModel getListItemModel(IModel model, int index) {
		return new BoundCompoundPropertyModel(super.getListItemModel(model, index));
	}
	
	/**
	 * Similar to the Link created by ListView.moveDownLink(), but can be overridden
	 * for saving changes to persistent storage.
	 */
	public static class MoveDownLink extends Link {
		private ListItem item;
		
		/**
		 * @param id Wicket id of move link
		 * @param item associated list item
		 */
		public MoveDownLink (String id, ListItem item) {
			super(id);
			this.item = item;
		}

		/** @return parent of ListItem casted as ListView. */
		protected ListView getListView() {
			return (ListView) item.getParent();
		}

		/** Disable link as appropriate.  */
		protected void onBeginRequest()
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
	
	/**
	 * Similar to the Link created by ListView.moveUpLink(), but can be overridden
	 * for saving changes to persistent storage.
	 */
	public static class MoveUpLink extends Link {
		private ListItem item;
		
		/**
		 * @param id Wicket id of move link
		 * @param item associated list item
		 */
		public MoveUpLink (String id, ListItem item) {
			super(id);
			this.item = item;
		}

		/** @return parent of ListItem casted as ListView. */
		protected ListView getListView() {
			return (ListView) item.getParent();
		}

		/** Disable link as appropriate.  */
		protected void onBeginRequest()
		{
			setAutoEnable(false);
            if (getListView().getList().indexOf(item.getModelObject()) == 0)
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
				Collections.swap(list, index, index - 1);
				getListView().modelChanged();
			}
		}
	}
	
	/**
	 * Similar to the Link created by ListView.removeLink(), but can be overridden
	 * for saving changes to persistent storage.
	 */
	public static class RemoveLink extends Link {
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

}
