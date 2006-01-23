package net.databinder.components;

import wicket.markup.ComponentTag;
import wicket.markup.MarkupStream;
import wicket.markup.html.WebMarkupContainer;
import wicket.model.IModel;
import wicket.util.string.Strings;

/**
 * Component for an anchor linking outside of Wicket. Similar to 
 * wicket.markup.html.link.ExternaLink, but uses an IModel for the href attribute
 * value and will not replace its body. (Use a span with wicket.markup.html.basic.Label 
 * if your link body text is variable.)
 * @author Nathan Hamblen
 */
public class WebLink extends WebMarkupContainer {
	
	/**
	 * Initialize with a compound model.
	 */
	public WebLink(String id) {
		super(id);
	}
	
	/**
	 * Initialize with a specific model.
	 */
	public WebLink(String id, IModel model) {
		super(id, model);
	}
	
	/**
	 *  Sets the link's href to this component's model value, changing any ampersands
	 *  to the escaped form.
	 */
	@Override
	protected void onComponentTag(ComponentTag tag) {
		tag.put("href", Strings.replaceAll(getModelObjectAsString(), "&", "&amp;"));
		super.onComponentTag(tag);
	}
	
	/**
	 * Ensures that is component is mapped to an "a" tag.
	 */
	@Override
	protected void onComponentTagBody(MarkupStream markupStream, ComponentTag openTag) {
		checkComponentTag(openTag, "a");
		super.onComponentTagBody(markupStream, openTag);
	}
}
