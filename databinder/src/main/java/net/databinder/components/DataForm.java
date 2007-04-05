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

import java.io.Serializable;

import net.databinder.DataStaticService;
import net.databinder.models.HibernateObjectModel;

import org.hibernate.Session;

import wicket.model.BoundCompoundPropertyModel;

/**
 * Form for a persistant model object nested in a BoundCompoundPropertyModel.
 * Saves the model object to persistent storage when a valid form is submitted. This
 * form can be a child component of any Wicket page.
 * @author Nathan Hamblen
 */
public class DataForm extends DataFormBase {
	private Serializable version;

	/**
	 * Create form with an existing persistent object model.
	 * @param id
	 * @param model to be wrapped in a BoundCompoundPropertyModel
	 */
	public DataForm(String id, HibernateObjectModel model) {
		super(id, new BoundCompoundPropertyModel(model));
		version = getPersistentObjectModel().getVersion();
	}

	/**
	 * Instatiates this form and a new, blank instance of the given class as a persistent modell
	 * object. By default the model object created is <b>not</b> retained between requests until
	 * it is persisted. This works well when the object's initial state is determined wholly by
	 * data posted with this form. For special cases, such as non-posting ajax requests,
	 * call <tt>retainUnsaved()</tt>.
	 * @param id
	 * @param modelClass for the persistent object
	 */
	public DataForm(String id, Class modelClass) {
		super(id, new BoundCompoundPropertyModel(new HibernateObjectModel(modelClass)));
	}

	/** @deprecated retain unsaved is now the default behavior; this method does nothing */
	public DataForm retainUnsaved() {
		return this;
	}

	/**
	 * Instantiates this form with a persistent object of the given class and id.
	 * @param id Wicket id
	 * @param modelClass for the persistent object
	 * @param persistentObjectId id of the persistent object
	 */
	public DataForm(String id, Class modelClass, Serializable persistentObjectId) {
		super(id, new BoundCompoundPropertyModel(new HibernateObjectModel(modelClass, persistentObjectId)));
	}

	public HibernateObjectModel getPersistentObjectModel() {
		return (HibernateObjectModel) getBindingModel().getTarget();
	}

	/**
	 * Change the persistent model object of this form.
	 * @param object  to attach to this form
	 * @return this form, for chaining
	 */
	public DataForm setPersistentObject(Object object) {
		getPersistentObjectModel().setObject(object);
		setModel(getModel());		// informs child components
		version = getPersistentObjectModel().getVersion();
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
		version = null;
		return this;
	}

	/**
	 * @return this form's model, for binding components to expressions
	 */
	protected BoundCompoundPropertyModel getBindingModel() {
		return (BoundCompoundPropertyModel) getModel();
	}

	/**
	 * Saves the form's model object to persistent storage if it is new and commits
	 * the database transaction.
	 */
	@Override
	protected void onSubmit() {
		Object modelObject = getModelObject();
		Session session = DataStaticService.getHibernateSession();
		if (!session.contains(modelObject)) {
			session.save(modelObject);
			setPersistentObject(modelObject);	// tell model this object is now bound
		}
		super.onSubmit();	// flush and commit session
		// if version is present it should have changed
		if (version != null) {
			version = getPersistentObjectModel().getVersion();
		}
	}

	/**
	 * Checks that the version number, if present, is the last known version number.
	 * If it does not match, validation fails and will continue to fail until the form is
	 * reloaded with the updated data and version number. This allows the user to
	 * preserve her unsaved changes while preventing overwrites. <p> <b>Note:</b> although
	 * timestamp versions are supported, beware of rounding errors. equals() must return true
	 * when comparing the retained version object to the one loaded from persistent storage.
	 */
	@Override
	protected void validate() {
		if (version != null) {
			Serializable currentVersion = getPersistentObjectModel().getVersion();
			if (!version.equals(currentVersion))
				error(getString("version.mismatch", null)); // report error
				// do not update version number as old data still appears in form
		}

		super.validate();
	}

	/**
	 * Deletes the form's model object from persistent storage. Flushes change so that
	 * queries executed in the same request (e.g., in a HibernateListModel) will not return
	 * this object.
	 * @return true if the object was deleted, false if it did not exist
	 */
	protected boolean deletePersistentObject() {
		Session session = DataStaticService.getHibernateSession();
		Object modelObject = getModelObject();
		if (!session.contains(modelObject))
			return false;
		session.delete(modelObject);
		session.flush();
		return true;
	}
}
