/*
 * Databinder: a simple bridge from Wicket to Hibernate
 * Copyright (C) 2008  Nathan Hamblen nathan@technically.us

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
package net.databinder.valid.hib;

import java.io.Serializable;

import net.databinder.components.hib.DataForm;
import net.databinder.models.hib.HibernateObjectModel;
import net.databinder.valid.hib.DatabinderValidator.UnrecognizedModelException;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.hibernate.Hibernate;
import org.hibernate.validator.ClassValidator;
import org.hibernate.validator.InvalidValue;

/**
 * Form that adds a {@link DatabinderValidator} to all its components that
 * do not have any other validator in place. To exempt a component from
 * this validation, add to it the dummy validator returned by {@link #nonValidator()}.
 * Components are inspected in {@link #onBeforeRender()}. Those that do not have
 * a usable model (see {@link DatabinderValidator#DatabinderValidator()}
 * at that time are ignored.
 * @author Nathan Hamblen
 * @see DatabinderValidator
 */
public class ValidDataForm extends DataForm {
	/**
	 * Instantiates this form and a new, blank instance of the given class as a persistent model
	 * object. By default the model object created is serialized and retained between requests until
	 * it is persisted.
	 * @param id
	 * @param modelClass for the persistent object
	 * @see HibernateObjectModel#setRetainUnsaved(boolean)
	 */
	public ValidDataForm(String id, Class modelClass) {
		super(id, modelClass);
	}

	public ValidDataForm(String id, HibernateObjectModel model) {
		super(id, model);
	}

	/**
	 * Instantiates this form with a persistent object of the given class and id.
	 * @param id Wicket id
	 * @param modelClass for the persistent object
	 * @param persistentObjectId id of the persistent object
	 */
	public ValidDataForm(String id, Class modelClass, Serializable persistentObjectId) {
		super(id, modelClass, persistentObjectId);
	}

	/**
	 * Form that is nested below a component with a compound model containing a Hibernate
	 * model.
	 * @param id
	 */
	public ValidDataForm(String id) {
		super(id);
	}
	
	@SuppressWarnings("unchecked")
	protected void validateModelObject() {
		Object o = getPersistentObjectModel().getObject();
		for (InvalidValue iv : new ClassValidator(Hibernate.getClass(o)).getInvalidValues(o))
			error(iv.getPropertyName() + " " + iv.getMessage());
	}
	
	/**
	 * Add a validator to any form components that have no existing validator
	 * and whose model is recognized by {@link DatabinderValidator#addTo(FormComponent)}.
	 */
	@Override
	protected void onBeforeRender() {
		super.onBeforeRender();
		visitFormComponents(new FormComponent.AbstractVisitor() {
			@Override
			protected void onFormComponent(FormComponent formComponent) {
				if (formComponent.getValidators().isEmpty()) try {
					DatabinderValidator.addTo(formComponent);
				} catch (UnrecognizedModelException e) { }
			}
		});
	}
	
	/**
	 * @return dummy validator that can be used to exempt a component
	 * from this form's inspection in {@link #onBeforeRender()}
	 */
	public static IValidator nonValidator() {
		return new IValidator() {
			public void validate(IValidatable validatable) { }
		};
	}
}
