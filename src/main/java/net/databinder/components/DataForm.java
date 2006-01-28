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

import net.databinder.DataRequestCycle;
import net.databinder.models.HibernateObjectModel;

import org.hibernate.Session;


import wicket.markup.html.form.Form;
import wicket.model.BoundCompoundPropertyModel;

/**
 * Form for a persistant model object nested in a BoundCompoundPropertyModel. 
 * Saves the model object to persistent storage when a valid form is submitted. This 
 * form can be a child to any Wicket page (not necessarily a net.databinder.DataPage).
 * @author Nathan Hamblen
 */
public class DataForm extends Form {
	/**
	 * Create form with an existing persistent object model.
	 * @param id
	 * @param model to be wrapped in a BoundCompoundPropertyModel
	 */
	public DataForm(String id, HibernateObjectModel model) {
		super(id, model);
	}
	
	/**
	 * Instatiates this form and a new, blank instance of the given class as a persistent model object. 
	 * @param id
	 * @param modelClass for the model object
	 */
	public DataForm(String id, Class modelClass) {
		super(id, new BoundCompoundPropertyModel(new HibernateObjectModel(modelClass)));
	}
	
	protected HibernateObjectModel getPersistentObjectModel() {
		try {
			return (HibernateObjectModel) getModel().getNestedModel();
		} catch (ClassCastException c) {
			throw new RuntimeException("DataForm's nested model was not a HibernateObjectModel", c);
		}
	}
	
	/**
	 * Change the persistent model object of this form.
	 * @param object  to attach to this form
	 * @return this form, for chaining
	 */
	public DataForm setPersistentObject(Object object) {
		getPersistentObjectModel().setPersistentObject(object);
		setModel(getModel());		// informs child components
		return this;
	}
	
	/**
	 * Replaces the form's model object with a new, blank instance. Does not affect
	 * persistent storage.
	 * @return this form, for chaining
	 */
	public DataForm clearPersistentObject() {
		getPersistentObjectModel().clearPersistentObject();
		setModel(getModel());		// informs child components
		return this;
	}
	
	/**
	 * @return this form's model, for binding components to expressions
	 */
	protected BoundCompoundPropertyModel getBindingModel() {
		return (BoundCompoundPropertyModel) getModel();
	}
	
	/**
	 * Saves the form's model object to persistent storage if it is new, and commits
	 * database transaction.
	 */
	@Override
	protected void onSubmit() {
		Object modelObject = getModelObject();
		Session session = DataRequestCycle.getHibernateSession();
		if (!session.contains(modelObject)) {
			session.save(modelObject);
			setPersistentObject(modelObject);	// tell model this object is now bound
		}
		session.getTransaction().commit();
	}
	
	/**
	 * Deletes the form's model object from persistent storage.
	 * @return true if the object was deleted, false if it did not exist
	 */
	protected boolean deletePersistentObject() {
		Session session = DataRequestCycle.getHibernateSession();
		Object modelObject = getModelObject();
		if (!session.contains(modelObject))
			return false;
		session.delete(modelObject);
		session.flush();
		return true;
	}
}
