package net.databinder.components;

import wicket.markup.ComponentTag;
import wicket.markup.MarkupStream;
import wicket.markup.html.PackageResourceReference;
import wicket.markup.html.WebMarkupContainer;
import wicket.model.IModel;
import wicket.model.Model;

/**
 * Component for a stylesheet link. The stylesheet is expected to be named
 * &gt;ClassName&lt;.css for the Class specified in the constructor or via a
 * compound model, and be located in the same package as that class.
 * @author Nathan Hamblen
 */
public class PageStyleLink extends WebMarkupContainer {
	
	/** Builds a PageStyleLink based on the given class. */
	public PageStyleLink(String id, Class pageClass) {
		this(id, new Model(pageClass));
	}
	
        /** Builds a PageStyleLink from a compound property model. */
	public PageStyleLink(String id) {
		super(id);
	}
	
        /** Builds a PageStyleLink based on a class in an IModel. */
	public PageStyleLink(String id, IModel model) {
		super(id, model);
	}
	
	/** Ensures that this is a "link" HTML element. */
	@Override
	protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
		checkComponentTag(openTag, "link");
		super.onComponentTagBody(markupStream, openTag);
	}

	/** Sets appropriate href, type, and rel values for the stylesheet. */
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
