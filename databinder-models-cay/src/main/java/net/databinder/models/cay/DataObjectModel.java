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

import net.databinder.cay.Databinder;
import net.databinder.models.BindingModel;
import net.databinder.models.LoadableWritableModel;

import org.apache.cayenne.DataObject;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.PersistenceState;

public class DataObjectModel extends LoadableWritableModel implements BindingModel {
	
	private ObjectId id;
	private DataObject retainedObject;

	public DataObjectModel(Class<? extends DataObject> objectClass) {
		setObject(Databinder.getContext().newObject(objectClass));
	}
	
	public DataObjectModel(ObjectId id) {
		this.id = id;
	}

	public DataObjectModel(DataObject object) {
		id = object.getObjectId();
	}

	@Override
	protected Object load() {
		if (retainedObject != null) {
			Databinder.getContext().registerNewObject(retainedObject);
			return retainedObject;
		}
		return DataObjectUtils.objectForPK(Databinder.getContext(), id);
	}

	public void setObject(Object object) {
		DataObject dataObject = (DataObject) object;
		id = dataObject.getObjectId();
		setTempModelObject(dataObject);
		if (!isBound())
			retainedObject = dataObject;
		else
			retainedObject = null;
	}
	
	@Override
	public DataObject getObject() {
		return (DataObject) super.getObject();
	}
	
	@Override
	protected void onDetach() {
		if (retainedObject != null) {
			id = retainedObject.getObjectId();
			if (retainedObject.getPersistenceState() == PersistenceState.COMMITTED)
				retainedObject = null;
		}
	}
	
	public boolean isBound() {
		return !id.isTemporary();
	}
	
	public void unbind() {
		setObject(Databinder.getContext().createAndRegisterNewObject(id.getEntityName()));
	}

}
