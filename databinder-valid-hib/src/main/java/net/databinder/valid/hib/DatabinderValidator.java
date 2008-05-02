package net.databinder.valid.hib;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.IWrapModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidatorAddListener;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.AbstractValidator;
import org.hibernate.Hibernate;
import org.hibernate.validator.ClassValidator;
import org.hibernate.validator.InvalidValue;

public class DatabinderValidator extends AbstractValidator implements IValidatorAddListener {
	Component component;
	String property;
	public DatabinderValidator(String property) {
		this.property = property;
	}
	
	public void onAdded(Component component) {
		this.component = component;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onValidate(IValidatable comp) {
		IModel model = component.getModel();
		// TODO don't assume wrapped model, "unwrap" util method?
		Object o  = ((IWrapModel)model).getWrappedModel().getObject();
		Class c = Hibernate.getClass(o);
		ClassValidator validator = new ClassValidator(c);
		for (InvalidValue iv : validator.getPotentialInvalidValues(property, comp.getValue()))
			comp.error(new ValidationError().setMessage(iv.getMessage()));
	}
	
	@Override
	public boolean validateOnNullValue() {
		return true;
	}

}
