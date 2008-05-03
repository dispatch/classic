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

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IWrapModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidatorAddListener;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.hibernate.Hibernate;
import org.hibernate.validator.ClassValidator;
import org.hibernate.validator.InvalidValue;

/**
 * Checks a base model and property name against Hibernate Validator.
 * @author Nathan Hamblen
 */
public class DatabinderValidator extends AbstractValidator implements IValidatorAddListener {
	/** base model, may be null until first call to onValidate. */
	private IModel base;
	/** property of base to validate, may be null until first call to onValidate. */
	private String property;
	/** component added to */
	private Component component;
	
	/**
	 * Validator for a property of an entity.
	 * @param base entity to validate
	 * @param property property of base to validate
	 */
	public DatabinderValidator(IModel base, String property) {
		this.base = base;
		this.property = property;
	}
	
	/**
	 * Construct instance that attempts to determine the base object and property
	 * to validate form the component it is added to. This is only possible for
	 * components that depend on a parent CompoundPropertyModel or their own 
	 * PropertyModels. The attempt is not made until the first validation check 
	 * in {@link #onValidate(IValidatable)} (to allow the full component 
	 * hierarchy to be constructed). Do not use an instance for more than 
	 * one component.
	 */
	public DatabinderValidator() { }
	
	/**
	 * Checks the component against Hibernate Validator. If the base model
	 * and property were not supplied in the constructor, they will be determined
	 * from the component this validator was added to.
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void onValidate(IValidatable comp) {
		if (base == null || property == null) {
			ModelProp mp = getModelProp(component);
			base = mp.model;
			property = mp.prop;
		}
		Object o  = base.getObject();
		Class c = Hibernate.getClass(o);
		ClassValidator validator = new ClassValidator(c);
		for (InvalidValue iv : validator.getPotentialInvalidValues(property, comp.getValue()))
			comp.error(new ValidationError().setMessage(iv.getPropertyName() + " " + iv.getMessage()));
	}
	
	/** Retains component for possible use in onValidate. */
	public void onAdded(Component component) {
		this.component = component;
	}
	
	/** @return always true */
	@Override
	public boolean validateOnNullValue() {
		return true;
	}
	
	private static class ModelProp { IModel model; String prop; }
	
	/** @return base object and property derived from this component */
	private static ModelProp getModelProp(Component formComponent) {
		IModel model = formComponent.getModel();
		ModelProp mp = new ModelProp();
		if (model instanceof PropertyModel) {
			PropertyModel propModel = (PropertyModel) model;
			mp.model = propModel.getChainedModel();
			mp.prop = propModel.getPropertyExpression();
		} else if (model instanceof IWrapModel) {
			mp.model = ((IWrapModel)model).getWrappedModel();
			mp.prop = formComponent.getId();
		} else throw new UnrecognizedModelException(formComponent, model);
		return mp;
	}
	
	/**
	 * Add immediately to a form component. Note that the component's model
	 * object must be available for inspection at this point or an exception will
	 * be thrown. (For a CompoundPropertyModel, this means the hierarchy must
	 * be established.) This is only possible for components that depend on a 
	 * parent CompoundPropertyModel or their own PropertyModels.
	 * @param formComponent component to add validator to
	 * @throws UnrecognizedModelException if no usable model is present
	 */
	public static void addTo(FormComponent formComponent) {
		ModelProp mp = getModelProp(formComponent);
		formComponent.add(new DatabinderValidator(mp.model, mp.prop));
	}
	
	public static class UnrecognizedModelException extends RuntimeException {
		public UnrecognizedModelException(Component formComponent, IModel model) {
			super("DatabinderValidator doesn't recognize the model " 
				+ model + " of component " + formComponent.toString()); 
		}
	}
}
