package net.databinder.components;

import net.databinder.models.BindingModel;

import org.apache.wicket.markup.html.link.Link;

public class UnbindLink extends Link {
	public UnbindLink(String id, BindingModel model) {
		super(id, model);
	}
	@Override
	public void onClick() {
		((BindingModel)getModel()).unbind();
	}
	@Override
	public boolean isEnabled() {
		return ((BindingModel)getModel()).isBound();
	}
}
