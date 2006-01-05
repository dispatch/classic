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

import java.util.List;

import wicket.markup.html.list.ListView;
import wicket.model.BoundCompoundPropertyModel;
import wicket.model.IModel;

/**
 * Simple ListVew subclass that wraps its item models in a BoundCompoundPropertyModel.
 * Useful for lists where the item components will be mapped through property expressions.
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
}
