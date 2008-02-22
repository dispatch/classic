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
package net.databinder.models;

import org.apache.wicket.markup.repeater.data.DefaultDataProvider;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;

/**
 * Base IDataProvider class with support for wrapping item models in a
 * CompoundPropertyModel.
 * @author Nathan Hamblen
 */
public abstract class PropertyDataProvider extends DefaultDataProvider {

	/** Controls wrapping with a compound property model. */
	private boolean wrapWithPropertyModel = true;
	
	public PropertyDataProvider setWrapWithPropertyModel(boolean wrapWithPropertyModel) {
		this.wrapWithPropertyModel = wrapWithPropertyModel;
		return this;
	}
	
	/**
	 * Wraps object in a persistent object model, and also CompoundPropertyModel if 
	 * wrapInCompoundModel is true.
	 * @param object object DataView would like to wrap
	 * @return object wrapped in a peristent model and possibly CompoundPropertyModel
	 */
	public IModel model(Object object) {
		IModel model = dataModel(object);
		if (wrapWithPropertyModel)
			model = new CompoundPropertyModel(model);
		return model;
	}
	
	/** Wrap in appropriate persistent model in subclass */
	protected abstract IModel dataModel(Object object);

}
