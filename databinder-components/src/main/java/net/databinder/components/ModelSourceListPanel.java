package net.databinder.components;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;

public class ModelSourceListPanel extends SourceListPanel {
	private Component target;
	public ModelSourceListPanel(String id, Component target, String bodyProperty, IModel listModel ) {
		super(id, bodyProperty, listModel);
		this.target = target;
	}
	@Override
	protected Link sourceLink(String id, IModel model) {
		return new ModelSourceLink("link", target, model);
	}
}
