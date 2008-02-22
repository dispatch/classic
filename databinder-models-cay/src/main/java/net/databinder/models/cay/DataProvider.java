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
package net.databinder.models.cay;

import java.util.Iterator;
import java.util.List;

import net.databinder.cay.Databinder;
import net.databinder.models.PropertyDataProvider;

import org.apache.cayenne.DataObject;
import org.apache.cayenne.query.SelectQuery;
import org.apache.wicket.model.IModel;

/**
 * IDataProvider implementation for Cayenne. Note that because Cayenne pagination
 * is abstracted through a java.util.List, this provider has no performance advantage 
 * over a PageableListView with a page size set in the SelectQuery.
 * @author Nathan Hamblen
 */
public class DataProvider extends PropertyDataProvider {
	
	private SelectQuery query;

	public DataProvider(SelectQuery query) {
		this.query = query;
	}
	
	public List getList() {
		return Databinder.getContext().performQuery(query);
	}
	
	public Iterator iterator(int first, int count) {
		query.setPageSize(count);
		return getList().subList(first, first + count).iterator();
	}
	
	public int size() {
		return getList().size();
	}
	
	@Override
	protected IModel dataModel(Object object) {
		return new DataObjectModel((DataObject)object);
	}

}
