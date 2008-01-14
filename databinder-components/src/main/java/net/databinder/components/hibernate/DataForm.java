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

package net.databinder.components.hibernate;

import java.io.Serializable;

import net.databinder.models.HibernateObjectModel;

import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.BoundCompoundPropertyModel;
import org.apache.wicket.model.ComponentPropertyModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.hibernate.Session;

/**
 * Form for a persistent model object nested in a BoundCompoundPropertyModel.
 * Saves the model object to persistent storage when a valid form is submitted. This
 * form can be a child component of any Wicket page.
 * @author Nathan Hamblen
 */
public class DataForm extends DataFormBase {
	private Serializable version;

	/**
	 * Instantiates this form and a new, blank instance of the given class as a persistent model
	 * object. By default the model object created is serialized and retained between requests until
	 * it is persisted.
	 * @param id
	 * @param modelClass for the persistent object
	 * @see HibernateObjectModel#setRetainUnsaved(boolean)
	 */
	public DataForm(String id, Class modelClass) {
		super(id, new BoundCompoundPropertyModel(new HibernateObjectModel(modelClass)));
	}

	public DataForm(String id, HibernateObjectModel model) {
		super(id, new BoundCompoundPropertyModel(model));
		setFactoryKey(model.getFactoryKey());
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

	/**
	 * Form that is nested below a component with a compound model containing a Hibernate
	 * model.
	 * @param id
	 * @see DataWrapper
	 */
	public DataForm(String id) {
		super(id);
	}
	
	/**
	 * @param key for the Hibernate session factory to be used with this component
	 * @return this 
	 */
	@Override
	public DataForm setFactoryKey(Object key) {
		super.setFactoryKey(key);
		getPersistentObjectModel().setFactoryKey(key);
		return this;
	}


	public HibernateObjectModel getPersistentObjectModel() {
		return (HibernateObjectModel) getCompoundModel().getChainedModel();
	}

	/**
	 * Change the persistent model object of this form.
	 * @param object  to attach to this form
	 * @return this form, for chaining
	 */
	public DataForm setPersistentObject(Object object) {
		getPersistentObjectModel().setObject(object);
		modelChanged();
		return this;
	}
	
	private void updateVersion() {
		version = getPersistentObjectModel().getVersion();
	}

	/** Late-init version record. */
	@Override
	protected void onBeforeRender() {
		super.onBeforeRender();
		if (version == null)
			updateVersion();
	}
	
	@Override
	protected void onModelChanged() {
		updateVersion();
	}

	/**
	 * Replaces the form's model object with a new, blank instance. Does not affect
	 * persistent storage.
	 * @return this form, for chaining
	 */
	public DataForm clearPersistentObject() {
		getPersistentObjectModel().clearPersistentObject();
		modelChanged();
		return this;
	}

	/**
	 * Binding models to be phased out.
	 * @deprecated
	 * @see ComponentPropertyModel
	 */
	protected BoundCompoundPropertyModel getBindingModel() {
		return (BoundCompoundPropertyModel) getModel();
	}
	
	protected CompoundPropertyModel getCompoundModel() {
		IModel model = getModel();
		Component cur = this;
		while (cur != null) {
			model = cur.getModel();
			if (model != null && model instanceof CompoundPropertyModel)
				return (CompoundPropertyModel) model;
			cur = cur.getParent();
		}
		throw new WicketRuntimeException("DataForm has no parent compound model");
	}

	/**
	 * Saves the form's model object to persistent storage if it is new and commits
	 * the database transaction.
	 */
	@Override
	protected void onSubmit() {
		Object modelObject = getPersistentObjectModel().getObject();
		Session session = getHibernateSession();
		if (!session.contains(modelObject)) {
			session.save(modelObject);
			setPersistentObject(modelObject);	// tell model this object is now bound
		}
		super.onSubmit();	// flush and commit session
		// if version is present it should have changed
		if (version != null) {
			updateVersion();
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
	 * @return persistent storage version number if available, null otherwise
	 */
	protected Serializable getVersion() {
		return version;
	}

	/**
	 * Deletes the form's model object from persistent storage. Flushes change so that
	 * queries executed in the same request (e.g., in a HibernateListModel) will not return
	 * this object.
	 * @return true if the object was deleted, false if it did not exist
	 */
	protected boolean deletePersistentObject() {
		Session session = getHibernateSession();
		Object modelObject = getPersistentObjectModel().getObject();
		if (!session.contains(modelObject))
			return false;
		session.delete(modelObject);
		session.flush();
		return true;
	}
	
	public class ClearLink extends Link {
		public ClearLink(String id) {
			super(id);
		}
		@Override
		public boolean isEnabled() {
			return !DataForm.this.isVisibleInHierarchy() || getPersistentObjectModel().isBound();
		}
		@Override
		public void onClick() {
			clearPersistentObject();
			DataForm.this.setVisible(true);
		}
	}

	/** @deprecated retain unsaved is now the default behavior; this method does nothing */
	public DataForm retainUnsaved() {
		return this;
	}
}