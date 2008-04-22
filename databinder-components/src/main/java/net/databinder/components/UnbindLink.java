package net.databinder.components;

import net.databinder.models.BindingModel;

import org.apache.wicket.markup.html.link.Link;

/**
 * Unbinds the target persistent model. This will generally revert the object
 * to a "blank" state, such as when creating a new object with a form instead of
 * updating one.
 * @see BindingModel
 */
public class UnbindLink extends Link {
	public UnbindLink(String id, BindingModel model) {
		super(id, model);
	}
	/** unbinds model */
	@Override
	public void onClick() {
		((BindingModel)getModel()).unbind();
	}
	/** @return true if model is bound */
	@Override
	public boolean isEnabled() {
		return ((BindingModel)getModel()).isBound();
	}
}
