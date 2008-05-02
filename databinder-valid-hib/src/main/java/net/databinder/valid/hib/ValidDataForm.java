package net.databinder.valid.hib;

import java.io.Serializable;

import net.databinder.components.hib.DataForm;
import net.databinder.models.hib.HibernateObjectModel;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.ComponentPropertyModel;
import org.hibernate.Hibernate;
import org.hibernate.validator.ClassValidator;
import org.hibernate.validator.InvalidValue;

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

	public final MarkupContainer addValid(FormComponent child, String property) {
		child.add(new DatabinderValidator(property));
		child.setModel(new ComponentPropertyModel(property));
		return super.add(child);
	}
	public final MarkupContainer addValid(FormComponent child) {
		child.add(new DatabinderValidator(child.getId()));
		return super.add(child);
	}
	
}
