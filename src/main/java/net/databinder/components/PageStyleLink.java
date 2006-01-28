package net.databinder.components;

import wicket.markup.ComponentTag;
import wicket.markup.MarkupStream;
import wicket.markup.html.PackageResourceReference;
import wicket.markup.html.WebMarkupContainer;
import wicket.model.IModel;
import wicket.model.Model;

public class PageStyleLink extends WebMarkupContainer {
	
	public PageStyleLink(String id, Class pageClass) {
		this(id, new Model(pageClass));
	}
	
	public PageStyleLink(String id) {
		super(id);
	}
	
	public PageStyleLink(String id, IModel model) {
		super(id, model);
	}
	
	@Override
	protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
		checkComponentTag(openTag, "link");
		super.onComponentTagBody(markupStream, openTag);
	}

	@Override
	protected void onComponentTag(ComponentTag tag) {
		Class pageClass = (Class) getModelObject();
		tag.put("href", getPage().urlFor(new PackageResourceReference(pageClass, 
				pageClass.getSimpleName() + ".css").getPath()));
		// ensure valid css attributes
		tag.put("type", "text/css");
		tag.put("rel", "stylesheet");
		super.onComponentTag(tag);
	}

}
