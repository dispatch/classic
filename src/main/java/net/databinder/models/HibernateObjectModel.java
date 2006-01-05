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

import java.io.Serializable;

import net.databinder.DataRequestCycle;

import wicket.model.LoadableDetachableModel;

/**
 * Model persisted by Hibernate.
 * @author Nathan Hamblen
 */

public class HibernateObjectModel extends LoadableDetachableModel {
	private Class objectClass;
	private Serializable objectId;
	
	/**
	 * @param objectClass class to be loaded and stored by Hibernate
	 * @param objectId id of the persistent object
	 */
	public HibernateObjectModel(Class objectClass, Serializable objectId) {
		this.objectClass = objectClass;
		this.objectId = objectId;
	}
	
	/**
	 * Constructor for a model with no existing persistent object. The model object
	 * will NOT be persisted in any way until it is replaced by a call to 
	 * setPersistentObject(Object persistent Object). Instead, it is newly constructed and 
	 * empty after any call to detach().  Form components themselves will hold any entered 
	 * data until the object can be saved for the first time, usually after the first successful 
	 * form submittal. 
	 * @param objectClass class to be loaded and stored by Hibernate
	 */
	public HibernateObjectModel(Class objectClass) {
		this.objectClass = objectClass;
	}
	
	/**
	 * Construct with a Hibernate persistant object.
	 * @param persistentObject must already be contained in the Hibernate session
	 */
	public HibernateObjectModel(Object persistentObject) {
		setPersistentObject(persistentObject);
	}
	
	/**
	 * Change the persistant object contained in this model. By using this method instead of
	 * replacing the model itself, you avoid accidently referencing the old model.
	 */
	public void setPersistentObject(Object persistentObject) {
		objectId = DataRequestCycle.getHibernateSession().getIdentifier(persistentObject);
		objectClass = persistentObject.getClass();
		attach();
	}
	
	/**
	 * Disassociates this object from any persitant object, but retains the class information
	 * @see HibernateObjectModel(Class objectClass)
	 */
	public void clearPersistentObject() {
		objectId = null;
		attach();
	}
	
	/**
	 * Load the object through Hibernate, or contruct a new instance if it is not bound to an id.
	 */
	@Override
	protected Object load() {
		try {
			if (objectId == null)
				return objectClass.newInstance();
		} catch (Exception e) {
			throw new RuntimeException("Unable to instantiate object. Does it have a default constructor?", e);
		}
		return DataRequestCycle.getHibernateSession().load(objectClass, objectId);
	}
}
